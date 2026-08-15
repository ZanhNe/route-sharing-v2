package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.location.TripLocationRequest;
import com.zanh.route_sharing.dto.trip.location.TripLocationResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.TripLocationService;
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
public class TripLocationController {

    private final TripLocationService service;

    public TripLocationController(TripLocationService service) {
        this.service = service;
    }

    @PostMapping("/{tripId}/locations")
    @PreAuthorize("hasAuthority('SUBMIT_OWN_TRIP_LOCATION')")
    public ResponseEntity<ApiResponse<TripLocationResponse>> submitLocation(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "tripId phải là số dương.") Long tripId,
            @Valid @RequestBody TripLocationRequest request) {
        TripLocationResponse data = service.submitLocation(principal.getId(), tripId, request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                data,
                "Đã ghi nhận vị trí chuyến đi."));
    }
}
