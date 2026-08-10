package com.zanh.route_sharing.security;

import com.zanh.route_sharing.dto.response.ApiErrorResponse;
import com.zanh.route_sharing.utils.time.TimePolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Override
    public void handle(HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        jsonMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(
                TimePolicy.now(clock), HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED",
                "Bạn không có quyền thực hiện thao tác này.",
                request.getRequestURI()));
    }
}
