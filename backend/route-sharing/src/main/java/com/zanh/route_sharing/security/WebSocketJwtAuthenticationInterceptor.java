package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.exception.ResourceNotFoundException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketJwtAuthenticationInterceptor implements ChannelInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final SecurityStateService securityStateService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticateConnect(accessor);
            case SEND -> {
                validateEstablishedPrincipal(accessor);
                requireDestination(accessor.getDestination(), "/app/");
            }
            case SUBSCRIBE -> {
                validateEstablishedPrincipal(accessor);
                String destination = accessor.getDestination();
                if (destination == null
                        || !(destination.startsWith("/user/queue/") || destination.startsWith("/topic/public/"))) {
                    throw new BadCredentialsException("Không thể subscribe vì destination không hợp lệ");
                }
            }
            default -> {

            }
        }
        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication existing && existing.isAuthenticated()) {
            validatePrincipal(existing);
            return;
        }

        String token = bearerToken(accessor);
        try {
            JwtAccessClaims claims = jwtUtil.parseAccessToken(token);
            SecurityState current = securityStateService.requireCurrent(claims.userId());
            JwtAuthenticationFilter.validateCurrentState(claims, current);
            List<SimpleGrantedAuthority> authorities = claims.authorities().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            CustomUserDetails principal = new CustomUserDetails(
                    claims.userId(), claims.email(), "", current.status(), current.securityVersion(), authorities);
            accessor.setUser(UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
        } catch (JwtException | org.springframework.security.core.AuthenticationException
                | ResourceNotFoundException exception) {
            throw new BadCredentialsException("Không thể kết nối WebSocket do token không hợp lệ", exception);
        }
    }

    private void validateEstablishedPrincipal(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("WebSocket session chưa được xác thực");
        }
        validatePrincipal(authentication);
    }

    private void validatePrincipal(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new BadCredentialsException("WebSocket principal không hợp lệ");
        }
        try {
            SecurityState current = securityStateService.requireCurrent(principal.getId());
            if (current.status() != TrangThaiTaiKhoan.ACTIVE
                    || current.securityVersion() != principal.getSecurityVersion()) {
                throw new BadCredentialsException("WebSocket security state không hợp lệ");
            }
        } catch (ResourceNotFoundException exception) {
            throw new BadCredentialsException("Tài khoản WebSocket không còn tồn tại", exception);
        }
    }

    private static String bearerToken(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader("Authorization");
        if (headers == null || headers.size() != 1) {
            throw new BadCredentialsException("Thiếu hoặc không rõ ràng Bearer token trong STOMP CONNECT");
        }
        String header = headers.get(0);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new BadCredentialsException("Thiếu Bearer token trong STOMP CONNECT");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BadCredentialsException("Không có Bearer token trong STOMP CONNECT");
        }
        return token;
    }

    private static void requireDestination(String destination, String prefix) {
        if (destination == null || !destination.startsWith(prefix)) {
            throw new BadCredentialsException("Không thể subscribe");
        }
    }
}
