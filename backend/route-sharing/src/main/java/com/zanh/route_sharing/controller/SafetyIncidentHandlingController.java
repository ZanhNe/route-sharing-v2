package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.safety.*;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.SafetyIncidentHandlingService;
import com.zanh.route_sharing.service.TripSafetyInterventionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/safety/incidents")
@Validated
public class SafetyIncidentHandlingController {
    private final SafetyIncidentHandlingService service;
    private final TripSafetyInterventionService interventionService;

    public SafetyIncidentHandlingController(SafetyIncidentHandlingService service,
            TripSafetyInterventionService interventionService) {
        this.service = service;
        this.interventionService = interventionService;
    }

    @PostMapping("/{incidentId}/claim")
    @PreAuthorize("hasAuthority('HANDLE_INCIDENT')")
    public ResponseEntity<ApiResponse<SafetyIncidentHandlingResponse>> claim(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable @Positive Long incidentId) {
        return ok(service.claim(principal.getId(), incidentId), "Tiếp nhận incident thành công.");
    }

    @PostMapping("/{incidentId}/investigate")
    @PreAuthorize("hasAuthority('HANDLE_INCIDENT')")
    public ResponseEntity<ApiResponse<SafetyIncidentHandlingResponse>> investigate(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable @Positive Long incidentId) {
        return ok(service.investigate(principal.getId(), incidentId),
                "Chuyển incident sang trạng thái điều tra thành công.");
    }

    @PostMapping("/{incidentId}/finalize")
    @PreAuthorize("hasAuthority('HANDLE_INCIDENT')")
    public ResponseEntity<ApiResponse<SafetyIncidentHandlingResponse>> finalizeIncident(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable @Positive Long incidentId,
            @Valid @RequestBody SafetyIncidentFinalizeRequest request) {
        return ok(service.finalizeIncident(principal.getId(), incidentId, request),
                "Hoàn tất xử lý incident thành công.");
    }

    @PostMapping("/{incidentId}/reassign")
    @PreAuthorize("hasAuthority('HANDLE_INCIDENT') and hasAuthority('REASSIGN_INCIDENT')")
    public ResponseEntity<ApiResponse<SafetyIncidentHandlingResponse>> reassign(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable @Positive Long incidentId,
            @Valid @RequestBody SafetyIncidentReassignRequest request) {
        return ok(service.reassign(principal.getId(), incidentId, request),
                "Chuyển người phụ trách incident thành công.");
    }

    @PostMapping("/{incidentId}/emergency-abort-trip")
    @PreAuthorize("hasAuthority('HANDLE_INCIDENT') and hasAuthority('INTERVENE_TRIP_SAFETY')")
    public ResponseEntity<ApiResponse<TripSafetyInterventionResponse>> emergencyAbortTrip(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable @Positive Long incidentId) {
        return ok(interventionService.abortTripBySafety(principal.getId(), incidentId),
                "Kết thúc Trip khẩn cấp thành công.");
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), data, message));
    }
}
