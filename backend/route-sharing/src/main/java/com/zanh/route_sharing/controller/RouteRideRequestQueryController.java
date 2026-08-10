package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.utils.PaginationPolicy;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestDetailResponse;
import com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestPageResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.RouteRideRequestQueryService;
import com.zanh.route_sharing.service.riderequest.query.model.RouteRideRequestPageResult;
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
@RequestMapping("/api/v1/shared-routes")
@Validated
public class RouteRideRequestQueryController {

        private final RouteRideRequestQueryService queryService;

        public RouteRideRequestQueryController(RouteRideRequestQueryService queryService) {
                this.queryService = queryService;
        }

        @GetMapping("/{routeId}/ride-requests")
        @PreAuthorize("hasAuthority('VIEW_ROUTE_RIDE_REQUESTS')")
        public ResponseEntity<ApiResponse<RouteRideRequestPageResponse>> listPending(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive(message = "routeId phải là số dương.") Long routeId,
                        @RequestParam(defaultValue = "0") @Min(value = 0, message = "page phải lớn hơn hoặc bằng 0.") int page,
                        @RequestParam(defaultValue = "10") @Min(value = 1, message = "size phải lớn hơn hoặc bằng 1.") @Max(value = PaginationPolicy.MAX_SIZE, message = "size không được vượt quá 50.") int size) {
                RouteRideRequestPageResult result = queryService.listPending(
                                principal.getId(),
                                routeId,
                                page,
                                size);
                ApiResponse<RouteRideRequestPageResponse> body = ApiResponse.of(
                                HttpStatus.OK.value(),
                                result.data(),
                                "Lấy danh sách yêu cầu đi chung đang chờ xử lý thành công.",
                                result.meta());
                return ResponseEntity.ok(body);
        }

        @GetMapping("/{routeId}/ride-requests/{rideRequestId}")
        @PreAuthorize("hasAuthority('VIEW_ROUTE_RIDE_REQUESTS')")
        public ResponseEntity<ApiResponse<RouteRideRequestDetailResponse>> getPendingDetail(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @PathVariable @Positive(message = "routeId phải là số dương.") Long routeId,
                        @PathVariable @Positive(message = "rideRequestId phải là số dương.") Long rideRequestId) {
                RouteRideRequestDetailResponse data = queryService.getPendingDetail(
                                principal.getId(),
                                routeId,
                                rideRequestId);
                ApiResponse<RouteRideRequestDetailResponse> body = ApiResponse.success(
                                HttpStatus.OK.value(),
                                data,
                                "Lấy chi tiết yêu cầu đi chung đang chờ xử lý thành công.");
                return ResponseEntity.ok(body);
        }
}
