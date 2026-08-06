package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestDetailResponse;
import com.zanh.route_sharing.service.riderequest.query.model.RouteRideRequestPageResult;

public interface RouteRideRequestQueryService {

    RouteRideRequestPageResult listPending(
            Long actorUserId,
            Long routeId,
            int page,
            int size);

    RouteRideRequestDetailResponse getPendingDetail(
            Long actorUserId,
            Long routeId,
            Long rideRequestId);
}
