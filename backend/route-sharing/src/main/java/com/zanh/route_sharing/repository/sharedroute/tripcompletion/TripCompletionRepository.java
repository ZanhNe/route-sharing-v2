package com.zanh.route_sharing.repository.sharedroute.tripcompletion;

import com.zanh.route_sharing.repository.sharedroute.tripcompletion.model.TripCompletionCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.tripcompletion.model.TripCompletionCommitResult;

public interface TripCompletionRepository {
    TripCompletionCommitResult commit(TripCompletionCommitCommand command);
}
