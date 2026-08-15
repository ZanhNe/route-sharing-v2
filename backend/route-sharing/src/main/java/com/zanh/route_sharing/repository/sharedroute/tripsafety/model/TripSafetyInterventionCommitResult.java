package com.zanh.route_sharing.repository.sharedroute.tripsafety.model;

import java.util.List;

public record TripSafetyInterventionCommitResult(
        TripSafetyInterventionSnapshot snapshot,
        boolean changed,
        List<Long> tripStateRealtimeRecipientUserIds,
        List<Long> participantRealtimeRecipientUserIds) {
    public TripSafetyInterventionCommitResult {
        if (snapshot == null) throw new IllegalArgumentException("snapshot không được trống.");
        tripStateRealtimeRecipientUserIds = tripStateRealtimeRecipientUserIds == null ? List.of() : List.copyOf(tripStateRealtimeRecipientUserIds);
        participantRealtimeRecipientUserIds = participantRealtimeRecipientUserIds == null ? List.of() : List.copyOf(participantRealtimeRecipientUserIds);
    }
}
