package com.zanh.route_sharing.service.tripsafety;

import com.zanh.route_sharing.dto.trip.safety.*;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.SafetyIncidentQuerySnapshots;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SafetyIncidentQueryResponseMapper {
        public SafetyIncidentQueueResponse toQueue(SafetyIncidentQuerySnapshots.Queue q, int page, int size) {
                return new SafetyIncidentQueueResponse(q.items().stream().map(i -> new SafetyIncidentQueueResponse.Item(
                                i.incidentId(), i.tripId(),
                                new SafetyIncidentQueueResponse.School(i.school().schoolId(), i.school().schoolName()),
                                i.type(), i.severity(), i.status(), i.reporterSource(), i.reportedAt(),
                                i.primaryHandler() == null ? null
                                                : new SafetyIncidentQueueResponse.Handler(i.primaryHandler().userId(),
                                                                i.primaryHandler().fullName()),
                                i.acknowledgedAt(), i.resolvedAt())).toList(),
                                SafetyPageMeta.of(page, size, q.total()));
        }

        public SafetyEligibleHandlersResponse toEligible(SafetyIncidentQuerySnapshots.Eligible e, int page, int size) {
                return new SafetyEligibleHandlersResponse(
                                e.items().stream()
                                                .map(i -> new SafetyEligibleHandlersResponse.Item(i.userId(),
                                                                i.fullName(), i.currentHandler()))
                                                .toList(),
                                SafetyPageMeta.of(page, size, e.total()));
        }

        public SafetyIncidentCaseResponse toCase(SafetyIncidentQuerySnapshots.Case c) {
                return new SafetyIncidentCaseResponse(toIncident(c.incident()), toTripContext(c.tripContext()),
                                toHandling(c.handling()),
                                c.safetyInterventions().stream().map(this::toCompactIntervention).toList(), c.readAt());
        }

        public SafetyInvestigationContextResponse toInvestigation(SafetyIncidentQuerySnapshots.Investigation x) {
                return new SafetyInvestigationContextResponse(
                                toIncident(x.incidentSnapshot()),
                                new SafetyInvestigationContextResponse.Trip(x.trip().tripId(), x.trip().status(),
                                                x.trip().startedAt(), x.trip().endedAt()),
                                new SafetyInvestigationContextResponse.Route(
                                                toInvestigationPositions(x.route().originalDriverRouteGeometry()),
                                                toInvestigationPositions(x.route().operationalTripRouteGeometry()),
                                                toInvestigationPosition(x.route().origin()),
                                                toInvestigationPosition(x.route().destination())),
                                new SafetyInvestigationContextResponse.Vehicle(x.vehicle().vehicleId(),
                                                x.vehicle().plateNumber(), x.vehicle().displayName()),
                                x.participants().stream()
                                                .map(p -> new SafetyInvestigationContextResponse.Participant(p.userId(),
                                                                p.fullName(), p.role(), p.bookingId(),
                                                                p.currentParticipationState(), p.acceptedAt(),
                                                                p.boardedAt(), p.noShowAt(), p.droppedOffAt()))
                                                .toList(),
                                x.stops().stream()
                                                .map(s -> new SafetyInvestigationContextResponse.Stop(s.stopId(),
                                                                s.order(), s.type(), s.status(), s.bookingId(),
                                                                toInvestigationPosition(s.plannedCoordinate()),
                                                                toInvestigationPosition(s.actualCoordinate()),
                                                                s.arrivedAt(), s.waitingStartedAt(),
                                                                s.waitingDeadline(), s.completedAt()))
                                                .toList(),
                                x.tripStateHistory().stream()
                                                .map(h -> new SafetyInvestigationContextResponse.TripHistory(
                                                                h.sequence(), h.previousStatus(), h.resultingStatus(),
                                                                h.actorUserId(), h.occurredAt(), h.reasonCode()))
                                                .toList(),
                                x.bookingStateHistories().stream()
                                                .map(h -> new SafetyInvestigationContextResponse.BookingHistory(
                                                                h.bookingId(), h.sequence(), h.previousStatus(),
                                                                h.resultingStatus(), h.actorUserId(), h.occurredAt(),
                                                                h.reasonCode()))
                                                .toList(),
                                x.monitoringHistory().stream()
                                                .map(h -> new SafetyInvestigationContextResponse.MonitoringHistory(
                                                                h.sequence(), h.previousStatus(), h.resultingStatus(),
                                                                h.transitionAt(), h.signalReferenceAt(),
                                                                h.reasonCode()))
                                                .toList(),
                                x.handlingHistory().stream().map(this::toHistory).toList(),
                                x.safetyInterventions().stream().map(this::toInvestigationIntervention).toList(),
                                new SafetyInvestigationContextResponse.LocationEvidenceSummary(
                                                x.locationEvidenceSummary().available(),
                                                x.locationEvidenceSummary().totalElements()),
                                x.readAt());
        }

        public SafetyLocationEvidenceResponse toLocations(SafetyIncidentQuerySnapshots.LocationPage p, int page,
                        int size) {
                return new SafetyLocationEvidenceResponse(
                                p.items().stream()
                                                .map(i -> new SafetyLocationEvidenceResponse.Item(i.locationSequence(),
                                                                i.latitude(), i.longitude(),
                                                                i.observedAt(), i.receivedAt(), i.effectiveObservedAt(),
                                                                i.accuracyMeters(), i.speedMetersPerSecond(),
                                                                i.headingDegrees(), i.source()))
                                                .toList(),
                                SafetyPageMeta.of(page, size, p.total()));
        }

        public ReporterSafetyIncidentStatusResponse toReporterStatus(SafetyIncidentQuerySnapshots.ReporterStatus s) {
                var i = s.intervention();
                return new ReporterSafetyIncidentStatusResponse(s.incidentId(), s.tripId(), s.type(), s.severity(),
                                s.reportedAt(), s.status(), s.acknowledgedAt(), s.resolvedAt(), s.safeConclusion(),
                                i == null ? null
                                                : new ReporterSafetyIncidentStatusResponse.Intervention(
                                                                i.interventionId(), i.type(), i.status(),
                                                                i.tripStatus(), i.ownBookingStatus(), i.changedAt()));
        }

        private SafetyIncidentCaseResponse.SafetyIntervention toCompactIntervention(
                        SafetyIncidentQuerySnapshots.CompactIntervention x) {
                return new SafetyIncidentCaseResponse.SafetyIntervention(x.interventionId(), x.type(), x.status(),
                                toCasePerson(x.target()), x.startedAt(), x.endedAt());
        }

        private SafetyInvestigationContextResponse.SafetyIntervention toInvestigationIntervention(
                        SafetyIncidentQuerySnapshots.Intervention x) {
                return new SafetyInvestigationContextResponse.SafetyIntervention(
                                x.interventionId(), x.incidentId(), x.tripId(), x.type(), x.status(),
                                x.target() == null ? null
                                                : new SafetyInvestigationContextResponse.InterventionTarget(
                                                                x.target().bookingId(), x.target().userId(),
                                                                x.target().fullName(), x.target().role()),
                                new SafetyInvestigationContextResponse.InterventionActor(x.initiator().userId(),
                                                x.initiator().fullName(), x.initiator().role()),
                                x.startedAt(),
                                x.finisher() == null ? null
                                                : new SafetyInvestigationContextResponse.InterventionActor(
                                                                x.finisher().userId(), x.finisher().fullName(),
                                                                x.finisher().role()),
                                x.endedAt(), x.participantSafeReason(), x.safeExitAt(),
                                toInvestigationPosition(x.safeExitPosition()),
                                x.actualPassengerCountBefore(), x.actualPassengerCountAfter(),
                                x.invalidatedBoardingCredentialCount(),
                                x.stopImpacts().stream()
                                                .map(d -> new SafetyInvestigationContextResponse.StopImpact(d.stopId(),
                                                                d.bookingId(), d.type(), d.previousStatus(),
                                                                d.resultingStatus(), d.previousWaitingDeadline(),
                                                                d.resultingWaitingDeadline(), d.occurredAt()))
                                                .toList());
        }

        private SafetyIncidentCaseResponse.Incident toIncident(SafetyIncidentQuerySnapshots.Incident i) {
                return new SafetyIncidentCaseResponse.Incident(i.incidentId(), i.tripId(), i.type(), i.severity(),
                                i.status(), i.reporterSource(), i.reportedAt(), i.originalDescription(),
                                toCasePerson(i.reporter()), toCasePerson(i.reportedParticipant()),
                                new SafetyIncidentCaseResponse.OperationalSnapshot(
                                                toCasePosition(i.reportOperationalSnapshot().referenceCoordinate()),
                                                i.reportOperationalSnapshot().observedAt(),
                                                i.reportOperationalSnapshot().receivedAt(),
                                                i.reportOperationalSnapshot().monitoringStatus(),
                                                i.reportOperationalSnapshot().signalReferenceAt()));
        }

        private SafetyIncidentCaseResponse.TripContext toTripContext(SafetyIncidentQuerySnapshots.TripContext t) {
                return new SafetyIncidentCaseResponse.TripContext(t.tripId(), t.status(), t.startedAt(), t.endedAt(),
                                toCasePerson(t.driver()),
                                new SafetyIncidentCaseResponse.Vehicle(t.vehicle().vehicleId(),
                                                t.vehicle().plateNumber(), t.vehicle().displayName()));
        }

        private SafetyIncidentCaseResponse.Handling toHandling(SafetyIncidentQuerySnapshots.Handling h) {
                return new SafetyIncidentCaseResponse.Handling(toCasePerson(h.primaryHandler()), h.acknowledgedAt(),
                                h.resolvedAt(), h.safeConclusion(), h.history().stream().map(this::toHistory).toList());
        }

        private SafetyIncidentCaseResponse.History toHistory(SafetyIncidentQuerySnapshots.History h) {
                return new SafetyIncidentCaseResponse.History(h.sequence(), h.action(), h.previousStatus(),
                                h.resultingStatus(), toCasePerson(h.previousHandler()),
                                toCasePerson(h.resultingHandler()), toCasePerson(h.actor()), h.occurredAt(), h.reason(),
                                h.safeConclusionSnapshot());
        }

        private SafetyIncidentCaseResponse.Person toCasePerson(SafetyIncidentQuerySnapshots.Person p) {
                return p == null ? null : new SafetyIncidentCaseResponse.Person(p.userId(), p.fullName(), p.role());
        }

        private SafetyIncidentCaseResponse.Position toCasePosition(SafetyIncidentQuerySnapshots.Position p) {
                return p == null ? null : new SafetyIncidentCaseResponse.Position(p.latitude(), p.longitude());
        }

        private SafetyInvestigationContextResponse.Position toInvestigationPosition(
                        SafetyIncidentQuerySnapshots.Position p) {
                return p == null ? null : new SafetyInvestigationContextResponse.Position(p.latitude(), p.longitude());
        }

        private List<SafetyInvestigationContextResponse.Position> toInvestigationPositions(
                        List<SafetyIncidentQuerySnapshots.Position> list) {
                return list == null ? List.of() : list.stream().map(this::toInvestigationPosition).toList();
        }
}
