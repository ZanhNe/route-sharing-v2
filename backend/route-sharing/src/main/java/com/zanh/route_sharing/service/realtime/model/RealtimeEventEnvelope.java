package com.zanh.route_sharing.service.realtime.model;

import java.time.Instant;
import java.util.Objects;

public record RealtimeEventEnvelope<T>(
        String eventType,
        int eventVersion,
        Instant occurredAt,
        RealtimeResource resource,
        T data) {

    public RealtimeEventEnvelope {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Realtime eventType không được trống.");
        }
        if (eventVersion < 1) {
            throw new IllegalArgumentException("Realtime eventVersion phải >= 1.");
        }
        Objects.requireNonNull(occurredAt, "Realtime occurredAt không được trống.");
        Objects.requireNonNull(resource, "Realtime resource không được trống.");
        Objects.requireNonNull(data, "Realtime data không được trống.");
    }
}
