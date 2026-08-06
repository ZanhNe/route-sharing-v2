package com.zanh.route_sharing.repository.sharedroute.riderequest.query.model;

import com.zanh.route_sharing.domain.enums.GioiTinh;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record PendingRideRequestDetailRow(
        Long rideRequestId,
        TrangThaiYeuCau status,
        Instant sentAt,
        Instant expiresAt,
        String note,
        Long passengerId,
        String passengerFullName,
        String passengerAvatarUrl,
        GioiTinh passengerGender,
        LocalDate passengerDateOfBirth,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,
        String pickupAddress,
        BigDecimal passengerDestinationLatitude,
        BigDecimal passengerDestinationLongitude,
        String passengerDestinationAddress,
        BigDecimal proposedDropoffLatitude,
        BigDecimal proposedDropoffLongitude,
        String proposedDropoffAddress,
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,
        String passengerDesiredRouteGeoJson,
        String servedSegmentGeoJson,
        BigDecimal pickupDeviationMeters,
        Long pickupDeviationSeconds,
        BigDecimal passengerDesiredDistanceMeters,
        BigDecimal servedDistanceMeters,
        BigDecimal remainingDistanceMeters,
        BigDecimal convenienceRatioPercent,
        BigDecimal suggestedSupportPerKmAtRequest,
        BigDecimal proposedSupportAmount,
        BigDecimal agreedSupportAmount,
        Instant departureTimeAtRequest) {

    public PendingRideRequestDetailRow {
        Objects.requireNonNull(rideRequestId, "rideRequestId không được trống.");
        if (status != TrangThaiYeuCau.PENDING) {
            throw new IllegalArgumentException("Pending detail must be PENDING");
        }
        Objects.requireNonNull(sentAt, "sentAt không được trống.");
        Objects.requireNonNull(expiresAt, "expiresAt không được trống.");
        Objects.requireNonNull(passengerId, "passengerId không được trống.");
        Objects.requireNonNull(passengerFullName, "passengerFullName không được trống.");
        Objects.requireNonNull(pickupLatitude, "pickupLatitude không được trống.");
        Objects.requireNonNull(pickupLongitude, "pickupLongitude không được trống.");
        Objects.requireNonNull(pickupAddress, "pickupAddress không được trống.");
        Objects.requireNonNull(passengerDestinationLatitude, "passengerDestinationLatitude không được trống.");
        Objects.requireNonNull(passengerDestinationLongitude, "passengerDestinationLongitude không được trống.");
        Objects.requireNonNull(passengerDestinationAddress, "passengerDestinationAddress không được trống.");
        Objects.requireNonNull(proposedDropoffLatitude, "proposedDropoffLatitude không được trống.");
        Objects.requireNonNull(proposedDropoffLongitude, "proposedDropoffLongitude không được trống.");
        Objects.requireNonNull(proposedDropoffAddress, "proposedDropoffAddress không được trống.");
        Objects.requireNonNull(matchType, "matchType không được trống.");
        Objects.requireNonNull(dropoffType, "dropoffType không được trống.");
        Objects.requireNonNull(passengerDesiredRouteGeoJson, "passengerDesiredRouteGeoJson không được trống.");
        Objects.requireNonNull(servedSegmentGeoJson, "servedSegmentGeoJson không được trống.");
        Objects.requireNonNull(pickupDeviationMeters, "pickupDeviationMeters không được trống.");
        Objects.requireNonNull(pickupDeviationSeconds, "pickupDeviationSeconds không được trống.");
        Objects.requireNonNull(passengerDesiredDistanceMeters, "passengerDesiredDistanceMeters không được trống.");
        Objects.requireNonNull(servedDistanceMeters, "servedDistanceMeters không được trống.");
        Objects.requireNonNull(remainingDistanceMeters, "remainingDistanceMeters không được trống.");
        Objects.requireNonNull(convenienceRatioPercent, "convenienceRatioPercent không được trống.");
        Objects.requireNonNull(proposedSupportAmount, "proposedSupportAmount không được trống.");
        Objects.requireNonNull(departureTimeAtRequest, "departureTimeAtRequest không được trống.");
    }
}
