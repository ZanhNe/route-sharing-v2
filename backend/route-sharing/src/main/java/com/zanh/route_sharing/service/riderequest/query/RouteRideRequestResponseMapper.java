package com.zanh.route_sharing.service.riderequest.query;

import com.zanh.route_sharing.dto.response.PageMeta;
import com.zanh.route_sharing.dto.riderequest.RideRequestPointResponse;
import com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestDetailResponse;
import com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestPageResponse;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.OwnedRouteSnapshot;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestDetailLookup;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestDetailRow;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestPageSnapshot;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestSummaryRow;
import com.zanh.route_sharing.service.riderequest.query.model.RouteRideRequestPageResult;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestDetailResponse.StoredRouteMeaning.DRIVER_ORIGINAL_ROUTE;
import static com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestDetailResponse.StoredRouteMeaning.PASSENGER_DESIRED_ROUTE_VIA_DROPOFF;
import static com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestDetailResponse.StoredRouteMeaning.PASSENGER_SERVED_SEGMENT;

@Component
public class RouteRideRequestResponseMapper {

    private final RouteGeoJsonWriter geoJsonWriter;

    public RouteRideRequestResponseMapper(RouteGeoJsonWriter geoJsonWriter) {
        this.geoJsonWriter = geoJsonWriter;
    }

    public RouteRideRequestPageResult toPage(
            PendingRideRequestPageSnapshot snapshot,
            Instant readAt) {
        RouteRideRequestPageResponse.RouteContext route = new RouteRideRequestPageResponse.RouteContext(
                snapshot.route().routeId(),
                snapshot.route().routeStatus(),
                snapshot.route().expectedDepartureTime(),
                snapshot.route().offeredSeats(),
                snapshot.route().remainingSeats());

        List<RouteRideRequestPageResponse.PendingRequestSummary> items = snapshot.rows().stream()
                .map(row -> toSummary(row, readAt))
                .toList();

        return new RouteRideRequestPageResult(
                new RouteRideRequestPageResponse(route, items),
                PageMeta.of(snapshot.page(), snapshot.size(), snapshot.totalElements()));
    }

    public RouteRideRequestDetailResponse toDetail(
            PendingRideRequestDetailLookup lookup,
            Instant readAt) {
        OwnedRouteSnapshot route = lookup.route();
        PendingRideRequestDetailRow request = lookup.request();

        RideRequestPointResponse origin = point(
                route.originLatitude(),
                route.originLongitude(),
                route.originAddress());
        RideRequestPointResponse driverDestination = point(
                route.driverDestinationLatitude(),
                route.driverDestinationLongitude(),
                route.driverDestinationAddress());
        RideRequestPointResponse pickup = point(
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.pickupAddress());
        RideRequestPointResponse passengerDestination = point(
                request.passengerDestinationLatitude(),
                request.passengerDestinationLongitude(),
                request.passengerDestinationAddress());
        RideRequestPointResponse proposedDropoff = point(
                request.proposedDropoffLatitude(),
                request.proposedDropoffLongitude(),
                request.proposedDropoffAddress());

        RouteRideRequestDetailResponse.StoredRoute originalRoute = new RouteRideRequestDetailResponse.StoredRoute(
                DRIVER_ORIGINAL_ROUTE,
                geoJsonWriter.readStoredLineString(route.originalRouteGeoJson()),
                route.originalDistanceMeters(),
                route.originalDurationSeconds());
        RouteRideRequestDetailResponse.StoredRoute passengerRoute = new RouteRideRequestDetailResponse.StoredRoute(
                PASSENGER_DESIRED_ROUTE_VIA_DROPOFF,
                geoJsonWriter.readStoredLineString(request.passengerDesiredRouteGeoJson()),
                request.passengerDesiredDistanceMeters(),
                null);
        RouteRideRequestDetailResponse.StoredRoute servedSegment = new RouteRideRequestDetailResponse.StoredRoute(
                PASSENGER_SERVED_SEGMENT,
                geoJsonWriter.readStoredLineString(request.servedSegmentGeoJson()),
                request.servedDistanceMeters(),
                null);

        RouteRideRequestDetailResponse.RouteContext routeContext = new RouteRideRequestDetailResponse.RouteContext(
                route.routeId(),
                route.routeStatus(),
                route.expectedDepartureTime(),
                route.offeredSeats(),
                route.remainingSeats(),
                origin,
                driverDestination,
                originalRoute);
        RouteRideRequestDetailResponse.Passenger passenger = new RouteRideRequestDetailResponse.Passenger(
                request.passengerId(),
                request.passengerFullName(),
                request.passengerAvatarUrl(),
                request.passengerGender(),
                request.passengerDateOfBirth());
        RouteRideRequestDetailResponse.PendingRequest pendingRequest = new RouteRideRequestDetailResponse.PendingRequest(
                request.rideRequestId(),
                request.status(),
                request.sentAt(),
                request.note(),
                pickup,
                passengerDestination,
                proposedDropoff,
                request.matchType(),
                request.dropoffType(),
                request.pickupDeviationMeters(),
                request.pickupDeviationSeconds(),
                request.passengerDesiredDistanceMeters(),
                request.servedDistanceMeters(),
                request.remainingDistanceMeters(),
                request.convenienceRatioPercent(),
                request.suggestedSupportPerKmAtRequest(),
                request.proposedSupportAmount(),
                request.agreedSupportAmount(),
                request.departureTimeAtRequest());
        RouteRideRequestDetailResponse.StoredMap map = new RouteRideRequestDetailResponse.StoredMap(
                originalRoute,
                passengerRoute,
                servedSegment,
                List.of(
                        marker(RouteWaypointRole.DRIVER_ORIGIN, "Điểm xuất phát tài xế", origin),
                        marker(RouteWaypointRole.PASSENGER_PICKUP, "Điểm đón hành khách", pickup),
                        marker(RouteWaypointRole.PROPOSED_DROPOFF, "Điểm thả đề xuất", proposedDropoff),
                        marker(RouteWaypointRole.PASSENGER_DESTINATION,
                                "Điểm đến cuối cùng của hành khách",
                                passengerDestination),
                        marker(RouteWaypointRole.DRIVER_DESTINATION,
                                "Điểm kết thúc của tài xế",
                                driverDestination)));

        return new RouteRideRequestDetailResponse(
                routeContext,
                passenger,
                pendingRequest,
                map,
                readAt);
    }

    private static RouteRideRequestPageResponse.PendingRequestSummary toSummary(
            PendingRideRequestSummaryRow row,
            Instant readAt) {
        return new RouteRideRequestPageResponse.PendingRequestSummary(
                row.rideRequestId(),
                row.status(),
                row.sentAt(),
                new RouteRideRequestPageResponse.PassengerSummary(
                        row.passengerId(),
                        row.passengerFullName(),
                        row.passengerAvatarUrl()),
                row.pickupAddress(),
                row.passengerDestinationAddress(),
                row.matchType(),
                row.dropoffType(),
                row.proposedSupportAmount());
    }

    private static RouteRideRequestDetailResponse.Marker marker(
            RouteWaypointRole role,
            String label,
            RideRequestPointResponse point) {
        return new RouteRideRequestDetailResponse.Marker(role, label, point);
    }

    private static RideRequestPointResponse point(
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            String address) {
        return new RideRequestPointResponse(latitude, longitude, address);
    }

}
