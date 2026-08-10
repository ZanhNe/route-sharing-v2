package com.zanh.route_sharing.repository.sharedroute.tripstart.model;

import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;

import java.time.Instant;
import java.util.List;

public record TripStartCommitResult(
        Long tripId,
        Long routeId,
        TrangThaiVanHanhChuyenDi tripStatus,
        Instant startedAt,
        Integer actualPassengerCount,
        Long driverStartStopId,
        TrangThaiDiemDung driverStartStatus,
        List<Long> realtimeRecipientUserIds,
        List<Long> realtimeRideRequestIds) {

    public TripStartCommitResult {
        realtimeRecipientUserIds = realtimeRecipientUserIds == null
                ? List.of()
                : List.copyOf(realtimeRecipientUserIds);
        realtimeRideRequestIds = realtimeRideRequestIds == null
                ? List.of()
                : List.copyOf(realtimeRideRequestIds);
        if (realtimeRecipientUserIds.size() != realtimeRideRequestIds.size()) {
            throw new IllegalArgumentException("Recipient và rideRequest realtime phải cùng số lượng.");
        }
    }
}
