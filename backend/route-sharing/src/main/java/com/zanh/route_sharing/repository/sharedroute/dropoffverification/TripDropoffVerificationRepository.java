package com.zanh.route_sharing.repository.sharedroute.dropoffverification;

import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.TripDropoffVerificationCommand;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.TripDropoffVerificationCommitResult;

public interface TripDropoffVerificationRepository {
    TripDropoffVerificationCommitResult commit(TripDropoffVerificationCommand command);
}
