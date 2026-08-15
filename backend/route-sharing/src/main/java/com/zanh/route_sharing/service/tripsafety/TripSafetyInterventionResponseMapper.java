package com.zanh.route_sharing.service.tripsafety;

import com.zanh.route_sharing.dto.trip.safety.TripSafetyInterventionResponse;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyInterventionSnapshot;
import org.springframework.stereotype.Component;

@Component
public class TripSafetyInterventionResponseMapper {
    public TripSafetyInterventionResponse toResponse(TripSafetyInterventionSnapshot s) {
        TripSafetyInterventionResponse.Position position = s.safeExitLatitude() == null || s.safeExitLongitude() == null
                ? null
                : new TripSafetyInterventionResponse.Position(s.safeExitLatitude(), s.safeExitLongitude());
        return new TripSafetyInterventionResponse(s.interventionId(), s.incidentId(), s.tripId(), s.type(), s.status(),
                s.tripStatus(), s.targetRideRequestId(), s.targetBookingStatus(), s.actualPassengerCount(),
                s.safeExitAt(), position, s.changedAt());
    }
}
