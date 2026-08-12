package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.noshow.TripNoShowResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.TripNoShowService;
import jakarta.validation.constraints.Positive;
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
public class TripNoShowController {
    private final TripNoShowService service;

    public TripNoShowController(TripNoShowService service) {
        this.service = service;
    }

    @PostMapping("/{tripId}/no-show")
    @PreAuthorize("hasAuthority('CONFIRM_OWN_TRIP_NO_SHOW')")
    public ResponseEntity<ApiResponse<TripNoShowResponse>> confirmNoShow(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "tripId phải là số dương.") Long tripId) {
        TripNoShowResponse data = service.confirmNoShow(principal.getId(), tripId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), data, "Xác nhận Passenger no-show thành công."));
    }
}
