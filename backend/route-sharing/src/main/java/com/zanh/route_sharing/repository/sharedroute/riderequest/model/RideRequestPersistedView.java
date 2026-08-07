package com.zanh.route_sharing.repository.sharedroute.riderequest.model;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.domain.riderequest.RideRequestPointSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record RideRequestPersistedView(
        Long rideRequestId,
        Long routeId,
        TrangThaiYeuCau status,
        Instant sentAt,
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,
        RideRequestPointSnapshot pickup,
        RideRequestPointSnapshot passengerDestination,
        RideRequestPointSnapshot proposedDropoff,
        BigDecimal pickupDeviationMeters,
        long pickupDeviationSeconds,
        BigDecimal passengerDesiredDistanceMeters,
        BigDecimal servedDistanceMeters,
        BigDecimal remainingDistanceMeters,
        BigDecimal convenienceRatioPercent,
        BigDecimal suggestedSupportPerKm,
        BigDecimal proposedSupportAmount,
        BigDecimal agreedSupportAmount) {

    public RideRequestPersistedView {
        Objects.requireNonNull(rideRequestId, "rideRequestId không được trống");
        Objects.requireNonNull(routeId, "routeId không được trống");
        Objects.requireNonNull(status, "status không được trống");
        Objects.requireNonNull(sentAt, "sentAt không được trống");
        Objects.requireNonNull(matchType, "matchType không được trống");
        Objects.requireNonNull(dropoffType, "dropoffType không được trống");
        Objects.requireNonNull(pickup, "pickup không được trống");
        Objects.requireNonNull(passengerDestination, "passengerDestination không được trống");
        Objects.requireNonNull(proposedDropoff, "proposedDropoff không được trống");
    }
}
