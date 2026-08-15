package com.zanh.route_sharing.service.triplocation;

import java.util.List;

public interface TripLocationObserverAccess {
    List<Long> findEligiblePassengerUserIds(Long tripId);

    boolean canSubscribe(Long actorUserId, Long tripId);
}
