package com.zanh.route_sharing.dto.riderequest.query;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RouteRideRequestPageResponse(
        RouteContext route,
        List<PendingRequestSummary> items) {

    public RouteRideRequestPageResponse {
        Objects.requireNonNull(route, "route must not be null");
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record RouteContext(
            Long routeId,
            TrangThaiLoTrinh routeStatus,
            Instant expectedDepartureTime,
            Integer offeredSeats,
            Integer remainingSeats) {

        public RouteContext {
            Objects.requireNonNull(routeId, "routeId must not be null");
            Objects.requireNonNull(routeStatus, "routeStatus must not be null");
            Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime must not be null");
            Objects.requireNonNull(offeredSeats, "offeredSeats must not be null");
            Objects.requireNonNull(remainingSeats, "remainingSeats must not be null");
        }
    }

    public record PendingRequestSummary(
            Long rideRequestId,
            TrangThaiYeuCau status,
            Instant sentAt,
            PassengerSummary passenger,
            String pickupAddress,
            String passengerDestinationAddress,
            LoaiGhepTuyen matchType,
            LoaiDiemTha dropoffType,
            BigDecimal proposedSupportAmount) {

        public PendingRequestSummary {
            Objects.requireNonNull(rideRequestId, "rideRequestId must not be null");
            if (status != TrangThaiYeuCau.PENDING) {
                throw new IllegalArgumentException("Pending summary must have PENDING status");
            }
            Objects.requireNonNull(sentAt, "sentAt must not be null");
            Objects.requireNonNull(passenger, "passenger must not be null");
            Objects.requireNonNull(pickupAddress, "pickupAddress must not be null");
            Objects.requireNonNull(passengerDestinationAddress,
                    "passengerDestinationAddress must not be null");
            Objects.requireNonNull(matchType, "matchType must not be null");
            Objects.requireNonNull(dropoffType, "dropoffType must not be null");
            Objects.requireNonNull(proposedSupportAmount, "proposedSupportAmount must not be null");
        }
    }

    public record PassengerSummary(
            Long passengerId,
            String fullName,
            String avatarUrl) {

        public PassengerSummary {
            Objects.requireNonNull(passengerId, "passengerId must not be null");
            Objects.requireNonNull(fullName, "fullName must not be null");
        }
    }
}
