package com.zanh.route_sharing.service.tripsafety;

import com.zanh.route_sharing.dto.trip.safety.SafetyIncidentSummaryResponse;
import com.zanh.route_sharing.dto.trip.safety.TripSafetyIncidentResponse;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.SafetyIncidentSummarySnapshot;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyIncidentCommitResult;
import org.springframework.stereotype.Component;

@Component
public class TripSafetyIncidentResponseMapper {
    private final TripSafetyInterventionResponseMapper interventionMapper;

    public TripSafetyIncidentResponseMapper() {
        this(new TripSafetyInterventionResponseMapper());
    }

    public TripSafetyIncidentResponseMapper(TripSafetyInterventionResponseMapper interventionMapper) {
        this.interventionMapper = interventionMapper;
    }

    public TripSafetyIncidentResponse toReportResponse(TripSafetyIncidentCommitResult result) {
        return new TripSafetyIncidentResponse(
                result.incidentId(), result.tripId(), result.type().name(), result.severity().name(),
                result.status().name(), result.reportedAt(),
                result.intervention() == null ? null : interventionMapper.toResponse(result.intervention()));
    }

    public SafetyIncidentSummaryResponse toSummaryResponse(SafetyIncidentSummarySnapshot snapshot) {
        return new SafetyIncidentSummaryResponse(
                snapshot.incidentId(), snapshot.tripId(), snapshot.type().name(), snapshot.severity().name(),
                snapshot.status().name(), snapshot.reporterSource().name(), snapshot.reportedAt());
    }
}
