package com.zanh.route_sharing.repository.sharedroute.riderequest.query.model;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PendingRideRequestSummaryRow(
        Long rideRequestId,
        TrangThaiYeuCau status,
        Instant sentAt,
        Long passengerId,
        String passengerFullName,
        String passengerAvatarUrl,
        String pickupAddress,
        String passengerDestinationAddress,
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,
        BigDecimal proposedSupportAmount) {

    public PendingRideRequestSummaryRow {
        Objects.requireNonNull(rideRequestId, "rideRequestId must not be null");
        if (status != TrangThaiYeuCau.PENDING) {
            throw new IllegalArgumentException("Pending row must have PENDING status");
        }
        Objects.requireNonNull(sentAt, "sentAt must not be null");
        Objects.requireNonNull(passengerId, "passengerId must not be null");
        Objects.requireNonNull(passengerFullName, "passengerFullName must not be null");
        Objects.requireNonNull(pickupAddress, "pickupAddress must not be null");
        Objects.requireNonNull(passengerDestinationAddress,
                "passengerDestinationAddress must not be null");
        Objects.requireNonNull(matchType, "matchType must not be null");
        Objects.requireNonNull(dropoffType, "dropoffType must not be null");
        Objects.requireNonNull(proposedSupportAmount, "proposedSupportAmount must not be null");
    }
}
