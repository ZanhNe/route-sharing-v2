package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.dropoffverification.TripDropoffVerificationRequest;
import com.zanh.route_sharing.dto.trip.dropoffverification.TripDropoffVerificationResponse;

public interface TripDropoffVerificationService {
    TripDropoffVerificationResponse verifyCurrentDropoff(Long actorId, Long tripId,
            TripDropoffVerificationRequest request);
}
