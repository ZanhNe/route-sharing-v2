package com.zanh.route_sharing.repository.sharedroute.pickuparrival.model;

import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.time.Instant;

public record TripPickupArrivalCommitResult(
        Long tripId,
        Long routeId,
        TrangThaiVanHanhChuyenDi tripStatus,
        Long rideRequestId,
        TrangThaiYeuCau bookingStatus,
        Integer actualPassengerCount,
        Long pickupStopId,
        Integer pickupStopOrder,
        TrangThaiDiemDung pickupStatus,
        Instant arrivedAt,
        Instant waitingStartedAt,
        Instant waitingDeadline,
        Long realtimeRecipientUserId) {
}
