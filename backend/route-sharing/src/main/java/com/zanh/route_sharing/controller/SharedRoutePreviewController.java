package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.preview.SharedRoutePreviewResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.SharedRoutePreviewService;
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
public class SharedRoutePreviewController {

    private final SharedRoutePreviewService previewService;

    public SharedRoutePreviewController(SharedRoutePreviewService previewService) {
        this.previewService = previewService;
    }

    @PostMapping("/{routeId}/preview")
    @PreAuthorize("hasAuthority('SEARCH_SHARED_ROUTE')")
    public ResponseEntity<ApiResponse<SharedRoutePreviewResponse>> preview(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive Long routeId,
            @Valid @RequestBody PreviewSharedRouteRequest request) {
        SharedRoutePreviewResponse result = previewService.preview(
                principal.getId(),
                routeId,
                request);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                result,
                "Tính phương án đi chung thành công."));
    }
}
