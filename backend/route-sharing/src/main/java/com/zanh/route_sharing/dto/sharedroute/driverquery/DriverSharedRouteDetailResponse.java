package com.zanh.route_sharing.dto.sharedroute.driverquery;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.GeoJsonLineStringResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record DriverSharedRouteDetailResponse(
        Long routeId,
        TrangThaiLoTrinh status,
        Instant createdAt,
        Instant expectedDepartureTime,
        Integer offeredSeats,
        Integer remainingSeats,
        BigDecimal suggestedSupportPerKm,
        RouteEndpointResponse origin,
        RouteEndpointResponse driverDestination,
        OriginalRoute originalRoute,
        DriverSharedRoutePageResponse.VehicleSummary vehicle,
        DriverSharedRoutePageResponse.BookingSummary bookings,
        boolean assignedToTrip,
        Long tripId,
        boolean canCancelRoute,
        Instant cancelledAt,
        String cancellationReason) {

    public DriverSharedRouteDetailResponse {
        Objects.requireNonNull(routeId, "routeId không được trống.");
        Objects.requireNonNull(status, "status không được trống.");
        Objects.requireNonNull(createdAt, "createdAt không được trống.");
        Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống.");
        Objects.requireNonNull(offeredSeats, "offeredSeats không được trống.");
        Objects.requireNonNull(remainingSeats, "remainingSeats không được trống.");
        Objects.requireNonNull(origin, "origin không được trống.");
        Objects.requireNonNull(driverDestination, "driverDestination không được trống.");
        Objects.requireNonNull(originalRoute, "originalRoute không được trống.");
        Objects.requireNonNull(vehicle, "vehicle không được trống.");
        Objects.requireNonNull(bookings, "bookings không được trống.");
    }


    public DriverSharedRouteDetailResponse(
            Long routeId, TrangThaiLoTrinh status, Instant createdAt, Instant expectedDepartureTime,
            Integer offeredSeats, Integer remainingSeats, BigDecimal suggestedSupportPerKm,
            RouteEndpointResponse origin, RouteEndpointResponse driverDestination, OriginalRoute originalRoute,
            DriverSharedRoutePageResponse.VehicleSummary vehicle, DriverSharedRoutePageResponse.BookingSummary bookings,
            boolean assignedToTrip, boolean canCancelRoute, Instant cancelledAt, String cancellationReason) {
        this(routeId, status, createdAt, expectedDepartureTime, offeredSeats, remainingSeats, suggestedSupportPerKm,
                origin, driverDestination, originalRoute, vehicle, bookings, assignedToTrip, null, canCancelRoute,
                cancelledAt, cancellationReason);
    }

    public record OriginalRoute(
            GeoJsonLineStringResponse geometry,
            BigDecimal distanceMeters,
            Long durationSeconds) {
        public OriginalRoute {
            Objects.requireNonNull(geometry, "geometry không được trống.");
            Objects.requireNonNull(distanceMeters, "distanceMeters không được trống.");
            Objects.requireNonNull(durationSeconds, "durationSeconds không được trống.");
        }
    }
}
