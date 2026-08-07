package com.zanh.route_sharing.dto.riderequest.query;

import com.zanh.route_sharing.domain.enums.GioiTinh;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.riderequest.RideRequestPointResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.GeoJsonLineStringResponse;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record RouteRideRequestDetailResponse(
        RouteContext route,
        Passenger passenger,
        PendingRequest request,
        StoredMap map,
        Instant readAt) {

    public RouteRideRequestDetailResponse {
        Objects.requireNonNull(route, "route must not be null");
        Objects.requireNonNull(passenger, "passenger must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(map, "map must not be null");
        Objects.requireNonNull(readAt, "readAt must not be null");
    }

    public record RouteContext(
            Long routeId,
            TrangThaiLoTrinh routeStatus,
            Instant expectedDepartureTime,
            Integer offeredSeats,
            Integer remainingSeats,
            RideRequestPointResponse origin,
            RideRequestPointResponse driverDestination,
            StoredRoute originalRoute) {

        public RouteContext {
            Objects.requireNonNull(routeId, "routeId must not be null");
            Objects.requireNonNull(routeStatus, "routeStatus must not be null");
            Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime must not be null");
            Objects.requireNonNull(offeredSeats, "offeredSeats must not be null");
            Objects.requireNonNull(remainingSeats, "remainingSeats must not be null");
            Objects.requireNonNull(origin, "origin must not be null");
            Objects.requireNonNull(driverDestination, "driverDestination must not be null");
            Objects.requireNonNull(originalRoute, "originalRoute must not be null");
        }
    }

    public record Passenger(
            Long passengerId,
            String fullName,
            String avatarUrl,
            GioiTinh gender,
            LocalDate dateOfBirth) {

        public Passenger {
            Objects.requireNonNull(passengerId, "passengerId must not be null");
            Objects.requireNonNull(fullName, "fullName must not be null");
        }
    }

    public record PendingRequest(
            Long rideRequestId,
            TrangThaiYeuCau status,
            Instant sentAt,
            String note,
            RideRequestPointResponse pickup,
            RideRequestPointResponse passengerDestination,
            RideRequestPointResponse proposedDropoff,
            LoaiGhepTuyen matchType,
            LoaiDiemTha dropoffType,
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

        public PendingRequest {
            Objects.requireNonNull(rideRequestId, "rideRequestId must not be null");
            if (status != TrangThaiYeuCau.PENDING) {
                throw new IllegalArgumentException("Pending detail must have PENDING status");
            }
            Objects.requireNonNull(sentAt, "sentAt must not be null");
            Objects.requireNonNull(pickup, "pickup must not be null");
            Objects.requireNonNull(passengerDestination, "passengerDestination must not be null");
            Objects.requireNonNull(proposedDropoff, "proposedDropoff must not be null");
            Objects.requireNonNull(matchType, "matchType must not be null");
            Objects.requireNonNull(dropoffType, "dropoffType must not be null");
            Objects.requireNonNull(pickupDeviationMeters, "pickupDeviationMeters must not be null");
            Objects.requireNonNull(pickupDeviationSeconds, "pickupDeviationSeconds must not be null");
            Objects.requireNonNull(passengerDesiredDistanceMeters,
                    "passengerDesiredDistanceMeters must not be null");
            Objects.requireNonNull(servedDistanceMeters, "servedDistanceMeters must not be null");
            Objects.requireNonNull(remainingDistanceMeters, "remainingDistanceMeters must not be null");
            Objects.requireNonNull(convenienceRatioPercent, "convenienceRatioPercent must not be null");
            Objects.requireNonNull(proposedSupportAmount, "proposedSupportAmount must not be null");
            Objects.requireNonNull(departureTimeAtRequest, "departureTimeAtRequest must not be null");
        }
    }

    public record StoredMap(
            StoredRoute originalDriverRoute,
            StoredRoute passengerDesiredRoute,
            StoredRoute servedSegment,
            List<Marker> markers) {

        public StoredMap {
            Objects.requireNonNull(originalDriverRoute, "originalDriverRoute must not be null");
            Objects.requireNonNull(passengerDesiredRoute, "passengerDesiredRoute must not be null");
            Objects.requireNonNull(servedSegment, "servedSegment must not be null");
            markers = markers == null ? List.of() : List.copyOf(markers);
        }
    }

    public record StoredRoute(
            StoredRouteMeaning meaning,
            GeoJsonLineStringResponse geoJson,
            BigDecimal distanceMeters,
            Long durationSeconds) {

        public StoredRoute {
            Objects.requireNonNull(meaning, "meaning must not be null");
            Objects.requireNonNull(geoJson, "geoJson must not be null");
        }
    }

    public record Marker(
            RouteWaypointRole role,
            String label,
            RideRequestPointResponse point) {

        public Marker {
            Objects.requireNonNull(role, "role must not be null");
            Objects.requireNonNull(label, "label must not be null");
            Objects.requireNonNull(point, "point must not be null");
        }
    }

    public enum StoredRouteMeaning {
        DRIVER_ORIGINAL_ROUTE,
        PASSENGER_DESIRED_ROUTE_VIA_DROPOFF,
        PASSENGER_SERVED_SEGMENT
    }
}
