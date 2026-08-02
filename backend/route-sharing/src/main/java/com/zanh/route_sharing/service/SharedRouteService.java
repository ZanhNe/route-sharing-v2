package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.SharedRouteResponse;

public interface SharedRouteService {

    SharedRouteResponse createSharedRoute(
            Long actorUserId,
            CreateSharedRouteRequest request);
}