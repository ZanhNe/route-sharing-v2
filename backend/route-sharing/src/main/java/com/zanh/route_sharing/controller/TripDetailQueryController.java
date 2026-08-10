package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.detail.TripDetailResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.TripDetailQueryService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@Validated
public class TripDetailQueryController {

    private final TripDetailQueryService queryService;

    public TripDetailQueryController(TripDetailQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{tripId}")
    @PreAuthorize("hasAuthority('VIEW_OWN_TRIP')")
    public ResponseEntity<ApiResponse<TripDetailResponse>> getTripDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "tripId phải là số dương.") Long tripId) {
        TripDetailResponse data = queryService.getTripDetail(principal.getId(), tripId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                data,
                "Lấy chi tiết kế hoạch chuyến thành công."));
    }
}
