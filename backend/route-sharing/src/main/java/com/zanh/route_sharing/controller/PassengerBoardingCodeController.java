package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.boarding.PassengerBoardingCodeResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.PassengerBoardingCodeService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@Validated
public class PassengerBoardingCodeController {
    private final PassengerBoardingCodeService service;

    public PassengerBoardingCodeController(PassengerBoardingCodeService service) {
        this.service = service;
    }

    @PostMapping("/{tripId}/boarding-code")
    @PreAuthorize("hasAuthority('VIEW_OWN_BOARDING_CODE')")
    public ResponseEntity<ApiResponse<PassengerBoardingCodeResponse>> requestOwnCode(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "tripId phải là số dương.") Long tripId) {
        PassengerBoardingCodeResponse data = service.requestOwnCode(principal.getId(), tripId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(ApiResponse.success(HttpStatus.OK.value(), data, "Lấy boarding code thành công."));
    }
}
