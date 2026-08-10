package com.zanh.route_sharing.dto.trip.detail;

import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.sharedroute.preview.GeoJsonLineStringResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record TripDetailResponse(
        ViewerRole viewerRole,
        Trip trip,
        Route route,
        Driver driver,
        Vehicle vehicle,
        OperationalRoute operationalRoute,
        List<Participant> participants,
        List<Stop> stops,
        Instant readAt) {

    public TripDetailResponse {
        Objects.requireNonNull(viewerRole, "viewerRole không được trống.");
        Objects.requireNonNull(trip, "trip không được trống.");
        Objects.requireNonNull(route, "route không được trống.");
        Objects.requireNonNull(driver, "driver không được trống.");
        Objects.requireNonNull(vehicle, "vehicle không được trống.");
        Objects.requireNonNull(operationalRoute, "operationalRoute không được trống.");
        participants = participants == null ? List.of() : List.copyOf(participants);
        stops = stops == null ? List.of() : List.copyOf(stops);
        Objects.requireNonNull(readAt, "readAt không được trống.");
    }

    public enum ViewerRole {
        DRIVER,
        PASSENGER
    }

    public record Trip(
            Long tripId,
            TrangThaiVanHanhChuyenDi status,
            Instant formedAt,
            Instant startedAt,
            Integer plannedPassengerCount,
            Integer actualPassengerCount) {
        public Trip {
            Objects.requireNonNull(tripId, "tripId không được trống.");
            Objects.requireNonNull(status, "trip status không được trống.");
            Objects.requireNonNull(formedAt, "formedAt không được trống.");
            Objects.requireNonNull(plannedPassengerCount, "plannedPassengerCount không được trống.");
            Objects.requireNonNull(actualPassengerCount, "actualPassengerCount không được trống.");
        }
    }

    public record Route(
            Long routeId,
            TrangThaiLoTrinh status,
            Instant lockedAt,
            Instant expectedDepartureTime,
            Integer offeredSeats,
            Integer remainingSeats,
            Point origin,
            Point driverDestination) {
        public Route {
            Objects.requireNonNull(routeId, "routeId không được trống.");
            Objects.requireNonNull(status, "route status không được trống.");
            Objects.requireNonNull(lockedAt, "lockedAt không được trống.");
            Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống.");
            Objects.requireNonNull(offeredSeats, "offeredSeats không được trống.");
            Objects.requireNonNull(remainingSeats, "remainingSeats không được trống.");
            Objects.requireNonNull(origin, "origin không được trống.");
            Objects.requireNonNull(driverDestination, "driverDestination không được trống.");
        }
    }

    public record Driver(Long driverId, String fullName, String avatarUrl) {
        public Driver {
            Objects.requireNonNull(driverId, "driverId không được trống.");
            Objects.requireNonNull(fullName, "driver fullName không được trống.");
        }
    }

    public record Vehicle(
            Long vehicleId,
            String licensePlate,
            String actualColor,
            String brandName,
            String modelName) {
        public Vehicle {
            Objects.requireNonNull(vehicleId, "vehicleId không được trống.");
            Objects.requireNonNull(licensePlate, "licensePlate không được trống.");
            Objects.requireNonNull(actualColor, "actualColor không được trống.");
            Objects.requireNonNull(brandName, "brandName không được trống.");
            Objects.requireNonNull(modelName, "modelName không được trống.");
        }
    }

    public record OperationalRoute(String meaning, GeoJsonLineStringResponse geometry) {
        public OperationalRoute {
            Objects.requireNonNull(meaning, "meaning không được trống.");
            Objects.requireNonNull(geometry, "geometry không được trống.");
        }
    }

    public record Participant(
            Long rideRequestId,
            Passenger passenger,
            Booking booking,
            Long pickupStopId,
            Long dropoffStopId) {
        public Participant {
            Objects.requireNonNull(rideRequestId, "rideRequestId không được trống.");
            Objects.requireNonNull(passenger, "passenger không được trống.");
            Objects.requireNonNull(booking, "booking không được trống.");
            Objects.requireNonNull(pickupStopId, "pickupStopId không được trống.");
            Objects.requireNonNull(dropoffStopId, "dropoffStopId không được trống.");
        }
    }

    public record Passenger(Long passengerId, String fullName, String avatarUrl) {
        public Passenger {
            Objects.requireNonNull(passengerId, "passengerId không được trống.");
            Objects.requireNonNull(fullName, "passenger fullName không được trống.");
        }
    }

    public record Booking(
            TrangThaiYeuCau status,
            Instant acceptedAt,
            LoaiGhepTuyen matchType,
            LoaiDiemTha dropoffType,
            BigDecimal agreedSupportAmount,
            String note) {
        public Booking {
            Objects.requireNonNull(status, "booking status không được trống.");
            Objects.requireNonNull(acceptedAt, "acceptedAt không được trống.");
            Objects.requireNonNull(matchType, "matchType không được trống.");
            Objects.requireNonNull(dropoffType, "dropoffType không được trống.");
            Objects.requireNonNull(agreedSupportAmount, "agreedSupportAmount không được trống.");
        }
    }

    public record Stop(
            Long stopId,
            Integer order,
            LoaiDiemDung type,
            TrangThaiDiemDung status,
            Long rideRequestId,
            Point point) {
        public Stop {
            Objects.requireNonNull(stopId, "stopId không được trống.");
            Objects.requireNonNull(order, "order không được trống.");
            Objects.requireNonNull(type, "stop type không được trống.");
            Objects.requireNonNull(status, "stop status không được trống.");
            Objects.requireNonNull(point, "stop point không được trống.");
        }
    }

    public record Point(BigDecimal latitude, BigDecimal longitude, String address) {
        public Point {
            Objects.requireNonNull(latitude, "latitude không được trống.");
            Objects.requireNonNull(longitude, "longitude không được trống.");
            Objects.requireNonNull(address, "address không được trống.");
        }
    }
}
