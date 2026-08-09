package com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PassengerRideRequestDetailRow(
        Long rideRequestId,
        TrangThaiYeuCau status,
        Instant sentAt,
        Instant acceptedAt,
        Instant rejectedAt,
        Instant cooldownUntil,
        Instant cancelledAt,
        String cancellationReason,
        boolean assignedToTrip,
        RouteRow route,
        DriverRow driver,
        VehicleRow vehicle,
        BookingRow booking) {

    public PassengerRideRequestDetailRow {
        Objects.requireNonNull(rideRequestId, "rideRequestId không được trống.");
        Objects.requireNonNull(status, "status không được trống.");
        Objects.requireNonNull(sentAt, "sentAt không được trống.");
        Objects.requireNonNull(route, "route không được trống.");
        Objects.requireNonNull(driver, "driver không được trống.");
        Objects.requireNonNull(vehicle, "vehicle không được trống.");
        Objects.requireNonNull(booking, "booking không được trống.");
    }

    public record RouteRow(
            Long routeId,
            TrangThaiLoTrinh status,
            Instant expectedDepartureTime,
            Integer offeredSeats,
            Integer remainingSeats,
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            String originAddress,
            BigDecimal destinationLatitude,
            BigDecimal destinationLongitude,
            String destinationAddress,
            String originalRouteGeoJson,
            BigDecimal originalDistanceMeters,
            Long originalDurationSeconds) {

        public RouteRow {
            Objects.requireNonNull(routeId, "routeId không được trống.");
            Objects.requireNonNull(status, "route status không được trống.");
            Objects.requireNonNull(expectedDepartureTime,
                    "expectedDepartureTime không được trống.");
            Objects.requireNonNull(offeredSeats, "offeredSeats không được trống.");
            Objects.requireNonNull(remainingSeats, "remainingSeats không được trống.");
            Objects.requireNonNull(originLatitude, "originLatitude không được trống.");
            Objects.requireNonNull(originLongitude, "originLongitude không được trống.");
            Objects.requireNonNull(originAddress, "originAddress không được trống.");
            Objects.requireNonNull(destinationLatitude,
                    "destinationLatitude không được trống.");
            Objects.requireNonNull(destinationLongitude,
                    "destinationLongitude không được trống.");
            Objects.requireNonNull(destinationAddress,
                    "destinationAddress không được trống.");
            Objects.requireNonNull(originalRouteGeoJson,
                    "originalRouteGeoJson không được trống.");
            Objects.requireNonNull(originalDistanceMeters,
                    "originalDistanceMeters không được trống.");
            Objects.requireNonNull(originalDurationSeconds,
                    "originalDurationSeconds không được trống.");
        }
    }

    public record DriverRow(
            Long driverId,
            String fullName,
            String avatarUrl) {

        public DriverRow {
            Objects.requireNonNull(driverId, "driverId không được trống.");
            Objects.requireNonNull(fullName, "driver fullName không được trống.");
        }
    }

    public record VehicleRow(
            Long vehicleId,
            String licensePlate,
            String actualColor,
            String brandName,
            String modelName) {

        public VehicleRow {
            Objects.requireNonNull(vehicleId, "vehicleId không được trống.");
            Objects.requireNonNull(licensePlate, "licensePlate không được trống.");
            Objects.requireNonNull(actualColor, "actualColor không được trống.");
            Objects.requireNonNull(brandName, "brandName không được trống.");
            Objects.requireNonNull(modelName, "modelName không được trống.");
        }
    }

    public record BookingRow(
            LoaiGhepTuyen matchType,
            LoaiDiemTha dropoffType,
            BigDecimal pickupLatitude,
            BigDecimal pickupLongitude,
            String pickupAddress,
            BigDecimal passengerDestinationLatitude,
            BigDecimal passengerDestinationLongitude,
            String passengerDestinationAddress,
            BigDecimal proposedDropoffLatitude,
            BigDecimal proposedDropoffLongitude,
            String proposedDropoffAddress,
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
            Instant departureTimeAtRequest,
            String note) {

        public BookingRow {
            Objects.requireNonNull(matchType, "matchType không được trống.");
            Objects.requireNonNull(dropoffType, "dropoffType không được trống.");
            Objects.requireNonNull(pickupLatitude, "pickupLatitude không được trống.");
            Objects.requireNonNull(pickupLongitude, "pickupLongitude không được trống.");
            Objects.requireNonNull(pickupAddress, "pickupAddress không được trống.");
            Objects.requireNonNull(passengerDestinationLatitude,
                    "passengerDestinationLatitude không được trống.");
            Objects.requireNonNull(passengerDestinationLongitude,
                    "passengerDestinationLongitude không được trống.");
            Objects.requireNonNull(passengerDestinationAddress,
                    "passengerDestinationAddress không được trống.");
            Objects.requireNonNull(proposedDropoffLatitude,
                    "proposedDropoffLatitude không được trống.");
            Objects.requireNonNull(proposedDropoffLongitude,
                    "proposedDropoffLongitude không được trống.");
            Objects.requireNonNull(proposedDropoffAddress,
                    "proposedDropoffAddress không được trống.");
            Objects.requireNonNull(passengerDesiredRouteGeoJson,
                    "passengerDesiredRouteGeoJson không được trống.");
            Objects.requireNonNull(servedSegmentGeoJson,
                    "servedSegmentGeoJson không được trống.");
            Objects.requireNonNull(pickupDeviationMeters,
                    "pickupDeviationMeters không được trống.");
            Objects.requireNonNull(pickupDeviationSeconds,
                    "pickupDeviationSeconds không được trống.");
            Objects.requireNonNull(passengerDesiredDistanceMeters,
                    "passengerDesiredDistanceMeters không được trống.");
            Objects.requireNonNull(servedDistanceMeters,
                    "servedDistanceMeters không được trống.");
            Objects.requireNonNull(remainingDistanceMeters,
                    "remainingDistanceMeters không được trống.");
            Objects.requireNonNull(convenienceRatioPercent,
                    "convenienceRatioPercent không được trống.");
            Objects.requireNonNull(proposedSupportAmount,
                    "proposedSupportAmount không được trống.");
            Objects.requireNonNull(departureTimeAtRequest,
                    "departureTimeAtRequest không được trống.");
        }
    }
}
