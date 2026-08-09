package com.zanh.route_sharing.dto.riderequest.passengerquery;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PassengerRideRequestPageResponse(
        List<RideRequestSummary> items) {

    public PassengerRideRequestPageResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record RideRequestSummary(
            Long rideRequestId,
            TrangThaiYeuCau status,
            Instant sentAt,
            RouteSummary route,
            DriverSummary driver,
            VehicleSummary vehicle,
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
            boolean cooldownActive,
            Instant cancelledAt,
            String cancellationReason,
            boolean canCancel) {

        public RideRequestSummary {
            Objects.requireNonNull(rideRequestId, "rideRequestId không được trống.");
            Objects.requireNonNull(status, "status không được trống.");
            Objects.requireNonNull(sentAt, "sentAt không được trống.");
            Objects.requireNonNull(route, "route không được trống.");
            Objects.requireNonNull(driver, "driver không được trống.");
            Objects.requireNonNull(vehicle, "vehicle không được trống.");
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

    public record RouteSummary(
            Long routeId,
            TrangThaiLoTrinh status,
            String originAddress,
            String destinationAddress,
            Instant expectedDepartureTime) {

        public RouteSummary {
            Objects.requireNonNull(routeId, "routeId không được trống.");
            Objects.requireNonNull(status, "status không được trống.");
            Objects.requireNonNull(originAddress, "originAddress không được trống.");
            Objects.requireNonNull(destinationAddress, "destinationAddress không được trống.");
            Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống.");
        }
    }

    public record DriverSummary(
            Long driverId,
            String fullName,
            String avatarUrl) {

        public DriverSummary {
            Objects.requireNonNull(driverId, "driverId không được trống.");
            Objects.requireNonNull(fullName, "fullName không được trống.");
        }
    }

    public record VehicleSummary(
            Long vehicleId,
            String licensePlate,
            String actualColor,
            String brandName,
            String modelName) {

        public VehicleSummary {
            Objects.requireNonNull(vehicleId, "vehicleId không được trống.");
            Objects.requireNonNull(licensePlate, "licensePlate không được trống.");
            Objects.requireNonNull(actualColor, "actualColor không được trống.");
            Objects.requireNonNull(brandName, "brandName không được trống.");
            Objects.requireNonNull(modelName, "modelName không được trống.");
        }
    }
}
