package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.formation.TripFormationResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.TripFormationService;
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
public class SharedRouteTripFormationController {

    private final TripFormationService service;

    public SharedRouteTripFormationController(TripFormationService service) {
        this.service = service;
    }

    @PostMapping("/{routeId}/lock")
    @PreAuthorize("hasAuthority('LOCK_OWN_SHARED_ROUTE')")
    public ResponseEntity<ApiResponse<TripFormationResponse>> formTrip(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "routeId phải là số dương.") Long routeId) {
        TripFormationResponse data = service.formTrip(principal.getId(), routeId);
        boolean created = "CREATED".equals(data.formationOutcome());
        HttpStatus status = created ? HttpStatus.CREATED : HttpStatus.OK;
        String message = created
                ? "Khóa danh sách và hình thành chuyến đi thành công."
                : "Chuyến đi đã được hình thành trước đó.";
        return ResponseEntity.status(status)
                .body(ApiResponse.success(status.value(), data, message));
    }
}
