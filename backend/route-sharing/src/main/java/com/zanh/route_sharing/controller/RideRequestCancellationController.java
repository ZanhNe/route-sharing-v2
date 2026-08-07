package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.riderequest.cancellation.RideRequestCancellationRequest;
import com.zanh.route_sharing.dto.riderequest.cancellation.RideRequestCancellationResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.RideRequestCancellationService;
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
@RequestMapping("/api/v1")
@Validated
public class RideRequestCancellationController {
        private final RideRequestCancellationService service;

        public RideRequestCancellationController(RideRequestCancellationService service) {
                this.service = service;
        }

        @PostMapping("/ride-requests/{rideRequestId}/cancel")
        @PreAuthorize("hasAuthority('CANCEL_OWN_RIDE_REQUEST')")
        public ResponseEntity<ApiResponse<RideRequestCancellationResponse>> cancelByPassenger(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive(message = "rideRequestId phải là số dương.") Long rideRequestId,
                        @Valid @RequestBody RideRequestCancellationRequest request) {
                RideRequestCancellationResponse data = service.cancelByPassenger(
                                principal.getId(), rideRequestId, request.reason());
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                                data, "Hủy yêu cầu đi chung thành công."));
        }

}
