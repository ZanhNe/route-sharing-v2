package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.auth.emailverification.EmailVerificationRequestResponse;
import com.zanh.route_sharing.dto.auth.emailverification.EmailVerificationResponse;
import com.zanh.route_sharing.dto.auth.emailverification.EmailVerificationVerifyRequest;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.security.OnboardingAuthenticationFilter;
import com.zanh.route_sharing.service.iam.emailverification.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding/email-verification")
public class EmailVerificationController {
    private static final String UNKNOWN_REMOTE_ADDRESS = "unknown";
    private final EmailVerificationService service;

    public EmailVerificationController(EmailVerificationService service) {
        this.service = service;
    }

    @PostMapping("/request")
    @PreAuthorize("hasAuthority('" + OnboardingAuthenticationFilter.ONBOARDING_VERIFY_EMAIL + "')")
    public ResponseEntity<ApiResponse<EmailVerificationRequestResponse>> requestCode(
            @AuthenticationPrincipal CustomUserDetails principal,
            HttpServletRequest servletRequest) {
        EmailVerificationRequestResponse data = service.requestCode(
                principal == null ? null : principal.getId(), boundedRemoteAddress(servletRequest));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(ApiResponse.success(
                        HttpStatus.ACCEPTED.value(),
                        data,
                        "Yêu cầu gửi mã xác thực đã được tiếp nhận."));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAuthority('" + OnboardingAuthenticationFilter.ONBOARDING_VERIFY_EMAIL + "')")
    public ResponseEntity<ApiResponse<EmailVerificationResponse>> verifyCode(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EmailVerificationVerifyRequest request,
            HttpServletRequest servletRequest) {
        EmailVerificationResponse data = service.verifyCode(
                principal == null ? null : principal.getId(), request.code(), boundedRemoteAddress(servletRequest));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(ApiResponse.success(HttpStatus.OK.value(), data, "Xác thực email thành công."));
    }

    private static String boundedRemoteAddress(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_REMOTE_ADDRESS;
        }
        String value = request.getRemoteAddr();
        String normalized = value == null || value.isBlank() ? UNKNOWN_REMOTE_ADDRESS : value.trim();
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }
}
