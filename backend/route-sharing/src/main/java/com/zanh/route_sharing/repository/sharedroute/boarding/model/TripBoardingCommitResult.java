package com.zanh.route_sharing.repository.sharedroute.boarding.model;

import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.time.Instant;

public record TripBoardingCommitResult(
        Long tripId,
        Long routeId,
        Long rideRequestId,
        Long pickupStopId,
        Integer pickupStopOrder,
        TrangThaiYeuCau bookingStatus,
        TrangThaiDiemDung pickupStatus,
        Instant boardedAt,
        Integer actualPassengerCount,
        Long realtimeRecipientUserId) {
}
