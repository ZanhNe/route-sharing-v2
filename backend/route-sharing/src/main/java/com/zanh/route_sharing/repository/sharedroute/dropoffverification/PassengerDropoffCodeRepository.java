package com.zanh.route_sharing.repository.sharedroute.dropoffverification;

import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.PassengerDropoffCodeCommand;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.PassengerDropoffCodeResult;

public interface PassengerDropoffCodeRepository {
    PassengerDropoffCodeResult getOrCreate(PassengerDropoffCodeCommand command);
}
