package com.zanh.route_sharing.dto.riderequest.passengerquery;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.riderequest.RideRequestPointResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.GeoJsonLineStringResponse;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PassengerRideRequestDetailResponse(
        Long rideRequestId,
        TrangThaiYeuCau status,
        Instant sentAt,
        Instant acceptedAt,
        Instant rejectedAt,
        Instant cooldownUntil,
        boolean cooldownActive,
        Instant cancelledAt,
        String cancellationReason,
        boolean assignedToTrip,
        boolean canCancel,
        RouteContext route,
        Driver driver,
        Vehicle vehicle,
        BookingSnapshot booking,
        StoredMap map) {

    public PassengerRideRequestDetailResponse {
        Objects.requireNonNull(rideRequestId, "rideRequestId không được trống.");
        Objects.requireNonNull(status, "status không được trống.");
        Objects.requireNonNull(sentAt, "sentAt không được trống.");
        Objects.requireNonNull(route, "route không được trống.");
        Objects.requireNonNull(driver, "driver không được trống.");
        Objects.requireNonNull(vehicle, "vehicle không được trống.");
        Objects.requireNonNull(booking, "booking không được trống.");
        Objects.requireNonNull(map, "map không được trống.");
    }

    public record RouteContext(
            Long routeId,
            TrangThaiLoTrinh status,
            Instant expectedDepartureTime,
            Integer offeredSeats,
            Integer remainingSeats,
            RideRequestPointResponse origin,
            RideRequestPointResponse driverDestination,
            StoredRoute originalRoute) {

        public RouteContext {
            Objects.requireNonNull(routeId, "routeId không được trống.");
            Objects.requireNonNull(status, "status không được trống.");
            Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống.");
            Objects.requireNonNull(offeredSeats, "offeredSeats không được trống.");
            Objects.requireNonNull(remainingSeats, "remainingSeats không được trống.");
            Objects.requireNonNull(origin, "origin không được trống.");
            Objects.requireNonNull(driverDestination, "driverDestination không được trống.");
            Objects.requireNonNull(originalRoute, "originalRoute không được trống.");
        }
    }

    public record Driver(
            Long driverId,
            String fullName,
            String avatarUrl) {

        public Driver {
            Objects.requireNonNull(driverId, "driverId không được trống.");
            Objects.requireNonNull(fullName, "fullName không được trống.");
        }
    }

    public record Vehicle(
            Long vehicleId,
            String licensePlate,
            String actualColor,
            String brandName,
            String modelName) {

        public Vehicle {
            Objects.requireNonNull(vehicleId, "vehicleId không được trống.");
            Objects.requireNonNull(licensePlate, "licensePlate không được trống.");
            Objects.requireNonNull(actualColor, "actualColor không được trống.");
            Objects.requireNonNull(brandName, "brandName không được trống.");
            Objects.requireNonNull(modelName, "modelName không được trống.");
        }
    }

    public record BookingSnapshot(
            LoaiGhepTuyen matchType,
            LoaiDiemTha dropoffType,
            RideRequestPointResponse pickup,
            RideRequestPointResponse passengerDestination,
            RideRequestPointResponse proposedDropoff,
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

        public BookingSnapshot {
            Objects.requireNonNull(matchType, "matchType không được trống.");
            Objects.requireNonNull(dropoffType, "dropoffType không được trống.");
            Objects.requireNonNull(pickup, "pickup không được trống.");
            Objects.requireNonNull(passengerDestination, "passengerDestination không được trống.");
            Objects.requireNonNull(proposedDropoff, "proposedDropoff không được trống.");
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

    public record StoredMap(
            StoredRoute originalDriverRoute,
            StoredRoute passengerDesiredRoute,
            StoredRoute servedSegment,
            List<Marker> markers) {

        public StoredMap {
            Objects.requireNonNull(originalDriverRoute, "originalDriverRoute không được trống.");
            Objects.requireNonNull(passengerDesiredRoute, "passengerDesiredRoute không được trống.");
            Objects.requireNonNull(servedSegment, "servedSegment không được trống.");
            markers = markers == null ? List.of() : List.copyOf(markers);
        }
    }

    public record StoredRoute(
            StoredRouteMeaning meaning,
            GeoJsonLineStringResponse geoJson,
            BigDecimal distanceMeters,
            Long durationSeconds) {

        public StoredRoute {
            Objects.requireNonNull(meaning, "meaning không được trống.");
            Objects.requireNonNull(geoJson, "geoJson không được trống.");
        }
    }

    public record Marker(
            RouteWaypointRole role,
            String label,
            RideRequestPointResponse point) {

        public Marker {
            Objects.requireNonNull(role, "role không được trống.");
            Objects.requireNonNull(label, "label không được trống.");
            Objects.requireNonNull(point, "point không được trống.");
        }
    }

    public enum StoredRouteMeaning {
        DRIVER_ORIGINAL_ROUTE,
        PASSENGER_DESIRED_ROUTE_VIA_DROPOFF,
        PASSENGER_SERVED_SEGMENT
    }
}
