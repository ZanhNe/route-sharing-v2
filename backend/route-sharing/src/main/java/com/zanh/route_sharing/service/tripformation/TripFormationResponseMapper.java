package com.zanh.route_sharing.service.tripformation;

import com.zanh.route_sharing.dto.trip.formation.TripFormationPointResponse;
import com.zanh.route_sharing.dto.trip.formation.TripFormationResponse;
import com.zanh.route_sharing.dto.trip.formation.TripFormationStopResponse;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationPersistedView;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationStopView;
import com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TripFormationResponseMapper {

        private final RouteGeoJsonWriter routeGeoJsonWriter;

        public TripFormationResponseMapper(RouteGeoJsonWriter routeGeoJsonWriter) {
                this.routeGeoJsonWriter = routeGeoJsonWriter;
        }

        public TripFormationResponse toResponse(
                        TripFormationPersistedView view,
                        String formationOutcome) {
                return new TripFormationResponse(
                                formationOutcome,
                                view.routeId(),
                                view.routeStatus().name(),
                                view.lockedAt(),
                                view.remainingSeats(),
                                view.tripId(),
                                view.tripStatus().name(),
                                view.formedAt(),
                                view.plannedPassengerCount(),
                                view.actualPassengerCount(),
                                "ALREADY_FORMED".equals(formationOutcome)
                                                ? routeGeoJsonWriter.writeStoredLineString(view.operationalRoute())
                                                : routeGeoJsonWriter.writeLineString(view.operationalRoute()),
                                view.stops().stream().map(TripFormationResponseMapper::stop).toList());
        }

        private static TripFormationStopResponse stop(TripFormationStopView stop) {
                Coordinate coordinate = stop.point().getCoordinate();
                return new TripFormationStopResponse(
                                stop.stopId(),
                                stop.order(),
                                stop.type().name(),
                                stop.status().name(),
                                stop.rideRequestId(),
                                new TripFormationPointResponse(
                                                BigDecimal.valueOf(coordinate.y),
                                                BigDecimal.valueOf(coordinate.x),
                                                stop.address()),
                                stop.arrivalRadiusMeters());
        }
}
