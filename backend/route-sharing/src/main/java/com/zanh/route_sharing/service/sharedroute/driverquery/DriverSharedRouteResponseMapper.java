package com.zanh.route_sharing.service.sharedroute.driverquery;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.dto.response.PageMeta;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointResponse;
import com.zanh.route_sharing.dto.sharedroute.driverquery.DriverSharedRouteDetailResponse;
import com.zanh.route_sharing.dto.sharedroute.driverquery.DriverSharedRoutePageResponse;
import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRouteDetailRow;
import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRoutePageSnapshot;
import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRouteSummaryRow;
import com.zanh.route_sharing.service.sharedroute.driverquery.model.DriverSharedRoutePageResult;
import com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter;
import org.springframework.stereotype.Component;

@Component
public class DriverSharedRouteResponseMapper {

        private final RouteGeoJsonWriter routeGeoJsonWriter;

        public DriverSharedRouteResponseMapper(RouteGeoJsonWriter routeGeoJsonWriter) {
                this.routeGeoJsonWriter = routeGeoJsonWriter;
        }

        public DriverSharedRoutePageResult toPage(DriverSharedRoutePageSnapshot snapshot) {
                var items = snapshot.rows().stream()
                                .map(DriverSharedRouteResponseMapper::toSummary)
                                .toList();
                return new DriverSharedRoutePageResult(
                                new DriverSharedRoutePageResponse(items),
                                PageMeta.of(snapshot.page(), snapshot.size(), snapshot.totalElements()));
        }

        public DriverSharedRouteDetailResponse toDetail(DriverSharedRouteDetailRow row) {
                return new DriverSharedRouteDetailResponse(
                                row.routeId(),
                                row.status(),
                                row.createdAt(),
                                row.expectedDepartureTime(),
                                row.offeredSeats(),
                                row.remainingSeats(),
                                row.suggestedSupportPerKm(),
                                endpoint(row.originLatitude(), row.originLongitude(), row.originAddress()),
                                endpoint(row.destinationLatitude(), row.destinationLongitude(),
                                                row.destinationAddress()),
                                new DriverSharedRouteDetailResponse.OriginalRoute(
                                                routeGeoJsonWriter.readStoredLineString(row.originalRouteGeoJson()),
                                                row.originalDistanceMeters(),
                                                row.originalDurationSeconds()),
                                vehicle(row.vehicleId(), row.licensePlate(), row.actualColor(), row.brandName(),
                                                row.modelName()),
                                bookings(row.totalRequests(), row.pendingRequests(), row.acceptedBookings(),
                                                row.rejectedRequests(), row.cancelledByPassenger(),
                                                row.cancelledByDriver()),
                                row.assignedToTrip(),
                                row.tripId(),
                                canCancelRoute(row.status(), row.assignedToTrip()),
                                row.cancelledAt(),
                                row.cancellationReason());
        }

        static boolean canCancelRoute(TrangThaiLoTrinh status, boolean assignedToTrip) {
                return status == TrangThaiLoTrinh.OPEN && !assignedToTrip;
        }

        private static DriverSharedRoutePageResponse.RouteSummary toSummary(DriverSharedRouteSummaryRow row) {
                return new DriverSharedRoutePageResponse.RouteSummary(
                                row.routeId(),
                                row.status(),
                                row.createdAt(),
                                row.expectedDepartureTime(),
                                endpoint(row.originLatitude(), row.originLongitude(), row.originAddress()),
                                endpoint(row.destinationLatitude(), row.destinationLongitude(),
                                                row.destinationAddress()),
                                row.offeredSeats(),
                                row.remainingSeats(),
                                vehicle(row.vehicleId(), row.licensePlate(), row.actualColor(), row.brandName(),
                                                row.modelName()),
                                bookings(row.totalRequests(), row.pendingRequests(), row.acceptedBookings(),
                                                row.rejectedRequests(), row.cancelledByPassenger(),
                                                row.cancelledByDriver()),
                                row.assignedToTrip(),
                                canCancelRoute(row.status(), row.assignedToTrip()));
        }

        private static RouteEndpointResponse endpoint(
                        java.math.BigDecimal latitude,
                        java.math.BigDecimal longitude,
                        String address) {
                return new RouteEndpointResponse(latitude, longitude, address);
        }

        private static DriverSharedRoutePageResponse.VehicleSummary vehicle(
                        Long vehicleId,
                        String licensePlate,
                        String actualColor,
                        String brandName,
                        String modelName) {
                return new DriverSharedRoutePageResponse.VehicleSummary(
                                vehicleId, licensePlate, actualColor, brandName, modelName);
        }

        private static DriverSharedRoutePageResponse.BookingSummary bookings(
                        long totalRequests,
                        long pendingRequests,
                        long acceptedBookings,
                        long rejectedRequests,
                        long cancelledByPassenger,
                        long cancelledByDriver) {
                return new DriverSharedRoutePageResponse.BookingSummary(
                                totalRequests,
                                pendingRequests,
                                acceptedBookings,
                                rejectedRequests,
                                cancelledByPassenger,
                                cancelledByDriver);
        }
}
