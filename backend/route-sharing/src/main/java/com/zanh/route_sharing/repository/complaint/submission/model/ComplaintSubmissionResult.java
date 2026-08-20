package com.zanh.route_sharing.repository.complaint.submission.model;

import com.zanh.route_sharing.domain.enums.TrangThaiKhieuNai;

import java.time.Instant;

public record ComplaintSubmissionResult(
        Long complaintId,
        Long tripId,
        Long rideRequestId,
        Long targetUserId,
        TrangThaiKhieuNai status,
        Instant submittedAt,
        Instant filingDeadlineApplied,
        Long incidentId) {
}
