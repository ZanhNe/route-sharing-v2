package com.zanh.route_sharing.service.realtime;

import com.zanh.route_sharing.domain.enums.TrangThaiGiamSatChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.service.realtime.model.BookingAcceptedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.BookingCancelledByPassengerRealtimeData;
import com.zanh.route_sharing.service.realtime.model.BookingRejectedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.BookingRequestRealtimeData;
import com.zanh.route_sharing.service.realtime.model.DriverArrivedPickupRealtimeData;
import com.zanh.route_sharing.service.realtime.model.DriverArrivedDropoffRealtimeData;
import com.zanh.route_sharing.service.realtime.model.PassengerBoardedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.PassengerDroppedOffRealtimeData;
import com.zanh.route_sharing.service.realtime.model.PassengerNoShowRealtimeData;
import com.zanh.route_sharing.service.realtime.model.RealtimeEventEnvelope;
import com.zanh.route_sharing.service.realtime.model.RealtimeResource;
import com.zanh.route_sharing.service.realtime.model.RouteCancelledByDriverRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripFormedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripCancelledBeforeStartRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripStartedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripSignalMonitoringChangedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripSafetyIncidentReportedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripSafetyIncidentStatusChangedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripSafetyIncidentWorkChangedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripLocationUpdatedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripSafetyStateChangedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripParticipantSafetyChangedRealtimeData;

import java.math.BigDecimal;
import java.time.Instant;

public final class RealtimeNotificationEventFactory {
        public static final int EVENT_VERSION = 1;
        public static final String RIDE_REQUEST_RESOURCE = "RIDE_REQUEST";
        public static final String SHARED_ROUTE_RESOURCE = "SHARED_ROUTE";
        public static final String TRIP_RESOURCE = "TRIP";
        public static final String TRIP_INCIDENT_RESOURCE = "TRIP_INCIDENT";

        private RealtimeNotificationEventFactory() {
        }

        public static RealtimeEventEnvelope<BookingRequestRealtimeData> bookingRequest(
                        Long rideRequestId,
                        Long routeId,
                        Instant sentAt) {
                return new RealtimeEventEnvelope<>(
                                "BOOKING_REQUEST",
                                EVENT_VERSION,
                                sentAt,
                                new RealtimeResource(RIDE_REQUEST_RESOURCE, rideRequestId),
                                new BookingRequestRealtimeData(
                                                rideRequestId,
                                                routeId,
                                                TrangThaiYeuCau.PENDING.name(),
                                                sentAt));
        }

        public static RealtimeEventEnvelope<BookingAcceptedRealtimeData> bookingAccepted(
                        Long rideRequestId,
                        Long routeId,
                        Instant decisionAt,
                        BigDecimal agreedSupportAmount) {
                return new RealtimeEventEnvelope<>(
                                "BOOKING_ACCEPTED",
                                EVENT_VERSION,
                                decisionAt,
                                new RealtimeResource(RIDE_REQUEST_RESOURCE, rideRequestId),
                                new BookingAcceptedRealtimeData(
                                                rideRequestId,
                                                routeId,
                                                TrangThaiYeuCau.ACCEPTED.name(),
                                                decisionAt,
                                                agreedSupportAmount));
        }

        public static RealtimeEventEnvelope<BookingRejectedRealtimeData> bookingRejected(
                        Long rideRequestId,
                        Long routeId,
                        Instant decisionAt,
                        Instant cooldownUntil) {
                return new RealtimeEventEnvelope<>(
                                "BOOKING_REJECTED",
                                EVENT_VERSION,
                                decisionAt,
                                new RealtimeResource(RIDE_REQUEST_RESOURCE, rideRequestId),
                                new BookingRejectedRealtimeData(
                                                rideRequestId,
                                                routeId,
                                                TrangThaiYeuCau.REJECTED.name(),
                                                decisionAt,
                                                cooldownUntil));
        }

        public static RealtimeEventEnvelope<BookingCancelledByPassengerRealtimeData> bookingCancelledByPassenger(
                        Long rideRequestId,
                        Long routeId,
                        TrangThaiYeuCau previousStatus,
                        Instant cancelledAt,
                        Integer remainingSeats) {
                return new RealtimeEventEnvelope<>(
                                "BOOKING_CANCELLED_BY_PASSENGER",
                                EVENT_VERSION,
                                cancelledAt,
                                new RealtimeResource(RIDE_REQUEST_RESOURCE, rideRequestId),
                                new BookingCancelledByPassengerRealtimeData(
                                                rideRequestId,
                                                routeId,
                                                previousStatus.name(),
                                                TrangThaiYeuCau.CANCELLED_BY_PASSENGER.name(),
                                                cancelledAt,
                                                remainingSeats));
        }

        public static RealtimeEventEnvelope<RouteCancelledByDriverRealtimeData> routeCancelledByDriver(
                        Long routeId,
                        Long rideRequestId,
                        Instant cancelledAt) {
                return new RealtimeEventEnvelope<>(
                                "ROUTE_CANCELLED_BY_DRIVER",
                                EVENT_VERSION,
                                cancelledAt,
                                new RealtimeResource(SHARED_ROUTE_RESOURCE, routeId),
                                new RouteCancelledByDriverRealtimeData(
                                                routeId,
                                                TrangThaiLoTrinh.CANCELLED.name(),
                                                rideRequestId,
                                                TrangThaiYeuCau.CANCELLED_BY_DRIVER.name(),
                                                cancelledAt));
        }

        public static RealtimeEventEnvelope<TripFormedRealtimeData> tripFormed(
                        Long tripId,
                        Long routeId,
                        Long rideRequestId,
                        Instant formedAt,
                        Instant expectedDepartureTime) {
                return new RealtimeEventEnvelope<>(
                                "TRIP_FORMED",
                                EVENT_VERSION,
                                formedAt,
                                new RealtimeResource(TRIP_RESOURCE, tripId),
                                new TripFormedRealtimeData(
                                                tripId,
                                                routeId,
                                                rideRequestId,
                                                TrangThaiLoTrinh.LOCKED.name(),
                                                TrangThaiVanHanhChuyenDi.PREPARING.name(),
                                                formedAt,
                                                expectedDepartureTime));
        }

        public static RealtimeEventEnvelope<TripCancelledBeforeStartRealtimeData> tripCancelledBeforeStart(
                        Long tripId,
                        Long routeId,
                        Long rideRequestId,
                        Instant cancelledAt) {
                return new RealtimeEventEnvelope<>(
                                "TRIP_CANCELLED_BEFORE_START",
                                EVENT_VERSION,
                                cancelledAt,
                                new RealtimeResource(TRIP_RESOURCE, tripId),
                                new TripCancelledBeforeStartRealtimeData(
                                                tripId,
                                                routeId,
                                                rideRequestId,
                                                TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START.name(),
                                                TrangThaiLoTrinh.CANCELLED.name(),
                                                TrangThaiYeuCau.CANCELLED_BY_DRIVER.name(),
                                                cancelledAt));
        }

        public static RealtimeEventEnvelope<DriverArrivedPickupRealtimeData> driverArrivedPickup(
                        Long tripId,
                        Long routeId,
                        Long rideRequestId,
                        Long pickupStopId,
                        Integer pickupStopOrder,
                        Instant arrivedAt,
                        Instant waitingDeadline) {
                return new RealtimeEventEnvelope<>(
                                "DRIVER_ARRIVED_PICKUP",
                                EVENT_VERSION,
                                arrivedAt,
                                new RealtimeResource(TRIP_RESOURCE, tripId),
                                new DriverArrivedPickupRealtimeData(
                                                tripId,
                                                routeId,
                                                rideRequestId,
                                                pickupStopId,
                                                pickupStopOrder,
                                                com.zanh.route_sharing.domain.enums.TrangThaiDiemDung.ARRIVED.name(),
                                                arrivedAt,
                                                waitingDeadline));
        }

        public static RealtimeEventEnvelope<DriverArrivedDropoffRealtimeData> driverArrivedDropoff(
                        Long tripId,
                        Long routeId,
                        Long rideRequestId,
                        Long dropoffStopId,
                        Integer dropoffStopOrder,
                        Instant arrivedAt) {
                return new RealtimeEventEnvelope<>(
                                "DRIVER_ARRIVED_DROPOFF",
                                EVENT_VERSION,
                                arrivedAt,
                                new RealtimeResource(TRIP_RESOURCE, tripId),
                                new DriverArrivedDropoffRealtimeData(
                                                tripId,
                                                routeId,
                                                rideRequestId,
                                                dropoffStopId,
                                                dropoffStopOrder,
                                                com.zanh.route_sharing.domain.enums.TrangThaiDiemDung.ARRIVED.name(),
                                                arrivedAt));
        }

        public static RealtimeEventEnvelope<PassengerBoardedRealtimeData> passengerBoarded(
                        Long tripId,
                        Long routeId,
                        Long rideRequestId,
                        Long pickupStopId,
                        Integer pickupStopOrder,
                        Instant boardedAt) {
                return new RealtimeEventEnvelope<>(
                                "PASSENGER_BOARDED",
                                EVENT_VERSION,
                                boardedAt,
                                new RealtimeResource(TRIP_RESOURCE, tripId),
                                new PassengerBoardedRealtimeData(
                                                tripId,
                                                routeId,
                                                rideRequestId,
                                                pickupStopId,
                                                pickupStopOrder,
                                                TrangThaiYeuCau.ON_BOARD.name(),
                                                com.zanh.route_sharing.domain.enums.TrangThaiDiemDung.COMPLETED.name(),
                                                boardedAt));
        }

        public static RealtimeEventEnvelope<PassengerDroppedOffRealtimeData> passengerDroppedOff(
                        Long tripId, Long routeId, Long rideRequestId, Long dropoffStopId, Integer dropoffStopOrder,
                        Instant droppedOffAt) {
                return new RealtimeEventEnvelope<>(
                                "PASSENGER_DROPPED_OFF",
                                EVENT_VERSION,
                                droppedOffAt,
                                new RealtimeResource(TRIP_RESOURCE, tripId),
                                new PassengerDroppedOffRealtimeData(
                                                tripId, routeId, rideRequestId, dropoffStopId, dropoffStopOrder,
                                                TrangThaiYeuCau.COMPLETED.name(),
                                                com.zanh.route_sharing.domain.enums.TrangThaiDiemDung.COMPLETED.name(),
                                                droppedOffAt));
        }

        public static RealtimeEventEnvelope<PassengerNoShowRealtimeData> passengerNoShow(
                        Long tripId,
                        Long routeId,
                        Long rideRequestId,
                        Long pickupStopId,
                        Integer pickupStopOrder,
                        Long dropoffStopId,
                        Integer dropoffStopOrder,
                        Instant noShowAt) {
                return new RealtimeEventEnvelope<>(
                                "PASSENGER_NO_SHOW",
                                EVENT_VERSION,
                                noShowAt,
                                new RealtimeResource(TRIP_RESOURCE, tripId),
                                new PassengerNoShowRealtimeData(
                                                tripId,
                                                routeId,
                                                rideRequestId,
                                                pickupStopId,
                                                pickupStopOrder,
                                                dropoffStopId,
                                                dropoffStopOrder,
                                                TrangThaiYeuCau.NO_SHOW.name(),
                                                com.zanh.route_sharing.domain.enums.TrangThaiDiemDung.SKIPPED.name(),
                                                com.zanh.route_sharing.domain.enums.TrangThaiDiemDung.SKIPPED.name(),
                                                noShowAt));
        }

        public static RealtimeEventEnvelope<TripLocationUpdatedRealtimeData> tripLocationUpdated(
                        Long tripId,
                        BigDecimal latitude,
                        BigDecimal longitude,
                        Instant observedAt,
                        Instant receivedAt,
                        BigDecimal accuracyMeters,
                        Long locationSequence) {
                return new RealtimeEventEnvelope<>(
                                "TRIP_LOCATION_UPDATED",
                                EVENT_VERSION,
                                receivedAt,
                                new RealtimeResource(TRIP_RESOURCE, tripId),
                                new TripLocationUpdatedRealtimeData(
                                                tripId,
                                                new TripLocationUpdatedRealtimeData.Position(latitude, longitude),
                                                observedAt,
                                                receivedAt,
                                                accuracyMeters,
                                                locationSequence));
        }

        public static RealtimeEventEnvelope<TripSignalMonitoringChangedRealtimeData> tripSignalMonitoringChanged(
                        Long tripId,
                        TrangThaiGiamSatChuyenDi previousStatus,
                        TrangThaiGiamSatChuyenDi monitoringStatus,
                        Instant signalReferenceAt,
                        Instant changedAt) {
                if (previousStatus == null || monitoringStatus == null || previousStatus == monitoringStatus) {
                        throw new IllegalArgumentException("Monitoring realtime event phải là actual transition.");
                }
                if (signalReferenceAt == null || changedAt == null || signalReferenceAt.isAfter(changedAt)) {
                        throw new IllegalArgumentException("Monitoring realtime timestamps không hợp lệ.");
                }
                return new RealtimeEventEnvelope<>(
                                "TRIP_SIGNAL_MONITORING_CHANGED",
                                EVENT_VERSION,
                                changedAt,
                                new RealtimeResource(TRIP_RESOURCE, tripId),
                                new TripSignalMonitoringChangedRealtimeData(
                                                tripId,
                                                previousStatus.name(),
                                                monitoringStatus.name(),
                                                signalReferenceAt,
                                                changedAt));
        }

        public static RealtimeEventEnvelope<TripStartedRealtimeData> tripStarted(
                        Long tripId,
                        Long routeId,
                        Long rideRequestId,
                        Instant startedAt) {
                return new RealtimeEventEnvelope<>(
                                "TRIP_STARTED",
                                EVENT_VERSION,
                                startedAt,
                                new RealtimeResource(TRIP_RESOURCE, tripId),
                                new TripStartedRealtimeData(
                                                tripId,
                                                routeId,
                                                rideRequestId,
                                                TrangThaiVanHanhChuyenDi.IN_PROGRESS.name(),
                                                startedAt));
        }

        public static RealtimeEventEnvelope<TripSafetyIncidentReportedRealtimeData> tripSafetyIncidentReported(
                        Long incidentId,
                        Long tripId,
                        String type,
                        String severity,
                        String status,
                        Instant reportedAt) {
                return new RealtimeEventEnvelope<>(
                                "TRIP_SAFETY_INCIDENT_REPORTED",
                                EVENT_VERSION,
                                reportedAt,
                                new RealtimeResource(TRIP_INCIDENT_RESOURCE, incidentId),
                                new TripSafetyIncidentReportedRealtimeData(
                                                incidentId, tripId, type, severity, status, reportedAt));
        }

        public static RealtimeEventEnvelope<TripSafetyIncidentWorkChangedRealtimeData> tripSafetyIncidentWorkChanged(
                        Long incidentId,
                        Long tripId,
                        String changeType,
                        String status,
                        Long primaryHandlerUserId,
                        Instant occurredAt) {
                return new RealtimeEventEnvelope<>(
                                "TRIP_SAFETY_INCIDENT_WORK_CHANGED",
                                EVENT_VERSION,
                                occurredAt,
                                new RealtimeResource(TRIP_INCIDENT_RESOURCE, incidentId),
                                new TripSafetyIncidentWorkChangedRealtimeData(
                                                incidentId, tripId, changeType, status, primaryHandlerUserId));
        }

        public static RealtimeEventEnvelope<TripSafetyIncidentStatusChangedRealtimeData> tripSafetyIncidentStatusChanged(
                        Long incidentId,
                        Long tripId,
                        String status,
                        Instant occurredAt) {
                return new RealtimeEventEnvelope<>(
                                "TRIP_SAFETY_INCIDENT_STATUS_CHANGED",
                                EVENT_VERSION,
                                occurredAt,
                                new RealtimeResource(TRIP_INCIDENT_RESOURCE, incidentId),
                                new TripSafetyIncidentStatusChangedRealtimeData(incidentId, tripId, status));
        }

        public static RealtimeEventEnvelope<TripSafetyStateChangedRealtimeData> tripSafetyStateChanged(
                        Long tripId, Long interventionId, String changeType, String tripStatus, Instant changedAt) {
                return new RealtimeEventEnvelope<>(
                                "TRIP_SAFETY_STATE_CHANGED", EVENT_VERSION, changedAt,
                                new RealtimeResource(TRIP_RESOURCE, tripId),
                                new TripSafetyStateChangedRealtimeData(tripId, interventionId, changeType, tripStatus,
                                                changedAt));
        }

        public static RealtimeEventEnvelope<TripParticipantSafetyChangedRealtimeData> tripParticipantSafetyChanged(
                        Long tripId, Long interventionId, Long rideRequestId, String bookingStatus, Instant changedAt) {
                return new RealtimeEventEnvelope<>(
                                "TRIP_PARTICIPANT_SAFETY_CHANGED", EVENT_VERSION, changedAt,
                                new RealtimeResource("RIDE_REQUEST", rideRequestId),
                                new TripParticipantSafetyChangedRealtimeData(tripId, interventionId, rideRequestId,
                                                "PASSENGER_ABORTED", bookingStatus, changedAt));
        }

}
