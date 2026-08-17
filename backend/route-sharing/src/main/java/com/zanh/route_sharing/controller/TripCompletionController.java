package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.completion.TripCompletionRequest;
import com.zanh.route_sharing.dto.trip.completion.TripCompletionResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.TripCompletionService;
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
public class TripCompletionController {
    private final TripCompletionService service;

    public TripCompletionController(TripCompletionService service) {
        this.service = service;
    }

    @PostMapping("/{tripId}/complete")
    @PreAuthorize("hasAuthority('COMPLETE_OWN_TRIP')")
    public ResponseEntity<ApiResponse<TripCompletionResponse>> completeTrip(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "tripId phải là số dương.") Long tripId,
            @Valid @RequestBody TripCompletionRequest request) {
        TripCompletionResponse data = service.completeTrip(principal.getId(), tripId, request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), data, "Kết thúc chuyến đi thành công."));
    }
}
