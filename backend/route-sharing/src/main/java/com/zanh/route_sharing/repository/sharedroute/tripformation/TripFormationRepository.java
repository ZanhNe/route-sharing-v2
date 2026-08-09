package com.zanh.route_sharing.repository.sharedroute.tripformation;

import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationCommitResult;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationPreparation;

import java.util.Optional;

public interface TripFormationRepository {
    Optional<TripFormationPreparation> prepare(Long actorId, Long routeId);

    TripFormationCommitResult commit(TripFormationCommitCommand command);
}
