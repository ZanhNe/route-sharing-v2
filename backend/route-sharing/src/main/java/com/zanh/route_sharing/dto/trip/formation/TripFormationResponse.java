package com.zanh.route_sharing.dto.trip.formation;

import com.zanh.route_sharing.dto.sharedroute.preview.GeoJsonLineStringResponse;
import java.time.Instant;
import java.util.List;

public record TripFormationResponse(
        String formationOutcome,
        Long routeId,
        String routeStatus,
        Instant lockedAt,
        Integer remainingSeats,
        Long tripId,
        String tripStatus,
        Instant formedAt,
        Integer plannedPassengerCount,
        Integer actualPassengerCount,
        GeoJsonLineStringResponse operationalRouteGeoJson,
        List<TripFormationStopResponse> stops) {

    public TripFormationResponse {
        stops = stops == null ? List.of() : List.copyOf(stops);
    }
}
