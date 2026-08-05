package com.zanh.route_sharing.repository.sharedroute.riderequest.model;

import java.util.Objects;

public record IdempotencyRecord(
        Long actorUserId,
        String key,
        String fingerprint,
        RideRequestPersistedView persistedView) {

    public IdempotencyRecord {
        Objects.requireNonNull(actorUserId, "actorUserId không được trống");
        Objects.requireNonNull(key, "key không được trống");
        Objects.requireNonNull(fingerprint, "fingerprint không được trống");
        Objects.requireNonNull(persistedView, "persistedView không được trống");
    }
}
