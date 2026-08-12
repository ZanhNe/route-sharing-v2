package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.boarding.TripBoardingRequest;
import com.zanh.route_sharing.dto.trip.boarding.TripBoardingResponse;

public interface TripBoardingService {
    TripBoardingResponse confirmBoarding(Long actorId, Long tripId, TripBoardingRequest request);
}
