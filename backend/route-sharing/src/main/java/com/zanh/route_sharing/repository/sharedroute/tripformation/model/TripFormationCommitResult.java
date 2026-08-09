package com.zanh.route_sharing.repository.sharedroute.tripformation.model;

import java.util.List;

public record TripFormationCommitResult(
        boolean created,
        TripFormationPersistedView view,
        List<Long> realtimeRecipientUserIds,
        List<Long> realtimeRideRequestIds) {

    public TripFormationCommitResult {
        realtimeRecipientUserIds = realtimeRecipientUserIds == null
                ? List.of()
                : List.copyOf(realtimeRecipientUserIds);
        realtimeRideRequestIds = realtimeRideRequestIds == null
                ? List.of()
                : List.copyOf(realtimeRideRequestIds);
        if (realtimeRecipientUserIds.size() != realtimeRideRequestIds.size()) {
            throw new IllegalArgumentException("Realtime recipient/request mapping không khớp");
        }
    }
}
