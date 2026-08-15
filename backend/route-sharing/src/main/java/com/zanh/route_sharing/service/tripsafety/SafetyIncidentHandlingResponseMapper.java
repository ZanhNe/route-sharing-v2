package com.zanh.route_sharing.service.tripsafety;

import com.zanh.route_sharing.dto.trip.safety.SafetyIncidentHandlingResponse;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.SafetyIncidentHandlingCommitResult;
import org.springframework.stereotype.Component;

@Component
public class SafetyIncidentHandlingResponseMapper {
    public SafetyIncidentHandlingResponse toResponse(SafetyIncidentHandlingCommitResult r) {
        return new SafetyIncidentHandlingResponse(r.incidentId(), r.tripId(), r.status(),
                r.primaryHandlerUserId() == null ? null
                        : new SafetyIncidentHandlingResponse.Handler(r.primaryHandlerUserId(),
                                r.primaryHandlerFullName()),
                r.acknowledgedAt(), r.resolvedAt(), r.safeConclusion(), r.changedAt());
    }
}
