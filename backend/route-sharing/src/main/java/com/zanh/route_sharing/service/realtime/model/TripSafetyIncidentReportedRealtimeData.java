package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record TripSafetyIncidentReportedRealtimeData(
                Long incidentId,
                Long tripId,
                String type,
                String severity,
                String status,
                Instant reportedAt) {
}
