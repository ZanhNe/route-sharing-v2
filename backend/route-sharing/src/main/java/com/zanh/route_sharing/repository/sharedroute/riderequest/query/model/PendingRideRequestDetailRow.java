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
        Objects.requireNonNull(rideRequestId, "rideRequestId must not be null");
        if (status != TrangThaiYeuCau.PENDING) {
            throw new IllegalArgumentException("Pending detail must be PENDING");
        }
        Objects.requireNonNull(sentAt, "sentAt must not be null");
        Objects.requireNonNull(passengerId, "passengerId must not be null");
        Objects.requireNonNull(passengerFullName, "passengerFullName must not be null");
        Objects.requireNonNull(pickupLatitude, "pickupLatitude must not be null");
        Objects.requireNonNull(pickupLongitude, "pickupLongitude must not be null");
        Objects.requireNonNull(pickupAddress, "pickupAddress must not be null");
        Objects.requireNonNull(passengerDestinationLatitude, "passengerDestinationLatitude must not be null");
        Objects.requireNonNull(passengerDestinationLongitude, "passengerDestinationLongitude must not be null");
        Objects.requireNonNull(passengerDestinationAddress, "passengerDestinationAddress must not be null");
        Objects.requireNonNull(proposedDropoffLatitude, "proposedDropoffLatitude must not be null");
        Objects.requireNonNull(proposedDropoffLongitude, "proposedDropoffLongitude must not be null");
        Objects.requireNonNull(proposedDropoffAddress, "proposedDropoffAddress must not be null");
        Objects.requireNonNull(matchType, "matchType must not be null");
        Objects.requireNonNull(dropoffType, "dropoffType must not be null");
        Objects.requireNonNull(passengerDesiredRouteGeoJson, "passengerDesiredRouteGeoJson must not be null");
        Objects.requireNonNull(servedSegmentGeoJson, "servedSegmentGeoJson must not be null");
        Objects.requireNonNull(pickupDeviationMeters, "pickupDeviationMeters must not be null");
        Objects.requireNonNull(pickupDeviationSeconds, "pickupDeviationSeconds must not be null");
        Objects.requireNonNull(passengerDesiredDistanceMeters, "passengerDesiredDistanceMeters must not be null");
        Objects.requireNonNull(servedDistanceMeters, "servedDistanceMeters must not be null");
        Objects.requireNonNull(remainingDistanceMeters, "remainingDistanceMeters must not be null");
        Objects.requireNonNull(convenienceRatioPercent, "convenienceRatioPercent must not be null");
        Objects.requireNonNull(proposedSupportAmount, "proposedSupportAmount must not be null");
        Objects.requireNonNull(departureTimeAtRequest, "departureTimeAtRequest must not be null");
    }
}
