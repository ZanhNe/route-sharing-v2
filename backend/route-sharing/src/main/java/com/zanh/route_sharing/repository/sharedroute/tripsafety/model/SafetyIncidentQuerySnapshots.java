package com.zanh.route_sharing.repository.sharedroute.tripsafety.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class SafetyIncidentQuerySnapshots {
    private SafetyIncidentQuerySnapshots() {}

    public record Person(Long userId, String fullName, String role) {}
    public record Position(BigDecimal latitude, BigDecimal longitude) {}
    public record Handler(Long userId, String fullName) {}
    public record School(Long schoolId, String schoolName) {}

    public record QueueItem(Long incidentId, Long tripId, School school, String type, String severity, String status,
                            String reporterSource, Instant reportedAt, Handler primaryHandler,
                            Instant acknowledgedAt, Instant resolvedAt) {}
    public record Queue(List<QueueItem> items, long total) {}
    public record EligibleItem(Long userId, String fullName, boolean currentHandler) {}
    public record Eligible(List<EligibleItem> items, long total) {}

    public record OperationalSnapshot(Position referenceCoordinate, Instant observedAt, Instant receivedAt,
                                      String monitoringStatus, Instant signalReferenceAt) {}
    public record History(Long sequence, String action, String previousStatus, String resultingStatus,
                          Person previousHandler, Person resultingHandler, Person actor, Instant occurredAt,
                          String reason, String safeConclusionSnapshot) {}
    public record Incident(Long incidentId, Long tripId, String type, String severity, String status,
                           String reporterSource, Instant reportedAt, String originalDescription,
                           Person reporter, Person reportedParticipant, OperationalSnapshot reportOperationalSnapshot) {}
    public record Vehicle(Long vehicleId, String plateNumber, String displayName) {}
    public record TripContext(Long tripId, String status, Instant startedAt, Instant endedAt, Person driver, Vehicle vehicle) {}
    public record Handling(Person primaryHandler, Instant acknowledgedAt, Instant resolvedAt,
                           String safeConclusion, List<History> history) {}
    public record CompactIntervention(Long interventionId, String type, String status, Person target, Instant startedAt, Instant endedAt) {}
    public record Case(Incident incident, TripContext tripContext, Handling handling, List<CompactIntervention> safetyInterventions, Instant readAt) {}

    public record Trip(Long tripId, String status, Instant startedAt, Instant endedAt) {}
    public record Route(List<Position> originalDriverRouteGeometry, List<Position> operationalTripRouteGeometry,
                        Position origin, Position destination) {}
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
    public record Intervention(Long interventionId, Long incidentId, Long tripId, String type, String status,
                               InterventionTarget target, InterventionActor initiator, Instant startedAt,
                               InterventionActor finisher, Instant endedAt, String participantSafeReason,
                               Instant safeExitAt, Position safeExitPosition, Integer actualPassengerCountBefore,
                               Integer actualPassengerCountAfter, Integer invalidatedBoardingCredentialCount,
                               List<StopImpact> stopImpacts) {}
    public record LocationEvidenceSummary(boolean available, long totalElements) {}
    public record Investigation(Incident incidentSnapshot, Trip trip, Route route, Vehicle vehicle,
                                List<Participant> participants, List<Stop> stops,
                                List<TripHistory> tripStateHistory, List<BookingHistory> bookingStateHistories,
                                List<MonitoringHistory> monitoringHistory, List<History> handlingHistory,
                                List<Intervention> safetyInterventions, LocationEvidenceSummary locationEvidenceSummary, Instant readAt) {}

    public record LocationItem(Long locationSequence, BigDecimal latitude, BigDecimal longitude,
                               Instant observedAt, Instant receivedAt, Instant effectiveObservedAt,
                               BigDecimal accuracyMeters, BigDecimal speedMetersPerSecond,
                               BigDecimal headingDegrees, String source) {}
    public record LocationPage(List<LocationItem> items, long total) {}

    public record ReporterIntervention(Long interventionId, String type, String status, String tripStatus,
                                       String ownBookingStatus, Instant changedAt) {}
    public record ReporterStatus(Long incidentId, Long tripId, String type, String severity, Instant reportedAt,
                                 String status, Instant acknowledgedAt, Instant resolvedAt, String safeConclusion,
                                 ReporterIntervention intervention) {
        public ReporterStatus(Long incidentId, Long tripId, String type, String severity, Instant reportedAt,
                              String status, Instant acknowledgedAt, Instant resolvedAt, String safeConclusion) {
            this(incidentId, tripId, type, severity, reportedAt, status, acknowledgedAt, resolvedAt, safeConclusion, null);
        }
    }
}
