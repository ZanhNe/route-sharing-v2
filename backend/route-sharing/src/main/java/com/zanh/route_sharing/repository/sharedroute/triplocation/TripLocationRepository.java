package com.zanh.route_sharing.repository.sharedroute.triplocation;

import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCommitResult;

public interface TripLocationRepository {
    TripLocationCommitResult record(TripLocationCommitCommand command);
}
