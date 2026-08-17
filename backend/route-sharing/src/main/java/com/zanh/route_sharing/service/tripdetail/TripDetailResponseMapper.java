package com.zanh.route_sharing.service.tripdetail;

import com.zanh.route_sharing.dto.trip.detail.TripDetailResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.GeoJsonLineStringResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripDetailSnapshot;
import com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripViewerRole;
import com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TripDetailResponseMapper {

        private static final String TRIP_OPERATIONAL_ROUTE = "TRIP_OPERATIONAL_ROUTE";

        private final RouteGeoJsonWriter routeGeoJsonWriter;

        public TripDetailResponseMapper(RouteGeoJsonWriter routeGeoJsonWriter) {
                this.routeGeoJsonWriter = routeGeoJsonWriter;
        }

        public TripDetailResponse toResponse(TripDetailSnapshot snapshot, Instant readAt) {
                var h = snapshot.header();
                boolean completedPassengerView = h.viewerRole() == TripViewerRole.PASSENGER
                                && snapshot.participants().size() == 1
                                && snapshot.participants().get(0)
                                                .status() == com.zanh.route_sharing.domain.enums.TrangThaiYeuCau.COMPLETED;
                var participants = snapshot.participants().stream()
                                .map(p -> new TripDetailResponse.Participant(
                                                p.rideRequestId(),
                                                new TripDetailResponse.Passenger(p.passengerId(), p.passengerFullName(),
                                                                p.passengerAvatarUrl()),
                                                new TripDetailResponse.Booking(
                                                                p.status(), p.acceptedAt(), p.boardedAt(), p.noShowAt(),
                                                                p.droppedOffAt(), p.matchType(),
                                                                p.dropoffType(), p.agreedSupportAmount(), p.note()),
                                                p.pickupStopId(),
                                                p.dropoffStopId()))
                                .toList();
                var stops = snapshot.stops().stream()
                                .map(s -> new TripDetailResponse.Stop(
                                                s.stopId(), s.order(), s.type(), s.status(), s.rideRequestId(),
                                                new TripDetailResponse.Point(s.latitude(), s.longitude(), s.address()),
                                                s.arrivedAt(), s.waitingStartedAt(), s.waitingDeadline(),
                                                s.completedAt()))
                                .toList();

                return new TripDetailResponse(
                                h.viewerRole() == TripViewerRole.DRIVER
                                                ? TripDetailResponse.ViewerRole.DRIVER
                                                : TripDetailResponse.ViewerRole.PASSENGER,
                                new TripDetailResponse.Trip(
                                                h.tripId(), completedPassengerView ? null : h.tripStatus(),
                                                completedPassengerView ? null : h.monitoringStatus(),
                                                completedPassengerView ? null : h.signalReferenceAt(),
                                                h.formedAt(), h.startedAt(),
                                                completedPassengerView ? null : h.endedAt(),
                                                h.cancelledAt(), h.cancellationReason(),
                                                completedPassengerView ? null : h.safetyHoldStartedAt(),
                                                completedPassengerView ? null : h.safetyMessage(),
                                                h.plannedPassengerCount(),
                                                completedPassengerView ? null : h.actualPassengerCount()),
                                new TripDetailResponse.Route(
                                                h.routeId(), h.routeStatus(), h.lockedAt(), h.expectedDepartureTime(),
                                                h.offeredSeats(), h.remainingSeats(),
                                                point(h.originLatitude(), h.originLongitude(), h.originAddress()),
                                                point(h.destinationLatitude(), h.destinationLongitude(),
                                                                h.destinationAddress())),
                                new TripDetailResponse.Driver(h.driverId(), h.driverFullName(), h.driverAvatarUrl()),
                                new TripDetailResponse.Vehicle(
                                                h.vehicleId(), h.licensePlate(), h.actualColor(), h.brandName(),
                                                h.modelName()),
                                new TripDetailResponse.OperationalRoute(
                                                TRIP_OPERATIONAL_ROUTE,
                                                readOperationalRoute(h.operationalRouteGeoJson())),
                                participants,
                                stops,
                                completedPassengerView ? null : activeSafetyHold(snapshot),
                                completedPassengerView ? null : currentDriverLocation(snapshot),
                                readAt);
        }

        private static TripDetailResponse.ActiveSafetyHold activeSafetyHold(TripDetailSnapshot snapshot) {
                var h = snapshot.header();
                if (h.viewerRole() != TripViewerRole.DRIVER || h
                                .tripStatus() != com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi.SECURITY_FROZEN) {
                        return null;
                }
                if (h.activeSafetyHoldInterventionId() == null || h.activeSafetyHoldTargetRideRequestId() == null) {
                        throw TripDetailSnapshotValidator.invalidStoredPlan();
                }
                return new TripDetailResponse.ActiveSafetyHold(h.activeSafetyHoldInterventionId(),
                                h.activeSafetyHoldTargetRideRequestId());
        }

        private static TripDetailResponse.CurrentDriverLocation currentDriverLocation(TripDetailSnapshot snapshot) {
                var location = snapshot.currentDriverLocation();
                if (location == null) {
                        return null;
                }
                return new TripDetailResponse.CurrentDriverLocation(
                                new TripDetailResponse.Position(location.latitude(), location.longitude()),
                                location.observedAt(),
                                location.receivedAt(),
                                location.accuracyMeters(),
                                location.locationSequence());
        }

        private GeoJsonLineStringResponse readOperationalRoute(String geoJson) {
                try {
                        return routeGeoJsonWriter.readStoredLineString(geoJson);
                } catch (BusinessException exception) {
                        throw TripDetailSnapshotValidator.invalidStoredPlan();
                }
        }

        private static TripDetailResponse.Point point(
                        java.math.BigDecimal latitude,
                        java.math.BigDecimal longitude,
                        String address) {
                return new TripDetailResponse.Point(latitude, longitude, address);
        }
}
