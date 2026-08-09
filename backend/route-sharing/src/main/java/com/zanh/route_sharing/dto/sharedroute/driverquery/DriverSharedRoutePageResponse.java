package com.zanh.route_sharing.dto.sharedroute.driverquery;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointResponse;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record DriverSharedRoutePageResponse(List<RouteSummary> items) {

    public DriverSharedRoutePageResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record RouteSummary(
            Long routeId,
            TrangThaiLoTrinh status,
            Instant createdAt,
            Instant expectedDepartureTime,
            RouteEndpointResponse origin,
            RouteEndpointResponse driverDestination,
            Integer offeredSeats,
            Integer remainingSeats,
            VehicleSummary vehicle,
            BookingSummary bookings,
            boolean assignedToTrip,
            boolean canCancelRoute) {
        public RouteSummary {
            Objects.requireNonNull(routeId, "routeId không được trống.");
            Objects.requireNonNull(status, "status không được trống.");
            Objects.requireNonNull(createdAt, "createdAt không được trống.");
            Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống.");
            Objects.requireNonNull(origin, "origin không được trống.");
            Objects.requireNonNull(driverDestination, "driverDestination không được trống.");
            Objects.requireNonNull(offeredSeats, "offeredSeats không được trống.");
            Objects.requireNonNull(remainingSeats, "remainingSeats không được trống.");
            Objects.requireNonNull(vehicle, "vehicle không được trống.");
            Objects.requireNonNull(bookings, "bookings không được trống.");
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

    public record BookingSummary(
            long totalRequests,
            long pendingRequests,
            long acceptedBookings,
            long rejectedRequests,
            long cancelledByPassenger,
            long cancelledByDriver) {
        public BookingSummary {
            if (totalRequests < 0 || pendingRequests < 0 || acceptedBookings < 0
                    || rejectedRequests < 0 || cancelledByPassenger < 0 || cancelledByDriver < 0) {
                throw new IllegalArgumentException("Booking counts không được âm.");
            }
        }
    }
}
