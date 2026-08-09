package com.zanh.route_sharing.service;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.dto.sharedroute.driverquery.DriverSharedRouteDetailResponse;
import com.zanh.route_sharing.service.sharedroute.driverquery.model.DriverSharedRoutePageResult;

public interface DriverSharedRouteQueryService {

    DriverSharedRoutePageResult listOwnRoutes(
            Long actorUserId,
            TrangThaiLoTrinh status,
            int page,
            int size);

    DriverSharedRouteDetailResponse getOwnRouteDetail(
            Long actorUserId,
            Long routeId);
}
