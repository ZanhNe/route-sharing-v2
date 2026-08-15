package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.safety.TripSafetyIncidentRequest;
import com.zanh.route_sharing.dto.trip.safety.TripSafetyIncidentResponse;
import com.zanh.route_sharing.dto.trip.safety.ReporterSafetyIncidentStatusResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.TripSafetyIncidentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@Validated
public class TripSafetyIncidentController {
    private final TripSafetyIncidentService service;

    public TripSafetyIncidentController(TripSafetyIncidentService service) {
        this.service = service;
    }

    @PostMapping("/{tripId}/incidents")
    @PreAuthorize("hasAuthority('REPORT_OWN_TRIP_INCIDENT')")
    public ResponseEntity<ApiResponse<TripSafetyIncidentResponse>> report(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "tripId phải là số dương.") Long tripId,
            @Valid @RequestBody TripSafetyIncidentRequest request) {
        var result = service.report(principal.getId(), tripId, request);
        HttpStatus status = result.createdNew() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.createdNew()
                ? "Ghi nhận báo cáo sự cố/SOS thành công."
                : "SOS đang mở đã được ghi nhận trước đó.";
        return ResponseEntity.status(status)
                .body(ApiResponse.success(status.value(), result.response(), message));
    }

    @GetMapping("/{tripId}/incidents/{incidentId}")
    @PreAuthorize("hasAuthority('REPORT_OWN_TRIP_INCIDENT')")
    public ResponseEntity<ApiResponse<ReporterSafetyIncidentStatusResponse>> getOwnIncidentStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive Long tripId,
            @PathVariable @Positive Long incidentId) {
        var data = service.getOwnIncidentStatus(principal.getId(), tripId, incidentId);
        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), data, "Lấy trạng thái báo cáo sự cố thành công."));
    }

}
