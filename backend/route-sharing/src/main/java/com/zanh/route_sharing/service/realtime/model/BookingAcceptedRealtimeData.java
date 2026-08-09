package com.zanh.route_sharing.service.realtime.model;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingAcceptedRealtimeData(
        Long rideRequestId,
        Long routeId,
        String status,
        Instant decisionAt,
        BigDecimal agreedSupportAmount) {
}
