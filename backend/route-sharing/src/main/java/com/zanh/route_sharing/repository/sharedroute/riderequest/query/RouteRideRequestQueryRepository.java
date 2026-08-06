package com.zanh.route_sharing.repository.sharedroute.riderequest.query;

import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestDetailLookup;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestPageSnapshot;

import java.util.Optional;

public interface RouteRideRequestQueryRepository {

        Optional<PendingRideRequestPageSnapshot> findPendingPage(
                        Long actorUserId,
                        Long routeId,
                        int page,
                        int size);

        PendingRideRequestDetailLookup findPendingDetail(
                        Long actorUserId,
                        Long routeId,
                        Long rideRequestId);
}
