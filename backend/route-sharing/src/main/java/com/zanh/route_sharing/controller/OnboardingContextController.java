package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.auth.entry.OnboardingContextResponse;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.iam.auth.OnboardingContextService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingContextController {
    private final OnboardingContextService service;

    public OnboardingContextController(OnboardingContextService service) {
        this.service = service;
    }

    @GetMapping("/context")
    public ResponseEntity<ApiResponse<OnboardingContextResponse>> getContext(
            @AuthenticationPrincipal CustomUserDetails principal) {
        OnboardingContextResponse data = service.getCurrent(principal == null ? null : principal.getId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(ApiResponse.success(HttpStatus.OK.value(), data, "Lấy ngữ cảnh onboarding thành công."));
    }
}
