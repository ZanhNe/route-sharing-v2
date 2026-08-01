package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import com.zanh.route_sharing.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InternalSessionSecurityFilter extends OncePerRequestFilter {
    private final SecurityStateService securityStateService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails principal) {
            try {
                SecurityState current = securityStateService.requireCurrent(principal.getId());
                if (current.status() != TrangThaiTaiKhoan.ACTIVE
                        || current.securityVersion() != principal.getSecurityVersion()) {
                    invalidateSession(request);
                    response.sendRedirect(request.getContextPath() + "/internal/login?reauth");
                    return;
                }
            } catch (ResourceNotFoundException exception) {
                invalidateSession(request);
                response.sendRedirect(request.getContextPath() + "/internal/login?reauth");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static void invalidateSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
