package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.sharedroute.cancellation.CancelSharedRouteResponse;

public interface SharedRouteCancellationService {
    CancelSharedRouteResponse cancelOwnedRoute(Long driverId, Long routeId, String reason);
}
