package com.zanh.route_sharing.service.realtime.model;

public record TripSafetyIncidentWorkChangedRealtimeData(
                Long incidentId,
                Long tripId,
                String changeType,
                String status,
                Long primaryHandlerUserId) {
}
