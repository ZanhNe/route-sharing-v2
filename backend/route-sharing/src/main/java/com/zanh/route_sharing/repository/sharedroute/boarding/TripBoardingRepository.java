package com.zanh.route_sharing.repository.sharedroute.boarding;

import com.zanh.route_sharing.repository.sharedroute.boarding.model.TripBoardingCommand;
import com.zanh.route_sharing.repository.sharedroute.boarding.model.TripBoardingCommitResult;

public interface TripBoardingRepository {
    TripBoardingCommitResult commit(TripBoardingCommand command);
}
