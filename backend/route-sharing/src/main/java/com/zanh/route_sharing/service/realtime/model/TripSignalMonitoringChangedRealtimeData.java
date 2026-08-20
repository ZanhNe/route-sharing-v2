package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;

public record TripSignalMonitoringChangedRealtimeData(
        Long tripId,
        String previousMonitoringStatus,
        String monitoringStatus,
        Instant signalReferenceAt,
        Instant changedAt) {
}
