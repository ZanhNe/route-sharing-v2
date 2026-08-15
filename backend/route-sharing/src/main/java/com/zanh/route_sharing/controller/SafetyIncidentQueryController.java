package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.domain.enums.MucDoSuCo;
import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.trip.safety.*;
import com.zanh.route_sharing.security.ClientRequestInfo;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.SafetyIncidentQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/safety/incidents")
@Validated
public class SafetyIncidentQueryController {
    private final SafetyIncidentQueryService service;

    public SafetyIncidentQueryController(SafetyIncidentQueryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HANDLE_INCIDENT')")
    public ResponseEntity<ApiResponse<SafetyIncidentQueueResponse>> queue(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) @Positive Long schoolId,
            @RequestParam(required = false) TrangThaiXuLySuCo status,
            @RequestParam(required = false) MucDoSuCo severity,
            @RequestParam(defaultValue = "ALL") String ownership,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ok(service.getQueue(principal.getId(), schoolId, status, severity, ownership, page, size),
                "Lấy Safety incident queue thành công.");
    }

    @GetMapping("/{incidentId}")
    @PreAuthorize("hasAuthority('HANDLE_INCIDENT')")
    public ResponseEntity<ApiResponse<SafetyIncidentSummaryResponse>> getSummary(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "incidentId phải là số dương.") Long incidentId) {
        return ok(service.getSummary(principal.getId(), incidentId), "Lấy safety incident thành công.");
    }

    @GetMapping("/{incidentId}/case")
    @PreAuthorize("hasAuthority('HANDLE_INCIDENT')")
    public ResponseEntity<ApiResponse<SafetyIncidentCaseResponse>> getCase(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable @Positive Long incidentId,
            HttpServletRequest request) {
        return ok(service.getCase(principal.getId(), incidentId, clientInfo(request)),
                "Lấy chi tiết xử lý incident thành công.");
    }

    @GetMapping("/{incidentId}/eligible-handlers")
    @PreAuthorize("hasAuthority('HANDLE_INCIDENT') and hasAuthority('REASSIGN_INCIDENT')")
    public ResponseEntity<ApiResponse<SafetyEligibleHandlersResponse>> eligibleHandlers(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable @Positive Long incidentId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ok(service.getEligibleHandlers(principal.getId(), incidentId, page, size),
                "Lấy danh sách nhân sự Safety phù hợp thành công.");
    }

    @GetMapping("/{incidentId}/investigation-context")
    @PreAuthorize("hasAuthority('HANDLE_INCIDENT') and hasAuthority('VIEW_SAFETY_INVESTIGATION_EVIDENCE')")
    public ResponseEntity<ApiResponse<SafetyInvestigationContextResponse>> investigationContext(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable @Positive Long incidentId,
            HttpServletRequest request) {
        return ok(service.getInvestigationContext(principal.getId(), incidentId, clientInfo(request)),
                "Lấy ngữ cảnh điều tra incident thành công.");
    }

    @GetMapping("/{incidentId}/location-evidence")
    @PreAuthorize("hasAuthority('HANDLE_INCIDENT') and hasAuthority('VIEW_SAFETY_INVESTIGATION_EVIDENCE')")
    public ResponseEntity<ApiResponse<SafetyLocationEvidenceResponse>> locationEvidence(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable @Positive Long incidentId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(50) int size,
            HttpServletRequest request) {
        return ok(service.getLocationEvidence(principal.getId(), incidentId, from, to, page, size, clientInfo(request)),
                "Lấy location evidence thành công.");
    }

    private static ClientRequestInfo clientInfo(HttpServletRequest request) {
        return new ClientRequestInfo(request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT));
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), data, message));
    }
}
