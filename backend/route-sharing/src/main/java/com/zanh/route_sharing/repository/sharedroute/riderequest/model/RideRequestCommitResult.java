package com.zanh.route_sharing.repository.sharedroute.riderequest.model;

import java.util.Objects;

public record RideRequestCommitResult(
        boolean created,
        RideRequestPersistedView persistedView) {

    public RideRequestCommitResult {
        Objects.requireNonNull(persistedView, "persistedView không được trống");
    }

    public static RideRequestCommitResult created(RideRequestPersistedView view) {
        return new RideRequestCommitResult(true, view);
    }

    public static RideRequestCommitResult replayed(RideRequestPersistedView view) {
        return new RideRequestCommitResult(false, view);
    }
}
