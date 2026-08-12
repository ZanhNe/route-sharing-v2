package com.zanh.route_sharing.repository.sharedroute.boarding;

import com.zanh.route_sharing.repository.sharedroute.boarding.model.PassengerBoardingCodeCommand;
import com.zanh.route_sharing.repository.sharedroute.boarding.model.PassengerBoardingCodeResult;

public interface PassengerBoardingCodeRepository {
    PassengerBoardingCodeResult getOrCreate(PassengerBoardingCodeCommand command);
}
