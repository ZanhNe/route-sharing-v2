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
        Objects.requireNonNull(route, "route không được trống.");
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record RouteContext(
            Long routeId,
            TrangThaiLoTrinh routeStatus,
            Instant expectedDepartureTime,
            Integer offeredSeats,
            Integer remainingSeats) {

        public RouteContext {
            Objects.requireNonNull(routeId, "routeId không được trống.");
            Objects.requireNonNull(routeStatus, "routeStatus không được trống.");
            Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống.");
            Objects.requireNonNull(offeredSeats, "offeredSeats không được trống.");
            Objects.requireNonNull(remainingSeats, "remainingSeats không được trống.");
        }
    }

    public record PendingRequestSummary(
            Long rideRequestId,
            TrangThaiYeuCau status,
            Instant sentAt,
            Instant expiresAt,
            boolean expired,
            PassengerSummary passenger,
            String pickupAddress,
            String passengerDestinationAddress,
            LoaiGhepTuyen matchType,
            LoaiDiemTha dropoffType,
            BigDecimal proposedSupportAmount) {

        public PendingRequestSummary {
            Objects.requireNonNull(rideRequestId, "rideRequestId không được trống.");
            if (status != TrangThaiYeuCau.PENDING) {
                throw new IllegalArgumentException("Pending summary phải có trạng thái PENDING.");
            }
            Objects.requireNonNull(sentAt, "sentAt không được trống.");
            Objects.requireNonNull(expiresAt, "expiresAt không được trống.");
            Objects.requireNonNull(passenger, "passenger không được trống.");
            Objects.requireNonNull(pickupAddress, "pickupAddress không được trống.");
            Objects.requireNonNull(passengerDestinationAddress,
                    "passengerDestinationAddress không được trống.");
            Objects.requireNonNull(matchType, "matchType không được trống.");
            Objects.requireNonNull(dropoffType, "dropoffType không được trống.");
            Objects.requireNonNull(proposedSupportAmount, "proposedSupportAmount không được trống.");
        }
    }

    public record PassengerSummary(
            Long passengerId,
            String fullName,
            String avatarUrl) {

        public PassengerSummary {
            Objects.requireNonNull(passengerId, "passengerId không được trống.");
            Objects.requireNonNull(fullName, "fullName không được trống.");
        }
    }
}
