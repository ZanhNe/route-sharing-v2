package com.zanh.route_sharing.repository.sharedroute.preview.model;

import java.util.Objects;

public record PreviewDriverSnapshot(
        Long id,
        String fullName,
        String avatarUrl) {

    public PreviewDriverSnapshot {
        Objects.requireNonNull(id, "id không được trống");
        Objects.requireNonNull(fullName, "fullName không được trống");
    }
}
