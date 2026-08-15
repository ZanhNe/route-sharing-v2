package com.zanh.route_sharing.dto.trip.safety;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SafetyInvestigationContextResponse(
        SafetyIncidentCaseResponse.Incident incidentSnapshot,
        Trip trip,
        Route route,
        Vehicle vehicle,
        List<Participant> participants,
        List<Stop> stops,
        List<TripHistory> tripStateHistory,
        List<BookingHistory> bookingStateHistories,
        List<MonitoringHistory> monitoringHistory,
        List<SafetyIncidentCaseResponse.History> handlingHistory,
        List<SafetyIntervention> safetyInterventions,
        LocationEvidenceSummary locationEvidenceSummary,
        Instant readAt) {
    public record Position(BigDecimal latitude, BigDecimal longitude) {}
    public record Trip(Long tripId, String status, Instant startedAt, Instant endedAt) {}
    public record Route(List<Position> originalDriverRouteGeometry, List<Position> operationalTripRouteGeometry,
                        Position origin, Position destination) {}
    public record Vehicle(Long vehicleId, String plateNumber, String displayName) {}
    public record Participant(Long userId, String fullName, String role, Long bookingId, String currentParticipationState,
                              Instant acceptedAt, Instant boardedAt, Instant noShowAt, Instant droppedOffAt) {}
    public record Stop(Long stopId, Integer order, String type, String status, Long bookingId,
                       Position plannedCoordinate, Position actualCoordinate, Instant arrivedAt,
                       Instant waitingStartedAt, Instant waitingDeadline, Instant completedAt) {}
    public record TripHistory(Long sequence, String previousStatus, String resultingStatus, Long actorUserId,
                              Instant occurredAt, String reasonCode) {}
    public record BookingHistory(Long bookingId, Long sequence, String previousStatus, String resultingStatus,
                                 Long actorUserId, Instant occurredAt, String reasonCode) {}
    public record MonitoringHistory(Long sequence, String previousStatus, String resultingStatus,
                                    Instant transitionAt, Instant signalReferenceAt, String reasonCode) {}
    public record InterventionTarget(Long bookingId, Long userId, String fullName, String role) {}
    public record InterventionActor(Long userId, String fullName, String role) {}
    public record StopImpact(Long stopId, Long bookingId, String type, String previousStatus, String resultingStatus,
                             Instant previousWaitingDeadline, Instant resultingWaitingDeadline, Instant occurredAt) {}
    public record SafetyIntervention(Long interventionId, Long incidentId, Long tripId, String type, String status,
                                     InterventionTarget target, InterventionActor initiator, Instant startedAt,
                                     InterventionActor finisher, Instant endedAt, String participantSafeReason,
                                     Instant safeExitAt, Position safeExitPosition,
                                     Integer actualPassengerCountBefore, Integer actualPassengerCountAfter,
                                     Integer invalidatedBoardingCredentialCount, List<StopImpact> stopImpacts) {}
    public record LocationEvidenceSummary(boolean available, long totalElements) {}
}
