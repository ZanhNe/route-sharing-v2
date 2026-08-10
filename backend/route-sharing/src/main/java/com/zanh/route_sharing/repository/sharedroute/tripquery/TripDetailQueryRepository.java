package com.zanh.route_sharing.repository.sharedroute.tripquery;

import com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripDetailSnapshot;

import java.util.Optional;

public interface TripDetailQueryRepository {
    Optional<TripDetailSnapshot> findDetail(Long actorUserId, Long tripId);
}
