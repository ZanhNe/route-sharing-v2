package com.zanh.route_sharing.dto.trip.safety;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SafetyIncidentCaseResponse(
        Incident incident,
        TripContext tripContext,
        Handling handling,
        List<SafetyIntervention> safetyInterventions,
        Instant readAt) {
    public record Position(BigDecimal latitude, BigDecimal longitude) {}
    public record Person(Long userId, String fullName, String role) {}
    public record Incident(Long incidentId, Long tripId, String type, String severity, String status,
                           String reporterSource, Instant reportedAt, String originalDescription,
                           Person reporter, Person reportedParticipant, OperationalSnapshot reportOperationalSnapshot) {}
    public record OperationalSnapshot(Position referenceCoordinate, Instant observedAt, Instant receivedAt,
                                      String monitoringStatus, Instant signalReferenceAt) {}
    public record TripContext(Long tripId, String status, Instant startedAt, Instant endedAt,
                              Person driver, Vehicle vehicle) {}
    public record Vehicle(Long vehicleId, String plateNumber, String displayName) {}
    public record Handling(Person primaryHandler, Instant acknowledgedAt, Instant resolvedAt,
                           String safeConclusion, List<History> history) {}
    public record History(Long sequence, String action, String previousStatus, String resultingStatus,
                          Person previousHandler, Person resultingHandler, Person actor, Instant occurredAt,
                          String reason, String safeConclusionSnapshot) {}
    public record SafetyIntervention(Long interventionId, String type, String status, Person target,
                                     Instant startedAt, Instant endedAt) {}
    public SafetyIncidentCaseResponse(Incident incident, TripContext tripContext, Handling handling, Instant readAt) {
        this(incident, tripContext, handling, List.of(), readAt);
    }
}
