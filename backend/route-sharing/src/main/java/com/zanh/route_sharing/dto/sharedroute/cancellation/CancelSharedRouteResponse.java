package com.zanh.route_sharing.dto.sharedroute.cancellation;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;

import java.time.Instant;

public record CancelSharedRouteResponse(
        Long routeId,
        TrangThaiLoTrinh previousStatus,
        TrangThaiLoTrinh status,
        Instant cancelledAt,
        String reason,
        int pendingRequestsCancelled,
        int acceptedBookingsCancelled,
        int seatsRestored,
        int passengersNotified) {
}
