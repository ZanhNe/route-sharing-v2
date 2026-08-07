package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.riderequest.decision.RideRequestDecisionResponse;

public interface RideRequestDecisionService {

    RideRequestDecisionResponse accept(Long actorId, Long routeId, Long rideRequestId);

    RideRequestDecisionResponse reject(Long actorId, Long routeId, Long rideRequestId);
}
