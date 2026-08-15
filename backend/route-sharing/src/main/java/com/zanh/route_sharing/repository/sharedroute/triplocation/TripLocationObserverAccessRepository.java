package com.zanh.route_sharing.repository.sharedroute.triplocation;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.util.List;
import java.util.Set;

public interface TripLocationObserverAccessRepository {
    List<Long> findEligiblePassengerUserIds(Long tripId, Set<TrangThaiYeuCau> activeStates);

    boolean isEligiblePassenger(Long actorUserId, Long tripId, Set<TrangThaiYeuCau> activeStates);
}
