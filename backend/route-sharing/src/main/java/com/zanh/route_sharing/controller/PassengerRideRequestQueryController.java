package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.riderequest.passengerquery.PassengerRideRequestDetailResponse;
import com.zanh.route_sharing.dto.riderequest.passengerquery.PassengerRideRequestPageResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.PassengerRideRequestQueryService;
import com.zanh.route_sharing.service.riderequest.passengerquery.model.PassengerRideRequestPageResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ride-requests")
@Validated
public class PassengerRideRequestQueryController {

    private final PassengerRideRequestQueryService queryService;

    public PassengerRideRequestQueryController(PassengerRideRequestQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_OWN_RIDE_REQUESTS')")
    public ResponseEntity<ApiResponse<PassengerRideRequestPageResponse>> listOwnRideRequests(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) TrangThaiYeuCau status,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page phải lớn hơn hoặc bằng 0.") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size phải lớn hơn hoặc bằng 1.")
            @Max(value = 50, message = "size không được vượt quá 50.") int size) {
        PassengerRideRequestPageResult result = queryService.listOwnRideRequests(
                principal.getId(),
                status,
                page,
                size);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(),
                result.data(),
                "Lấy danh sách yêu cầu đi chung của bạn thành công.",
                result.meta()));
    }

    @GetMapping("/{rideRequestId}")
    @PreAuthorize("hasAuthority('VIEW_OWN_RIDE_REQUESTS')")
    public ResponseEntity<ApiResponse<PassengerRideRequestDetailResponse>> getOwnRideRequestDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "rideRequestId phải là số dương.") Long rideRequestId) {
        PassengerRideRequestDetailResponse data = queryService.getOwnRideRequestDetail(
                principal.getId(),
                rideRequestId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                data,
                "Lấy chi tiết yêu cầu đi chung của bạn thành công."));
    }
}
