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
        Instant expiresAt,
        Long passengerId,
        String passengerFullName,
        String passengerAvatarUrl,
        String pickupAddress,
        String passengerDestinationAddress,
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,
        BigDecimal proposedSupportAmount) {

    public PendingRideRequestSummaryRow {
        Objects.requireNonNull(rideRequestId, "rideRequestId không được trống.");
        if (status != TrangThaiYeuCau.PENDING) {
            throw new IllegalArgumentException("Pending queue phải chứa duy nhất request đang ở trạng thái PENDING.");
        }
        Objects.requireNonNull(sentAt, "sentAt không được trống.");
        Objects.requireNonNull(expiresAt, "expiresAt không được trống.");
        Objects.requireNonNull(passengerId, "passengerId không được trống.");
        Objects.requireNonNull(passengerFullName, "passengerFullName không được trống.");
        Objects.requireNonNull(pickupAddress, "pickupAddress không được trống.");
        Objects.requireNonNull(passengerDestinationAddress, "passengerDestinationAddress không được trống.");
        Objects.requireNonNull(matchType, "matchType không được trống.");
        Objects.requireNonNull(dropoffType, "dropoffType không được trống.");
        Objects.requireNonNull(proposedSupportAmount, "proposedSupportAmount không được trống.");
    }
}
