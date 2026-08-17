package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.dropoffverification.TripDropoffVerificationRequest;
import com.zanh.route_sharing.dto.trip.dropoffverification.TripDropoffVerificationResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.TripDropoffVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@Validated
public class TripDropoffVerificationController {
    private final TripDropoffVerificationService service;

    public TripDropoffVerificationController(TripDropoffVerificationService service) {
        this.service = service;
    }

    @PostMapping("/{tripId}/dropoff")
    @PreAuthorize("hasAuthority('CONFIRM_OWN_TRIP_DROPOFF')")
    public ResponseEntity<ApiResponse<TripDropoffVerificationResponse>> verifyCurrentDropoff(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "tripId phải là số dương.") Long tripId,
            @Valid @RequestBody TripDropoffVerificationRequest request) {
        TripDropoffVerificationResponse data = service.verifyCurrentDropoff(principal.getId(), tripId, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), data, "Xác nhận trả khách thành công."));
    }
}
