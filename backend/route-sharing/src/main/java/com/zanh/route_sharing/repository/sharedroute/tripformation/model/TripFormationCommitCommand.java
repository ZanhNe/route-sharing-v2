package com.zanh.route_sharing.repository.sharedroute.tripformation.model;

import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.tripformation.model.PlannedTripStop;

import java.time.Instant;
import java.util.List;

public record TripFormationCommitCommand(
        Long actorId,
        Long routeId,
        Instant formedAt,
        TripFormationPreparation preparation,
        List<PlannedTripStop> orderedStops,
        RoutePlan operationalRoutePlan) {

    public TripFormationCommitCommand {
        orderedStops = orderedStops == null ? List.of() : List.copyOf(orderedStops);
    }
}
