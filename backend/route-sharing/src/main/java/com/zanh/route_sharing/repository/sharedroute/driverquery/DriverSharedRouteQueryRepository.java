package com.zanh.route_sharing.repository.sharedroute.driverquery;

import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRouteDetailRow;
import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRoutePageSnapshot;
import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRouteQueryCriteria;

import java.util.Optional;

public interface DriverSharedRouteQueryRepository {

    DriverSharedRoutePageSnapshot findPage(DriverSharedRouteQueryCriteria criteria);

    Optional<DriverSharedRouteDetailRow> findDetail(Long actorUserId, Long routeId);
}
