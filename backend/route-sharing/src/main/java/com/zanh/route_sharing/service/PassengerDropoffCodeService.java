package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.dropoffverification.PassengerDropoffCodeResponse;

public interface PassengerDropoffCodeService {
    PassengerDropoffCodeResponse requestOwnCode(Long actorId, Long tripId);
}
