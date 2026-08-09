package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.sharedroute.driverquery.DriverSharedRouteDetailResponse;
import com.zanh.route_sharing.dto.sharedroute.driverquery.DriverSharedRoutePageResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.DriverSharedRouteQueryService;
import com.zanh.route_sharing.service.sharedroute.driverquery.model.DriverSharedRoutePageResult;
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
public class DriverSharedRouteQueryController {

    private final DriverSharedRouteQueryService queryService;

    public DriverSharedRouteQueryController(DriverSharedRouteQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_OWN_SHARED_ROUTES')")
    public ResponseEntity<ApiResponse<DriverSharedRoutePageResponse>> listOwnRoutes(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) TrangThaiLoTrinh status,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page phải lớn hơn hoặc bằng 0.") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size phải lớn hơn hoặc bằng 1.")
            @Max(value = 50, message = "size không được vượt quá 50.") int size) {
        DriverSharedRoutePageResult result = queryService.listOwnRoutes(
                principal.getId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(),
                result.data(),
                "Lấy danh sách lộ trình của bạn thành công.",
                result.meta()));
    }

    @GetMapping("/{routeId}")
    @PreAuthorize("hasAuthority('VIEW_OWN_SHARED_ROUTES')")
    public ResponseEntity<ApiResponse<DriverSharedRouteDetailResponse>> getOwnRouteDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "routeId phải là số dương.") Long routeId) {
        DriverSharedRouteDetailResponse data = queryService.getOwnRouteDetail(principal.getId(), routeId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                data,
                "Lấy chi tiết lộ trình của bạn thành công."));
    }
}
