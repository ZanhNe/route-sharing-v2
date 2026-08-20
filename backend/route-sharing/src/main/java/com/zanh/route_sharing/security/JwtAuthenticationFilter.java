package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import com.zanh.route_sharing.exception.ResourceNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final SecurityStateService securityStateService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return uri.startsWith(contextPath + "/api/v1/auth/")
                || uri.startsWith(contextPath + "/api/v1/onboarding/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = bearerToken(header);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtAccessClaims claims = jwtUtil.parseAccessToken(token);
            SecurityState current = securityStateService.requireCurrent(claims.userId());
            validateCurrentState(claims, current);

            List<SimpleGrantedAuthority> authorities = claims.authorities().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            CustomUserDetails principal = new CustomUserDetails(
                    claims.userId(), claims.email(), "", current.status(), current.securityVersion(), authorities);

            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken
                    .authenticated(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            filterChain.doFilter(request, response);
        } catch (JwtException | org.springframework.security.core.AuthenticationException
                | ResourceNotFoundException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, toAuthenticationException(exception));
        }
    }

    private static String bearerToken(String header) {
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    static void validateCurrentState(JwtAccessClaims claims, SecurityState current) {
        if (claims.accountStatus() != TrangThaiTaiKhoan.ACTIVE || current.status() != TrangThaiTaiKhoan.ACTIVE) {
            throw new DisabledException("Account chưa được kích hoạt");
        }
        if (claims.securityVersion() != current.securityVersion()) {
            throw new CredentialsExpiredException("Phiên bảo mật đã lỗi thời");
        }
    }

    private static org.springframework.security.core.AuthenticationException toAuthenticationException(
            RuntimeException exception) {
        if (exception instanceof org.springframework.security.core.AuthenticationException authException) {
            return authException;
        }
        return new BadCredentialsException("Access token không hợp lệ", exception);
    }
}
