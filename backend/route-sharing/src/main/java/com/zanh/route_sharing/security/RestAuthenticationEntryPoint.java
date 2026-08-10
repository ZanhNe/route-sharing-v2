package com.zanh.route_sharing.security;

import com.zanh.route_sharing.dto.response.ApiErrorResponse;
import com.zanh.route_sharing.utils.time.TimePolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        int status = HttpServletResponse.SC_UNAUTHORIZED;
        String code = "UNAUTHENTICATED";
        String message = "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.";
        if (authException instanceof DisabledException || authException instanceof LockedException) {
            status = HttpServletResponse.SC_FORBIDDEN;
            code = "ACCOUNT_INACTIVE";
            message = "Tài khoản hiện không còn ở trạng thái hoạt động.";
        } else if (authException instanceof CredentialsExpiredException) {
            code = "TOKEN_STALE";
            message = "Quyền hoặc trạng thái tài khoản đã thay đổi. Vui lòng làm mới phiên đăng nhập.";
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        jsonMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(
                TimePolicy.now(clock), status, code, message, request.getRequestURI()));
    }
}
