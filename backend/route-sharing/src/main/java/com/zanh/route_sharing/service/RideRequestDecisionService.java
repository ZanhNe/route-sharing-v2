package com.zanh.route_sharing.service;

import com.zanh.route_sharing.service.riderequest.decision.model.RideRequestDecisionResult;

public interface RideRequestDecisionService {

    RideRequestDecisionResult accept(Long actorId, Long routeId, Long rideRequestId);

    RideRequestDecisionResult reject(Long actorId, Long routeId, Long rideRequestId);
}
