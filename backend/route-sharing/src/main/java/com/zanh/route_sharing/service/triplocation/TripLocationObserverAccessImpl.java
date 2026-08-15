package com.zanh.route_sharing.service.triplocation;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.repository.sharedroute.triplocation.TripLocationObserverAccessRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TripLocationObserverAccessImpl implements TripLocationObserverAccess {

    private final TripLocationObserverAccessRepository repository;

    public TripLocationObserverAccessImpl(TripLocationObserverAccessRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Long> findEligiblePassengerUserIds(Long tripId) {
        return repository.findEligiblePassengerUserIds(tripId, TrangThaiYeuCau.activeTripParticipantStates());
    }

    @Override
    public boolean canSubscribe(Long actorUserId, Long tripId) {
        return repository.isEligiblePassenger(actorUserId, tripId, TrangThaiYeuCau.activeTripParticipantStates());
    }
}
