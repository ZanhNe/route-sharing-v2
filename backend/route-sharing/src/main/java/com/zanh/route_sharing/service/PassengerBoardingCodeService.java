package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.boarding.PassengerBoardingCodeResponse;

public interface PassengerBoardingCodeService {
    PassengerBoardingCodeResponse requestOwnCode(Long actorId, Long tripId);
}
