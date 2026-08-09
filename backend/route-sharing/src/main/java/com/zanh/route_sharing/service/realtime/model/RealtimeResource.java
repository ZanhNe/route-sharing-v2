package com.zanh.route_sharing.service.realtime.model;

import java.util.Objects;

public record RealtimeResource(String type, Long id) {
    public RealtimeResource {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Realtime resource type không được trống.");
        }
        Objects.requireNonNull(id, "Realtime resource id không được trống.");
        if (id <= 0) {
            throw new IllegalArgumentException("Realtime resource id phải là số dương.");
        }
    }
}
