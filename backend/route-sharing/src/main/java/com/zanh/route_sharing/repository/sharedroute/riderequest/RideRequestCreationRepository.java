package com.zanh.route_sharing.repository.sharedroute.riderequest;

import com.zanh.route_sharing.repository.sharedroute.riderequest.model.IdempotencyRecord;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCommitResult;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCriteria;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluation;

import java.util.Optional;

public interface RideRequestCreationRepository {

    Optional<IdempotencyRecord> findReplay(Long actorUserId, String idempotencyKey);

    RideRequestEvaluation evaluate(RideRequestCriteria criteria);

    RideRequestCommitResult commit(RideRequestCommitCommand command);
}
