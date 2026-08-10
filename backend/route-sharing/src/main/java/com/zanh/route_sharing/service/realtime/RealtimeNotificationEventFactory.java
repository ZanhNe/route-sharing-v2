package com.zanh.route_sharing.service.realtime;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.service.realtime.model.BookingAcceptedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.BookingCancelledByPassengerRealtimeData;
import com.zanh.route_sharing.service.realtime.model.BookingRejectedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.BookingRequestRealtimeData;
import com.zanh.route_sharing.service.realtime.model.RealtimeEventEnvelope;
import com.zanh.route_sharing.service.realtime.model.RealtimeResource;
import com.zanh.route_sharing.service.realtime.model.RouteCancelledByDriverRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripFormedRealtimeData;
import com.zanh.route_sharing.service.realtime.model.TripStartedRealtimeData;

import java.math.BigDecimal;
import java.time.Instant;

public final class RealtimeNotificationEventFactory {
        public static final int EVENT_VERSION = 1;
        public static final String RIDE_REQUEST_RESOURCE = "RIDE_REQUEST";
        public static final String SHARED_ROUTE_RESOURCE = "SHARED_ROUTE";
        public static final String TRIP_RESOURCE = "TRIP";

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

}
