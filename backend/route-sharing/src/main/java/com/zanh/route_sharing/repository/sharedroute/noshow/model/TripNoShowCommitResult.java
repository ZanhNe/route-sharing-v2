package com.zanh.route_sharing.repository.sharedroute.noshow.model;

import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.time.Instant;

public record TripNoShowCommitResult(
        Long tripId,
        Long routeId,
        Long rideRequestId,
        Long pickupStopId,
        Integer pickupStopOrder,
        Long dropoffStopId,
        Integer dropoffStopOrder,
        TrangThaiYeuCau bookingStatus,
        TrangThaiDiemDung pickupStatus,
        TrangThaiDiemDung dropoffStatus,
        Instant noShowAt,
        Long realtimeRecipientUserId) {
}
