package com.zanh.route_sharing.repository.sharedroute.noshow;

import com.zanh.route_sharing.repository.sharedroute.noshow.model.TripNoShowCommand;
import com.zanh.route_sharing.repository.sharedroute.noshow.model.TripNoShowCommitResult;

public interface TripNoShowRepository {
    TripNoShowCommitResult commit(TripNoShowCommand command);
}
