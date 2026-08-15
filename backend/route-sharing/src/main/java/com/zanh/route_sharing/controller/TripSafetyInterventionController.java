package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.safety.TripSafetyInterventionResponse;
import com.zanh.route_sharing.dto.trip.safety.TripSafetySafeExitRequest;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.TripSafetyInterventionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/safety-interventions")
@Validated
public class TripSafetyInterventionController {
    private final TripSafetyInterventionService service;

    public TripSafetyInterventionController(TripSafetyInterventionService service) {
        this.service = service;
    }

    @PostMapping("/{interventionId}/confirm-safe-exit")
    @PreAuthorize("hasAuthority('MANAGE_OWN_TRIP_SAFETY_INTERVENTION')")
    public ResponseEntity<ApiResponse<TripSafetyInterventionResponse>> confirmSafeExit(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive Long tripId,
            @PathVariable @Positive Long interventionId,
            @Valid @RequestBody TripSafetySafeExitRequest request) {
        return ok(service.confirmSafeExit(principal.getId(), tripId, interventionId, request),
                "Xác nhận xuống xe khẩn cấp an toàn thành công.");
    }

    @PostMapping("/{interventionId}/abort-trip")
    @PreAuthorize("hasAuthority('MANAGE_OWN_TRIP_SAFETY_INTERVENTION')")
    public ResponseEntity<ApiResponse<TripSafetyInterventionResponse>> abortTrip(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive Long tripId,
            @PathVariable @Positive Long interventionId) {
        return ok(service.abortTripFromHold(principal.getId(), tripId, interventionId),
                "Kết thúc Trip khẩn cấp thành công.");
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), data, message));
    }
}
