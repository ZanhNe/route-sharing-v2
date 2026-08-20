package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.complaint.submission.SubmitComplaintRequest;
import com.zanh.route_sharing.dto.complaint.submission.SubmitComplaintResponse;
import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.ComplaintSubmissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@Validated
public class ComplaintSubmissionController {
    private final ComplaintSubmissionService service;

    public ComplaintSubmissionController(ComplaintSubmissionService service) {
        this.service = service;
    }

    @PostMapping("/{tripId}/ride-requests/{rideRequestId}/complaints")
    @PreAuthorize("hasAuthority('SUBMIT_OWN_COMPLAINT')")
    public ResponseEntity<ApiResponse<SubmitComplaintResponse>> submit(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive(message = "tripId phải là số dương.") Long tripId,
            @PathVariable @Positive(message = "rideRequestId phải là số dương.") Long rideRequestId,
            @Valid @RequestBody SubmitComplaintRequest request) {
        SubmitComplaintResponse data = service.submit(principal.getId(), tripId, rideRequestId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), data, "Nộp khiếu nại thành công."));
    }
}
