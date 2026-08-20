package com.zanh.route_sharing.dto.complaint.review;

import java.time.Instant;

public record ComplaintInvestigationContextResponse(Long complaintId, Long tripId, String tripStatus,
                Long rideRequestId, String rideRequestStatus, Long driverId, Long passengerId,
                long stopCount, Long linkedIncidentId, String linkedIncidentType, String linkedIncidentStatus,
                Instant tripEndedAt) {
}
