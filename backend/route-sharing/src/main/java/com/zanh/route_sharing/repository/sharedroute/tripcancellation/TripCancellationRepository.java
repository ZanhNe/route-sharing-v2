package com.zanh.route_sharing.repository.sharedroute.tripcancellation;

import com.zanh.route_sharing.repository.sharedroute.tripcancellation.model.TripCancellationCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.tripcancellation.model.TripCancellationCommitResult;

public interface TripCancellationRepository {
    TripCancellationCommitResult commit(TripCancellationCommitCommand command);
}
