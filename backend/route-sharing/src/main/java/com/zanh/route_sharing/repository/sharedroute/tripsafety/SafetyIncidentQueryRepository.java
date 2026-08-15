package com.zanh.route_sharing.repository.sharedroute.tripsafety;

import com.zanh.route_sharing.domain.enums.MucDoSuCo;
import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.SafetyIncidentQuerySnapshots;
import com.zanh.route_sharing.security.ClientRequestInfo;
import java.time.Instant;
import java.time.LocalDate;

public interface SafetyIncidentQueryRepository {
    SafetyIncidentQuerySnapshots.Queue findQueue(Long actorId, Long schoolId, TrangThaiXuLySuCo status, MucDoSuCo severity,
                                                  String ownership, int page, int size, LocalDate businessDate);
    SafetyIncidentQuerySnapshots.Case findCase(Long actorId, Long incidentId, Instant readAt, LocalDate businessDate, ClientRequestInfo client);
    SafetyIncidentQuerySnapshots.Investigation findInvestigationContext(Long actorId, Long incidentId, Instant readAt,
                                                                         LocalDate businessDate, ClientRequestInfo client);
    SafetyIncidentQuerySnapshots.LocationPage findLocationEvidence(Long actorId, Long incidentId, Instant from, Instant to,
                                                                    int page, int size, Instant readAt, LocalDate businessDate,
                                                                    ClientRequestInfo client);
    SafetyIncidentQuerySnapshots.Eligible findEligibleHandlers(Long actorId, Long incidentId, int page, int size,
                                                                LocalDate businessDate);
    SafetyIncidentQuerySnapshots.ReporterStatus findReporterStatus(Long actorId, Long tripId, Long incidentId);
}
