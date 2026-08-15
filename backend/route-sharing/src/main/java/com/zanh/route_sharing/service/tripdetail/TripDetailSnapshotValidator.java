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

import java.util.HashSet;
import java.util.Set;

@Component
public class TripDetailSnapshotValidator {

    public void validate(TripDetailSnapshot snapshot) {
        var header = snapshot.header();
        if (header.tripStatus() != TrangThaiVanHanhChuyenDi.PREPARING
                && header.tripStatus() != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                && header.tripStatus() != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN
                && header.tripStatus() != TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START
                && header.tripStatus() != TrangThaiVanHanhChuyenDi.EMERGENCY_ABORTED) {
            throw outsideCurrentScope();
        }

        validateBaseStructure(snapshot);
        if (header.viewerRole() == TripViewerRole.DRIVER)
            validateDriverStructure(snapshot);
        else
            validatePassengerStructure(snapshot);
        validateCurrentDriverLocation(snapshot);

        switch (header.tripStatus()) {
            case PREPARING -> validatePreparing(snapshot);
            case CANCELLED_BEFORE_START -> validateCancelledBeforeStart(snapshot);
            case IN_PROGRESS, SECURITY_FROZEN -> validateOperationalAfterStart(snapshot);
            case EMERGENCY_ABORTED -> validateEmergencyAborted(snapshot);
            default -> throw outsideCurrentScope();
        }
    }

    private static void validateBaseStructure(TripDetailSnapshot snapshot) {
        var h = snapshot.header();
        if (h.lockedAt() == null || h.formedAt() == null || h.activeSafetyHoldCount() == null
                || h.activeSafetyHoldCount() < 0) {
            throw invalidStoredPlan();
        }
        if (h.plannedPassengerCount() == null || h.plannedPassengerCount() <= 0
                || h.actualPassengerCount() == null || h.actualPassengerCount() < 0
                || h.actualPassengerCount() > h.plannedPassengerCount()
                || h.driverStartStatus() == null) {
            throw invalidStoredPlan();
        }
        boolean activeHoldProjection = h.safetyHoldStartedAt() != null || h.safetyMessage() != null
                || h.activeSafetyHoldCount() > 0 || h.activeSafetyHoldInterventionId() != null
                || h.activeSafetyHoldTargetRideRequestId() != null;
        if (h.tripStatus() == TrangThaiVanHanhChuyenDi.SECURITY_FROZEN) {
            if (h.activeSafetyHoldCount() != 1 || h.safetyHoldStartedAt() == null || h.safetyMessage() == null
                    || h.safetyMessage().isBlank() || h.activeSafetyHoldInterventionId() == null
                    || h.activeSafetyHoldTargetRideRequestId() == null) {
                throw invalidStoredPlan();
            }
        } else if (activeHoldProjection) {
            throw invalidStoredPlan();
        }
    }

    private static void validateDriverStructure(TripDetailSnapshot snapshot) {
        int n = snapshot.header().plannedPassengerCount();
        if (snapshot.participants().size() != n || snapshot.stops().size() != 2 + 2 * n)
            throw invalidStoredPlan();
        long starts = snapshot.stops().stream().filter(s -> s.type() == LoaiDiemDung.DRIVER_START).count();
        long ends = snapshot.stops().stream().filter(s -> s.type() == LoaiDiemDung.DRIVER_END).count();
        if (starts != 1 || ends != 1)
            throw invalidStoredPlan();

        Set<Long> participantIds = new HashSet<>();
        for (var participant : snapshot.participants()) {
            if (!participantIds.add(participant.rideRequestId()))
                throw invalidStoredPlan();
            validateParticipantStructure(participant);
            if (snapshot.stops().stream().noneMatch(s -> participant.pickupStopId().equals(s.stopId())
                    && s.type() == LoaiDiemDung.PICKUP && participant.rideRequestId().equals(s.rideRequestId())))
                throw invalidStoredPlan();
            if (snapshot.stops().stream().noneMatch(s -> participant.dropoffStopId().equals(s.stopId())
                    && s.type() == LoaiDiemDung.DROPOFF && participant.rideRequestId().equals(s.rideRequestId())))
                throw invalidStoredPlan();
        }
        var visibleDriverStart = snapshot.stops().stream().filter(s -> s.type() == LoaiDiemDung.DRIVER_START)
                .findFirst().orElseThrow(TripDetailSnapshotValidator::invalidStoredPlan);
        if (visibleDriverStart.status() != snapshot.header().driverStartStatus())
            throw invalidStoredPlan();
        validateOrder(snapshot);
    }

    private static void validatePassengerStructure(TripDetailSnapshot snapshot) {
        if (snapshot.participants().size() != 1 || snapshot.stops().size() != 2)
            throw invalidStoredPlan();
        long pickup = snapshot.stops().stream().filter(s -> s.type() == LoaiDiemDung.PICKUP).count();
        long dropoff = snapshot.stops().stream().filter(s -> s.type() == LoaiDiemDung.DROPOFF).count();
        if (pickup != 1 || dropoff != 1)
            throw invalidStoredPlan();
        var participant = snapshot.participants().get(0);
        Long requestId = participant.rideRequestId();
        if (snapshot.stops().stream().anyMatch(s -> !requestId.equals(s.rideRequestId())))
            throw invalidStoredPlan();
        if (snapshot.stops().stream()
                .noneMatch(s -> participant.pickupStopId().equals(s.stopId()) && s.type() == LoaiDiemDung.PICKUP))
            throw invalidStoredPlan();
        if (snapshot.stops().stream()
                .noneMatch(s -> participant.dropoffStopId().equals(s.stopId()) && s.type() == LoaiDiemDung.DROPOFF))
            throw invalidStoredPlan();
        validateParticipantStructure(participant);
        validateOrder(snapshot);
    }

    private static void validateParticipantStructure(TripDetailParticipantRow p) {
        if (p.status() == null || p.acceptedAt() == null || p.agreedSupportAmount() == null)
            throw invalidStoredPlan();
        if (p.boardedAt() != null && p.boardedAt().isBefore(p.acceptedAt()))
            throw invalidStoredPlan();
        if (p.noShowAt() != null && p.noShowAt().isBefore(p.acceptedAt()))
            throw invalidStoredPlan();
        switch (p.status()) {
            case ON_BOARD -> {
                if (p.boardedAt() == null || p.noShowAt() != null)
                    throw invalidStoredPlan();
            }
            case NO_SHOW -> {
                if (p.noShowAt() == null || p.boardedAt() != null)
                    throw invalidStoredPlan();
            }
            case ABORTED -> {
                if (p.noShowAt() != null)
                    throw invalidStoredPlan();
            }
            case ACCEPTED, CANCELLED_BY_DRIVER -> {
                if (p.boardedAt() != null || p.noShowAt() != null)
                    throw invalidStoredPlan();
            }
            default -> {
                if (p.boardedAt() != null || p.noShowAt() != null)
                    throw outsideCurrentScope();
            }
        }
    }

    private static void validateCurrentDriverLocation(TripDetailSnapshot snapshot) {
        var location = snapshot.currentDriverLocation();
        if (location == null)
            return;
        var h = snapshot.header();
        if (h.viewerRole() != TripViewerRole.PASSENGER
                || (h.tripStatus() != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                        && h.tripStatus() != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN)
                || snapshot.participants().size() != 1
                || !snapshot.participants().get(0).status().isActiveTripParticipant()
                || h.signalReferenceAt() == null || !h.signalReferenceAt().equals(location.receivedAt()))
            throw invalidStoredPlan();
    }

    private static void validatePreparing(TripDetailSnapshot snapshot) {
        var h = snapshot.header();
        if (h.routeStatus() != TrangThaiLoTrinh.LOCKED || h.startedAt() != null || h.endedAt() != null
                || h.cancelledAt() != null || h.cancellationReason() != null || h.actualPassengerCount() != 0
                || h.driverStartStatus() != TrangThaiDiemDung.PENDING || h.driverStartCompletedAt() != null)
            throw invalidStoredPlan();
        if (snapshot.participants().stream().anyMatch(p -> p.status() != TrangThaiYeuCau.ACCEPTED))
            throw invalidStoredPlan();
        if (snapshot.stops().stream().anyMatch(s -> s.status() != TrangThaiDiemDung.PENDING))
            throw invalidStoredPlan();
    }

    private static void validateOperationalAfterStart(TripDetailSnapshot snapshot) {
        var h = snapshot.header();
        validateStartedHeader(h);
        if (h.endedAt() != null)
            throw invalidStoredPlan();

        int onBoard = 0;
        for (var p : snapshot.participants()) {
            if (p.status() == TrangThaiYeuCau.ACCEPTED
                    || p.status() == TrangThaiYeuCau.NO_SHOW
                    || p.status() == TrangThaiYeuCau.ABORTED) {
                continue;
            }
            if (p.status() == TrangThaiYeuCau.ON_BOARD) {
                onBoard++;
                continue;
            }
            if (p.status() == TrangThaiYeuCau.PICKUP_FAILED
                    || p.status() == TrangThaiYeuCau.COMPLETED
                    || p.status() == TrangThaiYeuCau.DISPUTED) {
                throw outsideCurrentScope();
            }
            throw invalidStoredPlan();
        }
        if (h.viewerRole() == TripViewerRole.DRIVER && h.actualPassengerCount() != onBoard)
            throw invalidStoredPlan();
        if (h.tripStatus() == TrangThaiVanHanhChuyenDi.SECURITY_FROZEN && h.viewerRole() == TripViewerRole.DRIVER) {
            var holdTarget = snapshot.participants().stream()
                    .filter(p -> p.rideRequestId().equals(h.activeSafetyHoldTargetRideRequestId())).findFirst();
            if (holdTarget.isEmpty() || holdTarget.orElseThrow().status() != TrangThaiYeuCau.ON_BOARD)
                throw invalidStoredPlan();
        }

        validateOperationalStops(snapshot, false);
        validateParticipantStopConsistency(snapshot, false);
    }

    private static void validateEmergencyAborted(TripDetailSnapshot snapshot) {
        var h = snapshot.header();
        validateStartedHeader(h);
        if (h.endedAt() == null || h.endedAt().isBefore(h.startedAt()) || h.actualPassengerCount() != 0
                || snapshot.currentDriverLocation() != null)
            throw invalidStoredPlan();
        for (var p : snapshot.participants()) {
            if (p.status() != TrangThaiYeuCau.ABORTED && p.status() != TrangThaiYeuCau.NO_SHOW)
                throw invalidStoredPlan();
        }
        validateOperationalStops(snapshot, true);
        validateParticipantStopConsistency(snapshot, true);
    }

    private static void validateStartedHeader(
            com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripDetailHeaderRow h) {
        if (h.routeStatus() != TrangThaiLoTrinh.LOCKED || h.cancelledAt() != null || h.cancellationReason() != null
                || h.startedAt() == null || h.startedAt().isBefore(h.formedAt())
                || h.driverStartStatus() != TrangThaiDiemDung.COMPLETED || h.driverStartCompletedAt() == null
                || h.driverStartCompletedAt().isBefore(h.formedAt()))
            throw invalidStoredPlan();
    }

    private static void validateOperationalStops(TripDetailSnapshot snapshot, boolean terminalAbort) {
        int arrivedPickups = 0;
        for (var s : snapshot.stops()) {
            if (s.type() == LoaiDiemDung.DRIVER_START) {
                if (s.status() != TrangThaiDiemDung.COMPLETED || s.arrivedAt() != null || s.waitingStartedAt() != null
                        || s.waitingDeadline() != null || s.completedAt() == null)
                    throw invalidStoredPlan();
                continue;
            }
            if (s.status() == TrangThaiDiemDung.PENDING) {
                if (terminalAbort || hasAnyTimingEvidence(s))
                    throw invalidStoredPlan();
                continue;
            }
            if (s.status() == TrangThaiDiemDung.CANCELLED) {
                if (!terminalAbort && s.type() == LoaiDiemDung.DRIVER_END)
                    throw invalidStoredPlan();

                if (s.completedAt() != null)
                    throw invalidStoredPlan();
                continue;
            }
            if (s.type() == LoaiDiemDung.PICKUP && s.status() == TrangThaiDiemDung.ARRIVED) {
                if (terminalAbort || s.arrivedAt() == null || s.waitingStartedAt() == null
                        || s.waitingDeadline() == null
                        || s.completedAt() != null || !s.waitingStartedAt().equals(s.arrivedAt())
                        || s.waitingDeadline().isBefore(s.arrivedAt()))
                    throw invalidStoredPlan();
                arrivedPickups++;
                continue;
            }
            if (s.type() == LoaiDiemDung.PICKUP && s.status() == TrangThaiDiemDung.SKIPPED) {
                if (s.arrivedAt() == null || s.waitingStartedAt() == null || s.waitingDeadline() == null
                        || s.completedAt() != null
                        || !s.waitingStartedAt().equals(s.arrivedAt()) || s.waitingDeadline().isBefore(s.arrivedAt()))
                    throw invalidStoredPlan();
                continue;
            }
            if (s.type() == LoaiDiemDung.DROPOFF && s.status() == TrangThaiDiemDung.SKIPPED) {
                if (hasAnyTimingEvidence(s))
                    throw invalidStoredPlan();
                continue;
            }
            if (s.type() == LoaiDiemDung.PICKUP && s.status() == TrangThaiDiemDung.COMPLETED) {
                if (s.arrivedAt() == null || s.waitingStartedAt() == null || s.waitingDeadline() == null
                        || s.completedAt() == null
                        || !s.waitingStartedAt().equals(s.arrivedAt()) || s.waitingDeadline().isBefore(s.arrivedAt())
                        || s.completedAt().isBefore(s.arrivedAt()))
                    throw invalidStoredPlan();
                continue;
            }
            throw outsideCurrentScope();
        }
        if (!terminalAbort && snapshot.header().viewerRole() == TripViewerRole.DRIVER && arrivedPickups > 1)
            throw outsideCurrentScope();
    }

    private static void validateParticipantStopConsistency(TripDetailSnapshot snapshot, boolean terminalAbort) {
        for (var p : snapshot.participants()) {
            var pickup = snapshot.stops().stream().filter(s -> p.pickupStopId().equals(s.stopId())).findFirst()
                    .orElseThrow(TripDetailSnapshotValidator::invalidStoredPlan);
            var dropoff = snapshot.stops().stream().filter(s -> p.dropoffStopId().equals(s.stopId())).findFirst()
                    .orElseThrow(TripDetailSnapshotValidator::invalidStoredPlan);

            if (p.status() == TrangThaiYeuCau.ON_BOARD) {
                if (pickup.status() != TrangThaiDiemDung.COMPLETED || pickup.completedAt() == null
                        || !pickup.completedAt().equals(p.boardedAt()))
                    throw invalidStoredPlan();
            }
            if (p.status() == TrangThaiYeuCau.NO_SHOW) {
                if (pickup.status() != TrangThaiDiemDung.SKIPPED || pickup.completedAt() != null
                        || pickup.waitingDeadline() == null
                        || p.noShowAt().isBefore(pickup.waitingDeadline())
                        || dropoff.status() != TrangThaiDiemDung.SKIPPED
                        || dropoff.completedAt() != null)
                    throw invalidStoredPlan();
            }
            if (p.status() == TrangThaiYeuCau.ACCEPTED
                    && (pickup.status() == TrangThaiDiemDung.COMPLETED || pickup.status() == TrangThaiDiemDung.SKIPPED
                            || pickup.status() == TrangThaiDiemDung.CANCELLED
                            || dropoff.status() == TrangThaiDiemDung.CANCELLED))
                throw invalidStoredPlan();
            if (p.status() == TrangThaiYeuCau.ABORTED) {
                if (p.boardedAt() != null && pickup.status() != TrangThaiDiemDung.COMPLETED)
                    throw invalidStoredPlan();
                if (!terminalAbort && p.boardedAt() == null && pickup.status() != TrangThaiDiemDung.CANCELLED)
                    throw invalidStoredPlan();
                if (dropoff.status() != TrangThaiDiemDung.CANCELLED && dropoff.status() != TrangThaiDiemDung.SKIPPED)
                    throw invalidStoredPlan();
            }
        }
    }

    private static boolean hasAnyTimingEvidence(
            com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripDetailStopRow s) {
        return s.arrivedAt() != null || s.waitingStartedAt() != null || s.waitingDeadline() != null
                || s.completedAt() != null;
    }

    private static void validateCancelledBeforeStart(TripDetailSnapshot snapshot) {
        var h = snapshot.header();
        if (h.routeStatus() != TrangThaiLoTrinh.CANCELLED || h.startedAt() != null || h.endedAt() != null
                || h.cancelledAt() == null || h.cancelledAt().isBefore(h.formedAt()) || h.cancellationReason() == null
                || h.cancellationReason().isBlank() || h.cancellationReason().length() > 2000
                || h.actualPassengerCount() != 0
                || h.driverStartStatus() != TrangThaiDiemDung.CANCELLED || h.driverStartCompletedAt() != null)
            throw invalidStoredPlan();
        if (snapshot.participants().stream().anyMatch(p -> p.status() != TrangThaiYeuCau.CANCELLED_BY_DRIVER))
            throw invalidStoredPlan();
        if (snapshot.stops().stream()
                .anyMatch(s -> s.status() != TrangThaiDiemDung.CANCELLED || hasAnyTimingEvidence(s)))
            throw invalidStoredPlan();
    }

    private static void validateOrder(TripDetailSnapshot snapshot) {
        int previous = 0;
        Set<Integer> seen = new HashSet<>();
        for (var s : snapshot.stops()) {
            if (s.order() == null || s.order() <= previous || !seen.add(s.order()))
                throw invalidStoredPlan();
            previous = s.order();
        }
    }

    public static BusinessException outsideCurrentScope() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_DETAIL_OUTSIDE_CURRENT_SCOPE",
                "Trạng thái vận hành hiện tại của chuyến chưa thuộc phạm vi Trip Detail được hỗ trợ.");
    }

    public static BusinessException invalidStoredPlan() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_STORED_TRIP_PLAN",
                "Dữ liệu vận hành chuyến đi đang lưu không nhất quán.");
    }
}
