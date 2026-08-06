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
        Objects.requireNonNull(route, "route không được trống.");
        Objects.requireNonNull(passenger, "passenger không được trống.");
        Objects.requireNonNull(request, "request không được trống.");
        Objects.requireNonNull(map, "map không được trống.");
        Objects.requireNonNull(readAt, "readAt không được trống.");
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
            Objects.requireNonNull(routeId, "routeId phải là số dương.");
            Objects.requireNonNull(routeStatus, "routeStatus không được trống.");
            Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống.");
            Objects.requireNonNull(offeredSeats, "offeredSeats không được trống.");
            Objects.requireNonNull(remainingSeats, "remainingSeats không được trống.");
            Objects.requireNonNull(origin, "origin không được trống.");
            Objects.requireNonNull(driverDestination, "driverDestination không được trống.");
            Objects.requireNonNull(originalRoute, "originalRoute không được trống.");
        }
    }

    public record Passenger(
            Long passengerId,
            String fullName,
            String avatarUrl,
            GioiTinh gender,
            LocalDate dateOfBirth) {

        public Passenger {
            Objects.requireNonNull(passengerId, "passengerId không được trống.");
            Objects.requireNonNull(fullName, "fullName không được trống.");
        }
    }

    public record PendingRequest(
            Long rideRequestId,
            TrangThaiYeuCau status,
            Instant sentAt,
            Instant expiresAt,
            boolean expired,
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
            Objects.requireNonNull(rideRequestId, "rideRequestId không được trống.");
            if (status != TrangThaiYeuCau.PENDING) {
                throw new IllegalArgumentException("Pending detail phải có trạng thái PENDING");
            }
            Objects.requireNonNull(sentAt, "sentAt không được trống.");
            Objects.requireNonNull(expiresAt, "expiresAt không được trống.");
            Objects.requireNonNull(pickup, "pickup không được trống.");
            Objects.requireNonNull(passengerDestination, "passengerDestination không được trống.");
            Objects.requireNonNull(proposedDropoff, "proposedDropoff không được trống.");
            Objects.requireNonNull(matchType, "matchType không được trống.");
            Objects.requireNonNull(dropoffType, "dropoffType không được trống.");
            Objects.requireNonNull(pickupDeviationMeters, "pickupDeviationMeters không được trống.");
            Objects.requireNonNull(pickupDeviationSeconds, "pickupDeviationSeconds không được trống.");
            Objects.requireNonNull(passengerDesiredDistanceMeters,
                    "passengerDesiredDistanceMeters không được trống.");
            Objects.requireNonNull(servedDistanceMeters, "servedDistanceMeters không được trống.");
            Objects.requireNonNull(remainingDistanceMeters, "remainingDistanceMeters không được trống.");
            Objects.requireNonNull(convenienceRatioPercent, "convenienceRatioPercent không được trống.");
            Objects.requireNonNull(proposedSupportAmount, "proposedSupportAmount không được trống.");
            Objects.requireNonNull(departureTimeAtRequest, "departureTimeAtRequest không được trống.");
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
