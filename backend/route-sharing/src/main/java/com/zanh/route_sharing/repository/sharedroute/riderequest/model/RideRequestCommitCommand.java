package com.zanh.route_sharing.repository.sharedroute.riderequest.model;

import com.zanh.route_sharing.domain.riderequest.RideRequestSnapshot;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewConsistencyToken;

import java.time.Instant;
import java.util.Objects;

public record RideRequestCommitCommand(
        Long actorUserId,
        Long routeId,
        Instant sentAt,
        RideRequestSnapshot snapshot,
        String note,
        PreviewConsistencyToken consistencyToken) {

    public RideRequestCommitCommand {
        Objects.requireNonNull(actorUserId, "actorUserId không được trống");
        Objects.requireNonNull(routeId, "routeId không được trống");
        Objects.requireNonNull(sentAt, "sentAt không được trống");
        Objects.requireNonNull(snapshot, "snapshot không được trống");
        Objects.requireNonNull(consistencyToken, "consistencyToken không được trống");
    }
}
