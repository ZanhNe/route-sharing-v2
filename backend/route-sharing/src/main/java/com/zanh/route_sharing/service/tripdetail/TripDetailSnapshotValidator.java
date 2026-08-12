package com.zanh.route_sharing.service.tripdetail;

import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripDetailParticipantRow;
import com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripDetailSnapshot;
import com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripViewerRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Component
public class TripDetailSnapshotValidator {

    private static final Set<TrangThaiYeuCau> FUTURE_IN_PROGRESS_BOOKING_STATES = EnumSet.of(
            TrangThaiYeuCau.PICKUP_FAILED,
            TrangThaiYeuCau.COMPLETED,
            TrangThaiYeuCau.ABORTED,
            TrangThaiYeuCau.DISPUTED);

    public void validate(TripDetailSnapshot snapshot) {
        var header = snapshot.header();
        if (header.tripStatus() != TrangThaiVanHanhChuyenDi.PREPARING
                && header.tripStatus() != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                && header.tripStatus() != TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START) {
            throw outsideCurrentScope();
        }

        validateBaseStructure(snapshot);
        if (header.viewerRole() == TripViewerRole.DRIVER) {
            validateDriverStructure(snapshot);
        } else {
            validatePassengerStructure(snapshot);
        }

        if (header.tripStatus() == TrangThaiVanHanhChuyenDi.PREPARING) {
            validatePreparing(snapshot);
            return;
        }
        if (header.tripStatus() == TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START) {
            validateCancelledBeforeStart(snapshot);
            return;
        }
        validateInProgressAfterStart(snapshot);
    }

    private static void validateBaseStructure(TripDetailSnapshot snapshot) {
        var header = snapshot.header();
        if (header.lockedAt() == null || header.formedAt() == null) {
            throw invalidStoredPlan();
        }
        if (header.plannedPassengerCount() == null || header.plannedPassengerCount() <= 0
                || header.actualPassengerCount() == null || header.actualPassengerCount() < 0
                || header.actualPassengerCount() > header.plannedPassengerCount()) {
            throw invalidStoredPlan();
        }
        if (header.driverStartStatus() == null) {
            throw invalidStoredPlan();
        }
    }

    private static void validateDriverStructure(TripDetailSnapshot snapshot) {
        int n = snapshot.header().plannedPassengerCount();
        if (snapshot.participants().size() != n || snapshot.stops().size() != 2 + 2 * n) {
            throw invalidStoredPlan();
        }
        long starts = snapshot.stops().stream().filter(s -> s.type() == LoaiDiemDung.DRIVER_START).count();
        long ends = snapshot.stops().stream().filter(s -> s.type() == LoaiDiemDung.DRIVER_END).count();
        if (starts != 1 || ends != 1) {
            throw invalidStoredPlan();
        }

        Set<Long> participantIds = new HashSet<>();
        for (var participant : snapshot.participants()) {
            if (!participantIds.add(participant.rideRequestId())) {
                throw invalidStoredPlan();
            }
            validateParticipantStructure(participant);
            if (snapshot.stops().stream().noneMatch(s -> participant.pickupStopId().equals(s.stopId())
                    && s.type() == LoaiDiemDung.PICKUP
                    && participant.rideRequestId().equals(s.rideRequestId()))) {
                throw invalidStoredPlan();
            }
            if (snapshot.stops().stream().noneMatch(s -> participant.dropoffStopId().equals(s.stopId())
                    && s.type() == LoaiDiemDung.DROPOFF
                    && participant.rideRequestId().equals(s.rideRequestId()))) {
                throw invalidStoredPlan();
            }
        }

        var visibleDriverStart = snapshot.stops().stream()
                .filter(stop -> stop.type() == LoaiDiemDung.DRIVER_START)
                .findFirst()
                .orElseThrow(TripDetailSnapshotValidator::invalidStoredPlan);
        if (visibleDriverStart.status() != snapshot.header().driverStartStatus()) {
            throw invalidStoredPlan();
        }
        validateOrder(snapshot);
    }

    private static void validatePassengerStructure(TripDetailSnapshot snapshot) {
        if (snapshot.participants().size() != 1 || snapshot.stops().size() != 2) {
            throw invalidStoredPlan();
        }
        long pickup = snapshot.stops().stream().filter(s -> s.type() == LoaiDiemDung.PICKUP).count();
        long dropoff = snapshot.stops().stream().filter(s -> s.type() == LoaiDiemDung.DROPOFF).count();
        if (pickup != 1 || dropoff != 1) {
            throw invalidStoredPlan();
        }
        var participant = snapshot.participants().get(0);
        Long rideRequestId = participant.rideRequestId();
        if (snapshot.stops().stream().anyMatch(stop -> !rideRequestId.equals(stop.rideRequestId()))) {
            throw invalidStoredPlan();
        }
        if (snapshot.stops().stream().noneMatch(stop -> participant.pickupStopId().equals(stop.stopId())
                && stop.type() == LoaiDiemDung.PICKUP)) {
            throw invalidStoredPlan();
        }
        if (snapshot.stops().stream().noneMatch(stop -> participant.dropoffStopId().equals(stop.stopId())
                && stop.type() == LoaiDiemDung.DROPOFF)) {
            throw invalidStoredPlan();
        }
        validateParticipantStructure(participant);
        validateOrder(snapshot);
    }

    private static void validateParticipantStructure(TripDetailParticipantRow participant) {
        if (participant.status() == null || participant.acceptedAt() == null
                || participant.agreedSupportAmount() == null) {
            throw invalidStoredPlan();
        }
        if (participant.status() == TrangThaiYeuCau.ON_BOARD) {
            if (participant.boardedAt() == null || participant.boardedAt().isBefore(participant.acceptedAt())
                    || participant.noShowAt() != null) {
                throw invalidStoredPlan();
            }
        } else if (participant.status() == TrangThaiYeuCau.NO_SHOW) {
            if (participant.noShowAt() == null || participant.noShowAt().isBefore(participant.acceptedAt())
                    || participant.boardedAt() != null) {
                throw invalidStoredPlan();
            }
        } else if (participant.boardedAt() != null || participant.noShowAt() != null) {
            throw invalidStoredPlan();
        }
    }

    private static void validatePreparing(TripDetailSnapshot snapshot) {
        var header = snapshot.header();
        if (header.routeStatus() != TrangThaiLoTrinh.LOCKED
                || header.startedAt() != null
                || header.cancelledAt() != null
                || header.cancellationReason() != null
                || header.actualPassengerCount() != 0
                || header.driverStartStatus() != TrangThaiDiemDung.PENDING
                || header.driverStartCompletedAt() != null) {
            throw invalidStoredPlan();
        }
        if (snapshot.participants().stream().anyMatch(p -> p.status() != TrangThaiYeuCau.ACCEPTED)) {
            throw invalidStoredPlan();
        }
        if (snapshot.stops().stream().anyMatch(stop -> stop.status() != TrangThaiDiemDung.PENDING)) {
            throw invalidStoredPlan();
        }
    }

    private static void validateInProgressAfterStart(TripDetailSnapshot snapshot) {
        var header = snapshot.header();
        if (header.routeStatus() != TrangThaiLoTrinh.LOCKED
                || header.cancelledAt() != null
                || header.cancellationReason() != null
                || header.startedAt() == null
                || header.startedAt().isBefore(header.formedAt())
                || header.driverStartStatus() != TrangThaiDiemDung.COMPLETED
                || header.driverStartCompletedAt() == null
                || header.driverStartCompletedAt().isBefore(header.formedAt())) {
            throw invalidStoredPlan();
        }

        int onBoardParticipants = 0;
        for (var participant : snapshot.participants()) {
            if (participant.status() == TrangThaiYeuCau.ACCEPTED) {
                continue;
            }
            if (participant.status() == TrangThaiYeuCau.ON_BOARD) {
                onBoardParticipants++;
                continue;
            }
            if (participant.status() == TrangThaiYeuCau.NO_SHOW) {
                continue;
            }
            if (FUTURE_IN_PROGRESS_BOOKING_STATES.contains(participant.status())) {
                throw outsideCurrentScope();
            }
            throw invalidStoredPlan();
        }
        if (header.viewerRole() == TripViewerRole.DRIVER
                && header.actualPassengerCount() != onBoardParticipants) {
            throw invalidStoredPlan();
        }

        int arrivedPickups = 0;
        for (var stop : snapshot.stops()) {
            if (stop.type() == LoaiDiemDung.DRIVER_START) {
                if (stop.status() != TrangThaiDiemDung.COMPLETED
                        || stop.arrivedAt() != null
                        || stop.waitingStartedAt() != null
                        || stop.waitingDeadline() != null
                        || stop.completedAt() == null) {
                    throw invalidStoredPlan();
                }
                continue;
            }
            if (stop.status() == TrangThaiDiemDung.PENDING) {
                if (stop.arrivedAt() != null
                        || stop.waitingStartedAt() != null
                        || stop.waitingDeadline() != null
                        || stop.completedAt() != null) {
                    throw invalidStoredPlan();
                }
                continue;
            }
            if (stop.type() == LoaiDiemDung.PICKUP && stop.status() == TrangThaiDiemDung.ARRIVED) {
                if (stop.arrivedAt() == null
                        || stop.waitingStartedAt() == null
                        || stop.waitingDeadline() == null
                        || stop.completedAt() != null
                        || !stop.waitingStartedAt().equals(stop.arrivedAt())
                        || stop.waitingDeadline().isBefore(stop.arrivedAt())) {
                    throw invalidStoredPlan();
                }
                arrivedPickups++;
                continue;
            }
            if (stop.type() == LoaiDiemDung.PICKUP && stop.status() == TrangThaiDiemDung.SKIPPED) {
                if (stop.arrivedAt() == null
                        || stop.waitingStartedAt() == null
                        || stop.waitingDeadline() == null
                        || stop.completedAt() != null
                        || !stop.waitingStartedAt().equals(stop.arrivedAt())
                        || stop.waitingDeadline().isBefore(stop.arrivedAt())) {
                    throw invalidStoredPlan();
                }
                continue;
            }
            if (stop.type() == LoaiDiemDung.DROPOFF && stop.status() == TrangThaiDiemDung.SKIPPED) {
                if (stop.arrivedAt() != null || stop.waitingStartedAt() != null
                        || stop.waitingDeadline() != null || stop.completedAt() != null) {
                    throw invalidStoredPlan();
                }
                continue;
            }
            if (stop.type() == LoaiDiemDung.PICKUP && stop.status() == TrangThaiDiemDung.COMPLETED) {
                if (stop.arrivedAt() == null
                        || stop.waitingStartedAt() == null
                        || stop.waitingDeadline() == null
                        || stop.completedAt() == null
                        || !stop.waitingStartedAt().equals(stop.arrivedAt())
                        || stop.waitingDeadline().isBefore(stop.arrivedAt())
                        || stop.completedAt().isBefore(stop.arrivedAt())) {
                    throw invalidStoredPlan();
                }
                continue;
            }
            throw outsideCurrentScope();
        }
        if (snapshot.header().viewerRole() == TripViewerRole.DRIVER && arrivedPickups > 1) {
            throw outsideCurrentScope();
        }

        for (var participant : snapshot.participants()) {
            var pickup = snapshot.stops().stream()
                    .filter(stop -> participant.pickupStopId().equals(stop.stopId()))
                    .findFirst()
                    .orElseThrow(TripDetailSnapshotValidator::invalidStoredPlan);
            if (participant.status() == TrangThaiYeuCau.ON_BOARD) {
                if (pickup.status() != TrangThaiDiemDung.COMPLETED
                        || pickup.completedAt() == null
                        || !pickup.completedAt().equals(participant.boardedAt())) {
                    throw invalidStoredPlan();
                }
            }
            var dropoff = snapshot.stops().stream()
                    .filter(stop -> participant.dropoffStopId().equals(stop.stopId()))
                    .findFirst()
                    .orElseThrow(TripDetailSnapshotValidator::invalidStoredPlan);
            if (participant.status() == TrangThaiYeuCau.NO_SHOW) {
                if (pickup.status() != TrangThaiDiemDung.SKIPPED
                        || pickup.completedAt() != null
                        || pickup.waitingDeadline() == null
                        || participant.noShowAt().isBefore(pickup.waitingDeadline())
                        || dropoff.status() != TrangThaiDiemDung.SKIPPED
                        || dropoff.completedAt() != null) {
                    throw invalidStoredPlan();
                }
            }
            if (participant.status() == TrangThaiYeuCau.ACCEPTED
                    && (pickup.status() == TrangThaiDiemDung.COMPLETED || pickup.status() == TrangThaiDiemDung.SKIPPED)) {
                throw invalidStoredPlan();
            }
        }
    }

    private static void validateCancelledBeforeStart(TripDetailSnapshot snapshot) {
        var header = snapshot.header();
        if (header.routeStatus() != TrangThaiLoTrinh.CANCELLED
                || header.startedAt() != null
                || header.cancelledAt() == null
                || header.cancelledAt().isBefore(header.formedAt())
                || header.cancellationReason() == null
                || header.cancellationReason().isBlank()
                || header.cancellationReason().length() > 2000
                || header.actualPassengerCount() != 0
                || header.driverStartStatus() != TrangThaiDiemDung.CANCELLED
                || header.driverStartCompletedAt() != null) {
            throw invalidStoredPlan();
        }
        if (snapshot.participants().stream()
                .anyMatch(p -> p.status() != TrangThaiYeuCau.CANCELLED_BY_DRIVER)) {
            throw invalidStoredPlan();
        }
        if (snapshot.stops().stream()
                .anyMatch(stop -> stop.status() != TrangThaiDiemDung.CANCELLED
                        || stop.arrivedAt() != null
                        || stop.waitingStartedAt() != null
                        || stop.waitingDeadline() != null
                        || stop.completedAt() != null)) {
            throw invalidStoredPlan();
        }
    }

    private static void validateOrder(TripDetailSnapshot snapshot) {
        int previous = 0;
        Set<Integer> seen = new HashSet<>();
        for (var stop : snapshot.stops()) {
            if (stop.order() == null || stop.order() <= previous || !seen.add(stop.order())) {
                throw invalidStoredPlan();
            }
            previous = stop.order();
        }
    }

    public static BusinessException outsideCurrentScope() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "TRIP_DETAIL_OUTSIDE_CURRENT_SCOPE",
                "Trạng thái vận hành hiện tại của chuyến chưa thuộc phạm vi Trip Detail được hỗ trợ.");
    }

    public static BusinessException invalidStoredPlan() {
        return new BusinessException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INVALID_STORED_TRIP_PLAN",
                "Dữ liệu vận hành chuyến đi đang lưu không nhất quán.");
    }
}
