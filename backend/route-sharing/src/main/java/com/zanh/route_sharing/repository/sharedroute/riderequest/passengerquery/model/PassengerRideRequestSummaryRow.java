package com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PassengerRideRequestSummaryRow(
        Long rideRequestId,
        TrangThaiYeuCau status,
        Instant sentAt,
        Long routeId,
        TrangThaiLoTrinh routeStatus,
        String routeOriginAddress,
        String routeDestinationAddress,
        Instant expectedDepartureTime,
        Long driverId,
        String driverFullName,
        String driverAvatarUrl,
        Long vehicleId,
        String licensePlate,
        String actualColor,
        String brandName,
        String modelName,
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,
        String pickupAddress,
        String passengerDestinationAddress,
        String proposedDropoffAddress,
        BigDecimal proposedSupportAmount,
        BigDecimal agreedSupportAmount,
        Instant acceptedAt,
        Instant rejectedAt,
        Instant cooldownUntil,
        Instant cancelledAt,
        String cancellationReason,
        boolean assignedToTrip) {

    public PassengerRideRequestSummaryRow {
        Objects.requireNonNull(rideRequestId, "rideRequestId không được trống.");
        Objects.requireNonNull(status, "status không được trống.");
        Objects.requireNonNull(sentAt, "sentAt không được trống.");
        Objects.requireNonNull(routeId, "routeId không được trống.");
        Objects.requireNonNull(routeStatus, "routeStatus không được trống.");
        Objects.requireNonNull(routeOriginAddress, "routeOriginAddress không được trống.");
        Objects.requireNonNull(routeDestinationAddress, "routeDestinationAddress không được trống.");
        Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống.");
        Objects.requireNonNull(driverId, "driverId không được trống.");
        Objects.requireNonNull(driverFullName, "driverFullName không được trống.");
        Objects.requireNonNull(vehicleId, "vehicleId không được trống.");
        Objects.requireNonNull(licensePlate, "licensePlate không được trống.");
        Objects.requireNonNull(actualColor, "actualColor không được trống.");
        Objects.requireNonNull(brandName, "brandName không được trống.");
        Objects.requireNonNull(modelName, "modelName không được trống.");
        Objects.requireNonNull(matchType, "matchType không được trống.");
        Objects.requireNonNull(dropoffType, "dropoffType không được trống.");
        Objects.requireNonNull(pickupAddress, "pickupAddress không được trống.");
        Objects.requireNonNull(passengerDestinationAddress,
                "passengerDestinationAddress không được trống.");
        Objects.requireNonNull(proposedDropoffAddress,
                "proposedDropoffAddress không được trống.");
        Objects.requireNonNull(proposedSupportAmount,
                "proposedSupportAmount không được trống.");
    }
}
