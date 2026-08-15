package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.safety.*;

public interface SafetyIncidentHandlingService {
    SafetyIncidentHandlingResponse claim(Long actorId, Long incidentId);

    SafetyIncidentHandlingResponse investigate(Long actorId, Long incidentId);

    SafetyIncidentHandlingResponse reassign(Long actorId, Long incidentId, SafetyIncidentReassignRequest request);

    SafetyIncidentHandlingResponse finalizeIncident(Long actorId, Long incidentId,
            SafetyIncidentFinalizeRequest request);
}
