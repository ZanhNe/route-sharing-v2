package com.zanh.route_sharing.service;

import com.zanh.route_sharing.domain.enums.MucDoSuCo;
import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;
import com.zanh.route_sharing.dto.trip.safety.*;
import com.zanh.route_sharing.security.ClientRequestInfo;
import java.time.Instant;

public interface SafetyIncidentQueryService {
    SafetyIncidentSummaryResponse getSummary(Long actorId, Long incidentId);

    SafetyIncidentQueueResponse getQueue(Long actorId, Long schoolId, TrangThaiXuLySuCo status, MucDoSuCo severity,
            String ownership, int page, int size);

    SafetyIncidentCaseResponse getCase(Long actorId, Long incidentId, ClientRequestInfo client);

    SafetyInvestigationContextResponse getInvestigationContext(Long actorId, Long incidentId, ClientRequestInfo client);

    SafetyLocationEvidenceResponse getLocationEvidence(Long actorId, Long incidentId, Instant from, Instant to,
            int page, int size, ClientRequestInfo client);

    SafetyEligibleHandlersResponse getEligibleHandlers(Long actorId, Long incidentId, int page, int size);
}
