package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.service.riderequest.model.RideRequestCreationResult;

public interface RideRequestCreationService {

    RideRequestCreationResult create(
            Long actorUserId,
            Long routeId,
            String idempotencyKey,
            CreateRideRequestRequest request);
}
