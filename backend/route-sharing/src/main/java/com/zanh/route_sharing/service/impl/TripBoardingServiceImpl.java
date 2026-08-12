package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.trip.boarding.TripBoardingRequest;
import com.zanh.route_sharing.dto.trip.boarding.TripBoardingResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.boarding.TripBoardingRepository;
import com.zanh.route_sharing.repository.sharedroute.boarding.model.TripBoardingCommand;
import com.zanh.route_sharing.repository.sharedroute.boarding.model.TripBoardingCommitResult;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.TripBoardingService;
import com.zanh.route_sharing.service.boarding.TripBoardingResponseMapper;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class TripBoardingServiceImpl implements TripBoardingService {
    private final TripBoardingRepository repository;
    private final TripBoardingResponseMapper responseMapper;
    private final UserRealtimeEventPublisher realtimePublisher;
    private final Clock clock;

    public TripBoardingServiceImpl(
            TripBoardingRepository repository,
            TripBoardingResponseMapper responseMapper,
            UserRealtimeEventPublisher realtimePublisher,
            Clock clock) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.realtimePublisher = realtimePublisher;
        this.clock = clock;
    }

    @Override
    public TripBoardingResponse confirmBoarding(Long actorId, Long tripId, TripBoardingRequest request) {
        validate(actorId, tripId, request);
        TripBoardingCommitResult committed = repository.commit(new TripBoardingCommand(
                actorId, tripId, request.boardingCode(), TimePolicy.now(clock)));
        realtimePublisher.publish(
                committed.realtimeRecipientUserId(),
                RealtimeNotificationEventFactory.passengerBoarded(
                        committed.tripId(), committed.routeId(), committed.rideRequestId(),
                        committed.pickupStopId(), committed.pickupStopOrder(), committed.boardedAt()));
        return responseMapper.toResponse(committed);
    }

    private static void validate(Long actorId, Long tripId, TripBoardingRequest request) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0) {
            throw validation("tripId phải là số dương.");
        }
        if (request == null || request.boardingCode() == null || !request.boardingCode().matches("[0-9]{6}")) {
            throw validation("boardingCode phải gồm đúng 6 chữ số.");
        }
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
