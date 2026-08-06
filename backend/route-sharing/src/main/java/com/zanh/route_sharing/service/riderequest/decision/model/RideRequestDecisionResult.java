package com.zanh.route_sharing.service.riderequest.decision.model;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.math.BigDecimal;
import java.time.Instant;

public record RideRequestDecisionResult(
        Long routeId,
        Long rideRequestId,
        TrangThaiYeuCau status,
        Instant decisionAt,
        int remainingSeats,
        BigDecimal agreedSupportAmount,
        Instant cooldownUntil) {
}
