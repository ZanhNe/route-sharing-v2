package com.zanh.route_sharing.repository.sharedroute.tripsafety.model;

import com.zanh.route_sharing.domain.enums.LoaiSuCo;
import com.zanh.route_sharing.domain.enums.MucDoSuCo;
import com.zanh.route_sharing.domain.enums.NguonPhatHienSuCo;
import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;

import java.time.Instant;
import java.util.List;

public record TripSafetyIncidentCommitResult(
        Long incidentId,
        Long tripId,
        LoaiSuCo type,
        MucDoSuCo severity,
        TrangThaiXuLySuCo status,
        NguonPhatHienSuCo reporterSource,
        Instant reportedAt,
        boolean createdNew,
        List<Long> realtimeRecipientUserIds,
        TripSafetyInterventionSnapshot intervention,
        boolean interventionChanged,
        List<Long> tripStateRealtimeRecipientUserIds,
        List<Long> participantRealtimeRecipientUserIds) {
    public TripSafetyIncidentCommitResult {
        realtimeRecipientUserIds = realtimeRecipientUserIds == null ? List.of() : List.copyOf(realtimeRecipientUserIds);
        tripStateRealtimeRecipientUserIds = tripStateRealtimeRecipientUserIds == null ? List.of() : List.copyOf(tripStateRealtimeRecipientUserIds);
        participantRealtimeRecipientUserIds = participantRealtimeRecipientUserIds == null ? List.of() : List.copyOf(participantRealtimeRecipientUserIds);
    }

    public TripSafetyIncidentCommitResult(Long incidentId, Long tripId, LoaiSuCo type, MucDoSuCo severity,
                                          TrangThaiXuLySuCo status, NguonPhatHienSuCo reporterSource,
                                          Instant reportedAt, boolean createdNew, List<Long> realtimeRecipientUserIds) {
        this(incidentId, tripId, type, severity, status, reporterSource, reportedAt, createdNew,
                realtimeRecipientUserIds, null, false, List.of(), List.of());
    }
}
