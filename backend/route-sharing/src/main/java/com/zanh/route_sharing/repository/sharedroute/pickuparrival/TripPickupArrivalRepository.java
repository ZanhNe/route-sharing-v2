package com.zanh.route_sharing.repository.sharedroute.pickuparrival;

import com.zanh.route_sharing.repository.sharedroute.pickuparrival.model.TripPickupArrivalCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.pickuparrival.model.TripPickupArrivalCommitResult;

public interface TripPickupArrivalRepository {
    TripPickupArrivalCommitResult commit(TripPickupArrivalCommitCommand command);
}
