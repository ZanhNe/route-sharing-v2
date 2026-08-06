package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.riderequest.decision.RideRequestDecisionResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.RideRequestDecisionService;
import com.zanh.route_sharing.service.riderequest.decision.RideRequestDecisionResponseMapper;
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
@RequestMapping("/api/v1/shared-routes")
@Validated
public class RideRequestDecisionController {

    private final RideRequestDecisionService service;
    private final RideRequestDecisionResponseMapper mapper;

    public RideRequestDecisionController(
            RideRequestDecisionService service,
            RideRequestDecisionResponseMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/{routeId}/ride-requests/{rideRequestId}/accept")
    @PreAuthorize("hasAuthority('RESPOND_RIDE_REQUEST')")
    public ResponseEntity<ApiResponse<RideRequestDecisionResponse>> accept(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "routeId phải là số dương.") Long routeId,
            @PathVariable @Positive(message = "rideRequestId phải là số dương.") Long rideRequestId) {
        RideRequestDecisionResponse data = mapper.toResponse(
                service.accept(principal.getId(), routeId, rideRequestId));
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                data,
                "Chấp nhận yêu cầu đi chung thành công."));
    }

    @PostMapping("/{routeId}/ride-requests/{rideRequestId}/reject")
    @PreAuthorize("hasAuthority('RESPOND_RIDE_REQUEST')")
    public ResponseEntity<ApiResponse<RideRequestDecisionResponse>> reject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "routeId phải là số dương.") Long routeId,
            @PathVariable @Positive(message = "rideRequestId phải là số dương.") Long rideRequestId) {
        RideRequestDecisionResponse data = mapper.toResponse(
                service.reject(principal.getId(), routeId, rideRequestId));
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                data,
                "Từ chối yêu cầu đi chung thành công."));
    }
}
