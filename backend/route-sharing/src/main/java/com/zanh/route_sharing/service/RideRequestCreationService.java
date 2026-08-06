package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.dto.riderequest.RideRequestResponse;

public interface RideRequestCreationService {

    RideRequestResponse create(
            Long actorUserId,
            Long routeId,
            CreateRideRequestRequest request);
}
