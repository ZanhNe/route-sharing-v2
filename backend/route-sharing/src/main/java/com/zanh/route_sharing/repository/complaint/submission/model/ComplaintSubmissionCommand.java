package com.zanh.route_sharing.repository.complaint.submission.model;

public record ComplaintSubmissionCommand(
        Long actorId,
        Long tripId,
        Long rideRequestId,
        String title,
        String content,
        Long incidentId) {
}
