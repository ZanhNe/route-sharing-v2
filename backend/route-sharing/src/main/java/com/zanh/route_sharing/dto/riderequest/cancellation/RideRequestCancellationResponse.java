package com.zanh.route_sharing.dto.riderequest.cancellation;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.time.Instant;

public record RideRequestCancellationResponse(
        Long routeId,
        Long rideRequestId,
        TrangThaiYeuCau previousStatus,
        TrangThaiYeuCau status,
        Instant cancelledAt,
        Integer remainingSeats,
        String reason) {
}
