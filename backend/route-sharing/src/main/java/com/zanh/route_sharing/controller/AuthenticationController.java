package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.auth.LoginRequest;
import com.zanh.route_sharing.dto.auth.RefreshTokenRequest;
import com.zanh.route_sharing.dto.auth.session.AuthSessionResponse;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.security.ClientRequestInfo;
import com.zanh.route_sharing.service.iam.auth.AuthenticationSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AuthenticationSessionService service;

    public AuthenticationController(AuthenticationSessionService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        AuthSessionResponse data = service.login(request, clientInfo(servletRequest));
        return noStore(ApiResponse.success(HttpStatus.OK.value(), data, "Đăng nhập thành công."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest) {
        AuthSessionResponse data = service.refresh(request, clientInfo(servletRequest));
        return noStore(ApiResponse.success(HttpStatus.OK.value(), data, "Làm mới phiên đăng nhập thành công."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        service.logout(request);
        return noStore(ApiResponse.success(HttpStatus.OK.value(), null, "Đăng xuất thành công."));
    }

    private static <T> ResponseEntity<ApiResponse<T>> noStore(ApiResponse<T> body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    private static ClientRequestInfo clientInfo(HttpServletRequest request) {
        return new ClientRequestInfo(request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT));
    }
}
