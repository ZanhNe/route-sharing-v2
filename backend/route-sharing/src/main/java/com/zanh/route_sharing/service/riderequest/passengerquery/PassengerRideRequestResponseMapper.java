package com.zanh.route_sharing.service.riderequest.passengerquery;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.response.PageMeta;
import com.zanh.route_sharing.dto.riderequest.RideRequestPointResponse;
import com.zanh.route_sharing.dto.riderequest.passengerquery.PassengerRideRequestDetailResponse;
import com.zanh.route_sharing.dto.riderequest.passengerquery.PassengerRideRequestPageResponse;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestDetailRow;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestPageSnapshot;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestSummaryRow;
import com.zanh.route_sharing.service.riderequest.passengerquery.model.PassengerRideRequestPageResult;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static com.zanh.route_sharing.dto.riderequest.passengerquery.PassengerRideRequestDetailResponse.StoredRouteMeaning.DRIVER_ORIGINAL_ROUTE;
import static com.zanh.route_sharing.dto.riderequest.passengerquery.PassengerRideRequestDetailResponse.StoredRouteMeaning.PASSENGER_DESIRED_ROUTE_VIA_DROPOFF;
import static com.zanh.route_sharing.dto.riderequest.passengerquery.PassengerRideRequestDetailResponse.StoredRouteMeaning.PASSENGER_SERVED_SEGMENT;

@Component
public class PassengerRideRequestResponseMapper {

        private final RouteGeoJsonWriter geoJsonWriter;

        public PassengerRideRequestResponseMapper(RouteGeoJsonWriter geoJsonWriter) {
                this.geoJsonWriter = geoJsonWriter;
        }

        public PassengerRideRequestPageResult toPage(
                        PassengerRideRequestPageSnapshot snapshot,
                        Instant readAt) {
                List<PassengerRideRequestPageResponse.RideRequestSummary> items = snapshot.rows().stream()
                                .map(row -> toSummary(row, readAt))
                                .toList();
                return new PassengerRideRequestPageResult(
                                new PassengerRideRequestPageResponse(items),
                                PageMeta.of(snapshot.page(), snapshot.size(), snapshot.totalElements()));
        }

        public PassengerRideRequestDetailResponse toDetail(
                        PassengerRideRequestDetailRow row,
                        Instant readAt) {
                PassengerRideRequestDetailRow.RouteRow route = row.route();
                PassengerRideRequestDetailRow.BookingRow booking = row.booking();

                RideRequestPointResponse origin = point(
                                route.originLatitude(),
                                route.originLongitude(),
                                route.originAddress());
                RideRequestPointResponse driverDestination = point(
                                route.destinationLatitude(),
                                route.destinationLongitude(),
                                route.destinationAddress());
                RideRequestPointResponse pickup = point(
                                booking.pickupLatitude(),
                                booking.pickupLongitude(),
                                booking.pickupAddress());
                RideRequestPointResponse passengerDestination = point(
                                booking.passengerDestinationLatitude(),
                                booking.passengerDestinationLongitude(),
                                booking.passengerDestinationAddress());
                RideRequestPointResponse proposedDropoff = point(
                                booking.proposedDropoffLatitude(),
                                booking.proposedDropoffLongitude(),
                                booking.proposedDropoffAddress());

                PassengerRideRequestDetailResponse.StoredRoute originalRoute = new PassengerRideRequestDetailResponse.StoredRoute(
                                DRIVER_ORIGINAL_ROUTE,
                                geoJsonWriter.readStoredLineString(route.originalRouteGeoJson()),
                                route.originalDistanceMeters(),
                                route.originalDurationSeconds());
                PassengerRideRequestDetailResponse.StoredRoute passengerRoute = new PassengerRideRequestDetailResponse.StoredRoute(
                                PASSENGER_DESIRED_ROUTE_VIA_DROPOFF,
                                geoJsonWriter.readStoredLineString(booking.passengerDesiredRouteGeoJson()),
                                booking.passengerDesiredDistanceMeters(),
                                null);
                PassengerRideRequestDetailResponse.StoredRoute servedSegment = new PassengerRideRequestDetailResponse.StoredRoute(
                                PASSENGER_SERVED_SEGMENT,
                                geoJsonWriter.readStoredLineString(booking.servedSegmentGeoJson()),
                                booking.servedDistanceMeters(),
                                null);

                PassengerRideRequestDetailResponse.RouteContext routeContext = new PassengerRideRequestDetailResponse.RouteContext(
                                route.routeId(),
                                route.status(),
                                route.expectedDepartureTime(),
                                route.offeredSeats(),
                                route.remainingSeats(),
                                origin,
                                driverDestination,
                                originalRoute);

                PassengerRideRequestDetailResponse.Driver driver = new PassengerRideRequestDetailResponse.Driver(
                                row.driver().driverId(),
                                row.driver().fullName(),
                                row.driver().avatarUrl());

                PassengerRideRequestDetailResponse.Vehicle vehicle = new PassengerRideRequestDetailResponse.Vehicle(
                                row.vehicle().vehicleId(),
                                row.vehicle().licensePlate(),
                                row.vehicle().actualColor(),
                                row.vehicle().brandName(),
                                row.vehicle().modelName());

                PassengerRideRequestDetailResponse.BookingSnapshot bookingSnapshot = new PassengerRideRequestDetailResponse.BookingSnapshot(
                                booking.matchType(),
                                booking.dropoffType(),
                                pickup,
                                passengerDestination,
                                proposedDropoff,
                                booking.pickupDeviationMeters(),
                                booking.pickupDeviationSeconds(),
                                booking.passengerDesiredDistanceMeters(),
                                booking.servedDistanceMeters(),
                                booking.remainingDistanceMeters(),
                                booking.convenienceRatioPercent(),
                                booking.suggestedSupportPerKmAtRequest(),
                                booking.proposedSupportAmount(),
                                booking.agreedSupportAmount(),
                                booking.departureTimeAtRequest(),
                                booking.note());

                PassengerRideRequestDetailResponse.StoredMap map = new PassengerRideRequestDetailResponse.StoredMap(
                                originalRoute,
                                passengerRoute,
                                servedSegment,
                                List.of(
                                                marker(RouteWaypointRole.DRIVER_ORIGIN,
                                                                "Điểm xuất phát tài xế", origin),
                                                marker(RouteWaypointRole.PASSENGER_PICKUP,
                                                                "Điểm đón hành khách", pickup),
                                                marker(RouteWaypointRole.PROPOSED_DROPOFF,
                                                                "Điểm thả đề xuất", proposedDropoff),
                                                marker(RouteWaypointRole.PASSENGER_DESTINATION,
                                                                "Điểm đến cuối cùng của hành khách",
                                                                passengerDestination),
                                                marker(RouteWaypointRole.DRIVER_DESTINATION,
                                                                "Điểm kết thúc của tài xế",
                                                                driverDestination)));

                return new PassengerRideRequestDetailResponse(
                                row.rideRequestId(),
                                row.status(),
                                row.sentAt(),
                                row.acceptedAt(),
                                row.rejectedAt(),
                                row.cooldownUntil(),
                                isCooldownActive(row.status(), row.cooldownUntil(), readAt),
                                row.cancelledAt(),
                                row.cancellationReason(),
                                row.assignedToTrip(),
                                row.tripId(),
                                canCancel(row.status(), row.assignedToTrip()),
                                routeContext,
                                driver,
                                vehicle,
                                bookingSnapshot,
                                map);
        }

        private static PassengerRideRequestPageResponse.RideRequestSummary toSummary(
                        PassengerRideRequestSummaryRow row,
                        Instant readAt) {
                return new PassengerRideRequestPageResponse.RideRequestSummary(
                                row.rideRequestId(),
                                row.status(),
                                row.sentAt(),
                                new PassengerRideRequestPageResponse.RouteSummary(
                                                row.routeId(),
                                                row.routeStatus(),
                                                row.routeOriginAddress(),
                                                row.routeDestinationAddress(),
                                                row.expectedDepartureTime()),
                                new PassengerRideRequestPageResponse.DriverSummary(
                                                row.driverId(),
                                                row.driverFullName(),
                                                row.driverAvatarUrl()),
                                new PassengerRideRequestPageResponse.VehicleSummary(
                                                row.vehicleId(),
                                                row.licensePlate(),
                                                row.actualColor(),
                                                row.brandName(),
                                                row.modelName()),
                                row.matchType(),
                                row.dropoffType(),
                                row.pickupAddress(),
                                row.passengerDestinationAddress(),
                                row.proposedDropoffAddress(),
                                row.proposedSupportAmount(),
                                row.agreedSupportAmount(),
                                row.acceptedAt(),
                                row.rejectedAt(),
                                row.cooldownUntil(),
                                isCooldownActive(row.status(), row.cooldownUntil(), readAt),
                                row.cancelledAt(),
                                row.cancellationReason(),
                                canCancel(row.status(), row.assignedToTrip()));
        }

        static boolean canCancel(TrangThaiYeuCau status, boolean assignedToTrip) {
                return !assignedToTrip
                                && (status == TrangThaiYeuCau.PENDING || status == TrangThaiYeuCau.ACCEPTED);
        }

        static boolean isCooldownActive(
                        TrangThaiYeuCau status,
                        Instant cooldownUntil,
                        Instant readAt) {
                return status == TrangThaiYeuCau.REJECTED
                                && cooldownUntil != null
                                && readAt.isBefore(cooldownUntil);
        }

        private static PassengerRideRequestDetailResponse.Marker marker(
                        RouteWaypointRole role,
                        String label,
                        RideRequestPointResponse point) {
                return new PassengerRideRequestDetailResponse.Marker(role, label, point);
        }

        private static RideRequestPointResponse point(
                        java.math.BigDecimal latitude,
                        java.math.BigDecimal longitude,
                        String address) {
                return new RideRequestPointResponse(latitude, longitude, address);
        }
}
