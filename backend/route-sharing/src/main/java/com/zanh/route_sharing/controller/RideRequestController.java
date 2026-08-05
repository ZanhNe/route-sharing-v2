package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.dto.riderequest.RideRequestResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.RideRequestCreationService;
import com.zanh.route_sharing.service.riderequest.model.RideRequestCreationResult;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/shared-routes")
@Validated
public class RideRequestController {

    public static final String IDEMPOTENCY_REPLAYED_HEADER = "Idempotency-Replayed";

    private final RideRequestCreationService creationService;

    public RideRequestController(RideRequestCreationService creationService) {
        this.creationService = creationService;
    }

    @PostMapping("/{routeId}/ride-requests")
    @PreAuthorize("hasAuthority('CREATE_RIDE_REQUEST')")
    public ResponseEntity<ApiResponse<RideRequestResponse>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "routeId phải là số dương.") Long routeId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateRideRequestRequest request) {
        RideRequestCreationResult result = creationService.create(
                principal.getId(),
                routeId,
                idempotencyKey,
                request);

        RideRequestResponse data = result.response();
        URI location = URI.create("/api/v1/ride-requests/" + data.rideRequestId());
        ApiResponse<RideRequestResponse> body = ApiResponse.success(
                HttpStatus.CREATED.value(),
                data,
                "Gửi yêu cầu đi chung thành công. "
                        + "Yêu cầu đang chờ tài xế xử lý và chưa giữ ghế.");

        return ResponseEntity.created(location)
                .header(IDEMPOTENCY_REPLAYED_HEADER, Boolean.toString(result.replayed()))
                .body(body);
    }
}
