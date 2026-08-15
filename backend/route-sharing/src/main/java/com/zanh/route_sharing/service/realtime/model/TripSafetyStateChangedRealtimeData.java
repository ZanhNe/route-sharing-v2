package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record TripSafetyStateChangedRealtimeData(
                Long tripId,
                Long interventionId,
                String changeType,
                String tripStatus,
                Instant changedAt) {
}
