package com.zanh.route_sharing.service.sharedroute;

import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.service.routing.model.RoutePlan;

public interface SharedRoutePublicationCommitter {
    LoTrinhChiaSe commit(
            Long actorUserId,
            CreateSharedRouteRequest request,
            RoutePlan routePlan);
}
