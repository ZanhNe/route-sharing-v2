package com.zanh.route_sharing.service.preview;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.dto.sharedroute.preview.OriginalRouteResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewDriverResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewPointResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewPointsResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewRouteLegResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewRouteResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewVehicleResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.RouteBoundsResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.SharedRoutePreviewResponse;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewMatch;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewPreparation;
import com.zanh.route_sharing.service.routing.model.RouteBounds;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter;

@Component
public class PreviewResponseMapper {

        private final RouteGeoJsonWriter geoJsonWriter;

        public PreviewResponseMapper(RouteGeoJsonWriter geoJsonWriter) {
                this.geoJsonWriter = geoJsonWriter;
        }

        public SharedRoutePreviewResponse toResponse(
                        SharedRoutePreviewPreparation preparation,
                        PreviewSharedRouteRequest request,
                        RoutePlan routePlan,
                        Instant calculatedAt) {
                PreviewMatch match = preparation.match();
                String proposedDropoffAddress = match.dropoffType() == LoaiDiemTha.DIEM_DICH_CUOI_CUNG
                                ? request.passengerDestination().address()
                                : null;

                return new SharedRoutePreviewResponse(
                                preparation.route().routeId(),
                                calculatedAt,
                                true,
                                preparation.route().status(),
                                match.matchType(),
                                match.dropoffType(),
                                new PreviewDriverResponse(
                                                preparation.driver().id(),
                                                preparation.driver().fullName(),
                                                preparation.driver().avatarUrl()),
                                new PreviewVehicleResponse(
                                                preparation.vehicle().id(),
                                                preparation.vehicle().licensePlate(),
                                                preparation.vehicle().actualColor(),
                                                preparation.vehicle().brandName(),
                                                preparation.vehicle().modelName(),
                                                preparation.vehicle().vehicleType()),
                                preparation.route().expectedDepartureTime(),
                                preparation.route().remainingSeats(),
                                preparation.route().suggestedSupportPerKm(),
                                match.pickupDeviationMeters(),
                                match.destinationDeviationMeters(),
                                match.sharedSegmentMeters(),
                                new PreviewPointsResponse(
                                                point(
                                                                preparation.route().origin().latitude(),
                                                                preparation.route().origin().longitude(),
                                                                preparation.route().origin().address()),
                                                point(
                                                                request.pickup().latitude(),
                                                                request.pickup().longitude(),
                                                                request.pickup().address()),
                                                point(
                                                                request.passengerDestination().latitude(),
                                                                request.passengerDestination().longitude(),
                                                                request.passengerDestination().address()),
                                                point(
                                                                match.proposedDropoff().latitude(),
                                                                match.proposedDropoff().longitude(),
                                                                proposedDropoffAddress),
                                                point(
                                                                preparation.route().driverDestination().latitude(),
                                                                preparation.route().driverDestination().longitude(),
                                                                preparation.route().driverDestination().address())),
                                new OriginalRouteResponse(
                                                geoJsonWriter.readStoredLineString(
                                                                preparation.route().originalRouteGeoJson()),
                                                preparation.route().originalDistanceMeters(),
                                                preparation.route().originalDurationSeconds()),
                                new PreviewRouteResponse(
                                                geoJsonWriter.writePreviewLineString(routePlan.geometry()),
                                                bounds(routePlan.bounds()),
                                                routePlan.distanceMeters(),
                                                routePlan.durationSeconds(),
                                                routePlan.legs().stream()
                                                                .map(leg -> new PreviewRouteLegResponse(
                                                                                leg.sequence(),
                                                                                leg.fromRole(),
                                                                                leg.toRole(),
                                                                                leg.distanceMeters(),
                                                                                leg.durationSeconds(),
                                                                                leg.collapsed()))
                                                                .toList(),
                                                routePlan.warnings()));
        }

        private static RouteBoundsResponse bounds(RouteBounds bounds) {
                return new RouteBoundsResponse(
                                List.of(bounds.southWestLongitude(), bounds.southWestLatitude()),
                                List.of(bounds.northEastLongitude(), bounds.northEastLatitude()));
        }

        private static PreviewPointResponse point(
                        BigDecimal latitude,
                        BigDecimal longitude,
                        String address) {
                return new PreviewPointResponse(latitude, longitude, address);
        }

}
