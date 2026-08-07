package com.zanh.route_sharing.service.sharedroute.cancellation.model;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;

import java.time.Instant;

public record SharedRouteCancellationResult(
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
