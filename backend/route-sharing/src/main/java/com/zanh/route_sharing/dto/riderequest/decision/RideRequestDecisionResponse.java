package com.zanh.route_sharing.dto.riderequest.decision;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.math.BigDecimal;
import java.time.Instant;

public record RideRequestDecisionResponse(
        Long routeId,
        Long rideRequestId,
        TrangThaiYeuCau status,
        Instant decisionAt,
        Integer remainingSeats,
        BigDecimal agreedSupportAmount,
        Instant cooldownUntil) {
}
