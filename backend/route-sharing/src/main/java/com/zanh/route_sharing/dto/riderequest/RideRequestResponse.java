package com.zanh.route_sharing.dto.riderequest;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.math.BigDecimal;
import java.time.Instant;

public record RideRequestResponse(
        Long rideRequestId,
        Long routeId,
        TrangThaiYeuCau status,
        Instant sentAt,
        boolean seatReserved,
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,
        RideRequestPointResponse pickup,
        RideRequestPointResponse passengerDestination,
        RideRequestPointResponse proposedDropoff,
        BigDecimal pickupDeviationMeters,
        long pickupDeviationSeconds,
        BigDecimal passengerDesiredDistanceMeters,
        BigDecimal servedDistanceMeters,
        BigDecimal remainingDistanceMeters,
        BigDecimal convenienceRatioPercent,
        BigDecimal suggestedSupportPerKm,
        BigDecimal proposedSupportAmount,
        BigDecimal agreedSupportAmount) {
}
