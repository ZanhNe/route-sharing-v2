package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.pickuparrival.TripPickupArrivalRequest;
import com.zanh.route_sharing.dto.trip.pickuparrival.TripPickupArrivalResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.TripPickupArrivalService;
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
public class TripPickupArrivalController {

    private final TripPickupArrivalService service;

    public TripPickupArrivalController(TripPickupArrivalService service) {
        this.service = service;
    }

    @PostMapping("/{tripId}/pickup-arrival")
    @PreAuthorize("hasAuthority('CONFIRM_OWN_TRIP_PICKUP_ARRIVAL')")
    public ResponseEntity<ApiResponse<TripPickupArrivalResponse>> confirmArrival(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "tripId phải là số dương.") Long tripId,
            @Valid @RequestBody TripPickupArrivalRequest request) {
        TripPickupArrivalResponse data = service.confirmArrival(principal.getId(), tripId, request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                data,
                "Ghi nhận tài xế đã đến điểm đón thành công."));
    }
}
