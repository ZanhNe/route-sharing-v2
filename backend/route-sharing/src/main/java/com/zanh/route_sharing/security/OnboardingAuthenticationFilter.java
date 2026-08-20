package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.exception.ResourceNotFoundException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
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
public class OnboardingAuthenticationFilter extends OncePerRequestFilter {
    public static final String ONBOARDING_ACCESS = "ONBOARDING_ACCESS";
    public static final String ONBOARDING_VERIFY_EMAIL = "ONBOARDING_VERIFY_EMAIL";
    public static final String ONBOARDING_COMPLETE_PROFILE = "ONBOARDING_COMPLETE_PROFILE";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ONBOARDING_PREFIX = "/api/v1/onboarding/";

    private final OnboardingAccessTokenService tokenService;
    private final SecurityStateService securityStateService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + ONBOARDING_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = bearerToken(request.getHeader("Authorization"));
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtOnboardingClaims claims = tokenService.parse(token);
            SecurityState current = securityStateService.requireCurrent(claims.userId());
            validateCurrentState(claims, current);

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority(ONBOARDING_ACCESS),
                    new SimpleGrantedAuthority(stepAuthority(claims.step())));
            CustomUserDetails principal = new CustomUserDetails(
                    claims.userId(),
                    claims.email(),
                    "",
                    current.status(),
                    current.securityVersion(),
                    authorities);
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

    static void validateCurrentState(JwtOnboardingClaims claims, SecurityState current) {
        TrangThaiTaiKhoan currentStatus = current.status();
        // Account-state/email/security mutations bump securityVersion in PostgreSQL.
        // Classify a token bound to the prior version as TOKEN_STALE before evaluating
        // its now-obsolete onboarding state/step, including IAM-02 VERIFY_EMAIL ->
        // COMPLETE_PROFILE.
        if (claims.securityVersion() != current.securityVersion()) {
            throw new CredentialsExpiredException("Phiên onboarding đã lỗi thời");
        }
        if (claims.accountStatus() != currentStatus) {
            throw new DisabledException("Trạng thái onboarding đã thay đổi");
        }
        if (currentStatus != TrangThaiTaiKhoan.CHO_XAC_THUC_EMAIL
                && currentStatus != TrangThaiTaiKhoan.CHO_DUYET_HO_SO) {
            throw new DisabledException("Tài khoản không còn ở onboarding state");
        }
        if (claims.step() == OnboardingStep.VERIFY_EMAIL
                && currentStatus != TrangThaiTaiKhoan.CHO_XAC_THUC_EMAIL) {
            throw new DisabledException("Onboarding step không còn hợp lệ");
        }
        if (claims.step() == OnboardingStep.COMPLETE_PROFILE
                && currentStatus != TrangThaiTaiKhoan.CHO_DUYET_HO_SO) {
            throw new DisabledException("Onboarding step không còn hợp lệ");
        }
    }

    private static String stepAuthority(OnboardingStep step) {
        return switch (step) {
            case VERIFY_EMAIL -> ONBOARDING_VERIFY_EMAIL;
            case COMPLETE_PROFILE -> ONBOARDING_COMPLETE_PROFILE;
        };
    }

    private static String bearerToken(String header) {
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static org.springframework.security.core.AuthenticationException toAuthenticationException(
            RuntimeException exception) {
        if (exception instanceof org.springframework.security.core.AuthenticationException authException) {
            return authException;
        }
        return new BadCredentialsException("Onboarding token không hợp lệ", exception);
    }
}
