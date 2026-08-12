package com.zanh.route_sharing.repository.sharedroute.tripquery.model;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record TripDetailParticipantRow(
        Long rideRequestId,
        Long passengerId,
        String passengerFullName,
        String passengerAvatarUrl,
        TrangThaiYeuCau status,
        Instant acceptedAt,
        Instant boardedAt,
        Instant noShowAt,
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,
        BigDecimal agreedSupportAmount,
        String note,
        Long pickupStopId,
        Integer pickupStopOrder,
        Long dropoffStopId) {
    public TripDetailParticipantRow {
        Objects.requireNonNull(rideRequestId);
        Objects.requireNonNull(passengerId);
        Objects.requireNonNull(passengerFullName);
        Objects.requireNonNull(status);
        Objects.requireNonNull(acceptedAt);
        Objects.requireNonNull(matchType);
        Objects.requireNonNull(dropoffType);
        Objects.requireNonNull(agreedSupportAmount);
        Objects.requireNonNull(pickupStopId);
        Objects.requireNonNull(pickupStopOrder);
        Objects.requireNonNull(dropoffStopId);
    }
}
