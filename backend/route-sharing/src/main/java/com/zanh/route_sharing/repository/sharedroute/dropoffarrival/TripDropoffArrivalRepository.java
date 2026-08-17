package com.zanh.route_sharing.repository.sharedroute.dropoffarrival;

import com.zanh.route_sharing.repository.sharedroute.dropoffarrival.model.TripDropoffArrivalCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.dropoffarrival.model.TripDropoffArrivalCommitResult;

public interface TripDropoffArrivalRepository {
    TripDropoffArrivalCommitResult commit(TripDropoffArrivalCommitCommand command);
}
