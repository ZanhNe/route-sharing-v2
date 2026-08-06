package com.zanh.route_sharing.repository.sharedroute.riderequest;

import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCriteria;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluation;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPersistedView;

public interface RideRequestCreationRepository {

    RideRequestEvaluation evaluate(RideRequestCriteria criteria);

    RideRequestPersistedView commit(RideRequestCommitCommand command);
}
