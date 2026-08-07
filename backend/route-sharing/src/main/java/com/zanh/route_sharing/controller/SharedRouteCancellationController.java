package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.sharedroute.cancellation.CancelSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.cancellation.CancelSharedRouteResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.SharedRouteCancellationService;
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
@RequestMapping("/api/v1/shared-routes")
@Validated
public class SharedRouteCancellationController {
    private final SharedRouteCancellationService service;

    public SharedRouteCancellationController(SharedRouteCancellationService service) {
        this.service = service;
    }

    @PostMapping("/{routeId}/cancel")
    @PreAuthorize("hasAuthority('CANCEL_OWN_SHARED_ROUTE')")
    public ResponseEntity<ApiResponse<CancelSharedRouteResponse>> cancel(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "routeId phải là số dương.") Long routeId,
            @Valid @RequestBody CancelSharedRouteRequest request) {
        CancelSharedRouteResponse data = service.cancelOwnedRoute(
                principal.getId(), routeId, request.reason());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), data, "Hủy lộ trình chia sẻ thành công."));
    }
}
