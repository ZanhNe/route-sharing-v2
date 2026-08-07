package com.zanh.route_sharing.service.riderequest.cancellation.model;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.time.Instant;

public record RideRequestCancellationResult(
                Long routeId,
                Long rideRequestId,
                TrangThaiYeuCau previousStatus,
                TrangThaiYeuCau status,
                Instant cancelledAt,
                Integer remainingSeats,
                String reason) {
}
