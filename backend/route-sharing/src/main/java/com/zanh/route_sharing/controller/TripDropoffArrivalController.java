package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.dropoffarrival.TripDropoffArrivalRequest;
import com.zanh.route_sharing.dto.trip.dropoffarrival.TripDropoffArrivalResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.TripDropoffArrivalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@Validated
public class TripDropoffArrivalController {
    private final TripDropoffArrivalService service;

    public TripDropoffArrivalController(TripDropoffArrivalService service) {
        this.service = service;
    }

    @PostMapping("/{tripId}/dropoff-arrival")
    @PreAuthorize("hasAuthority('CONFIRM_OWN_TRIP_DROPOFF_ARRIVAL')")
    public ResponseEntity<ApiResponse<TripDropoffArrivalResponse>> confirmArrival(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "tripId phải là số dương.") Long tripId,
            @Valid @RequestBody TripDropoffArrivalRequest request) {
        TripDropoffArrivalResponse data = service.confirmArrival(principal.getId(), tripId, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), data,
                "Ghi nhận tài xế đã đến điểm trả khách thành công."));
    }
}
