package com.zanh.route_sharing.repository.sharedroute.tripcancellation.model;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;

import java.time.Instant;
import java.util.List;

public record TripCancellationCommitResult(
        Long tripId,
        Long routeId,
        TrangThaiVanHanhChuyenDi tripStatus,
        TrangThaiLoTrinh routeStatus,
        Instant cancelledAt,
        String reason,
        int affectedBookingCount,
        List<Long> realtimeRecipientUserIds,
        List<Long> realtimeRideRequestIds) {

    public TripCancellationCommitResult {
        realtimeRecipientUserIds = realtimeRecipientUserIds == null
                ? List.of()
                : List.copyOf(realtimeRecipientUserIds);
        realtimeRideRequestIds = realtimeRideRequestIds == null
                ? List.of()
                : List.copyOf(realtimeRideRequestIds);
        if (realtimeRecipientUserIds.size() != realtimeRideRequestIds.size()) {
            throw new IllegalArgumentException("Recipient và rideRequest realtime phải cùng số lượng.");
        }
        if (affectedBookingCount != realtimeRecipientUserIds.size()) {
            throw new IllegalArgumentException("Số booking bị ảnh hưởng phải khớp số recipient realtime.");
        }
    }
}
