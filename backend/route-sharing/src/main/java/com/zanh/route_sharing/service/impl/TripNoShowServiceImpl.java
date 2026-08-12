package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.trip.noshow.TripNoShowResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.noshow.TripNoShowRepository;
import com.zanh.route_sharing.repository.sharedroute.noshow.model.TripNoShowCommand;
import com.zanh.route_sharing.repository.sharedroute.noshow.model.TripNoShowCommitResult;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.TripNoShowService;
import com.zanh.route_sharing.service.noshow.TripNoShowResponseMapper;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class TripNoShowServiceImpl implements TripNoShowService {
    private final TripNoShowRepository repository;
    private final TripNoShowResponseMapper responseMapper;
    private final UserRealtimeEventPublisher realtimePublisher;
    private final Clock clock;

    public TripNoShowServiceImpl(
            TripNoShowRepository repository,
            TripNoShowResponseMapper responseMapper,
            UserRealtimeEventPublisher realtimePublisher,
            Clock clock) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.realtimePublisher = realtimePublisher;
        this.clock = clock;
    }

    @Override
    public TripNoShowResponse confirmNoShow(Long actorId, Long tripId) {
        validate(actorId, tripId);
        TripNoShowCommitResult committed = repository.commit(
                new TripNoShowCommand(actorId, tripId, TimePolicy.now(clock)));
        realtimePublisher.publish(
                committed.realtimeRecipientUserId(),
                RealtimeNotificationEventFactory.passengerNoShow(
                        committed.tripId(), committed.routeId(), committed.rideRequestId(),
                        committed.pickupStopId(), committed.pickupStopOrder(),
                        committed.dropoffStopId(), committed.dropoffStopOrder(), committed.noShowAt()));
        return responseMapper.toResponse(committed);
    }

    private static void validate(Long actorId, Long tripId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "tripId phải là số dương.");
        }
    }
}
