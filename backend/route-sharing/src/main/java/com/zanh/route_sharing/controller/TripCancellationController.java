package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.cancellation.CancelTripBeforeStartRequest;
import com.zanh.route_sharing.dto.trip.cancellation.CancelTripBeforeStartResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.TripCancellationService;
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
public class TripCancellationController {

    private final TripCancellationService service;

    public TripCancellationController(TripCancellationService service) {
        this.service = service;
    }

    @PostMapping("/{tripId}/cancel")
    @PreAuthorize("hasAuthority('CANCEL_OWN_TRIP')")
    public ResponseEntity<ApiResponse<CancelTripBeforeStartResponse>> cancelBeforeStart(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "tripId phải là số dương.") Long tripId,
            @Valid @RequestBody CancelTripBeforeStartRequest request) {
        CancelTripBeforeStartResponse data = service.cancelBeforeStart(principal.getId(), tripId, request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                data,
                "Hủy chuyến trước khi bắt đầu thành công."));
    }
}
