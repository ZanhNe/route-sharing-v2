package com.zanh.route_sharing.service.routing;

import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;

public interface RoutePlanner {

    RoutePlan plan(RoutePlanRequest request);
}
