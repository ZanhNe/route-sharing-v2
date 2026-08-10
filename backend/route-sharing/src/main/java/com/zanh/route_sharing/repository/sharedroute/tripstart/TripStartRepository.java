package com.zanh.route_sharing.repository.sharedroute.tripstart;

import com.zanh.route_sharing.repository.sharedroute.tripstart.model.TripStartCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.tripstart.model.TripStartCommitResult;

public interface TripStartRepository {
    TripStartCommitResult commit(TripStartCommitCommand command);
}
