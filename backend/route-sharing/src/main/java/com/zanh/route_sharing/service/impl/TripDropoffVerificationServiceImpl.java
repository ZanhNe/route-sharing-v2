package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.trip.dropoffverification.TripDropoffVerificationRequest;
import com.zanh.route_sharing.dto.trip.dropoffverification.TripDropoffVerificationResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.TripDropoffVerificationRepository;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.TripDropoffVerificationCommand;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.TripDropoffVerificationCommitResult;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.TripDropoffVerificationService;
import com.zanh.route_sharing.service.dropoffverification.TripDropoffVerificationResponseMapper;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Clock;

@Service
public class TripDropoffVerificationServiceImpl implements TripDropoffVerificationService {
    private final TripDropoffVerificationRepository repository;
    private final TripDropoffVerificationResponseMapper responseMapper;
    private final UserRealtimeEventPublisher realtimePublisher;
    private final Clock clock;

    public TripDropoffVerificationServiceImpl(TripDropoffVerificationRepository repository,
            TripDropoffVerificationResponseMapper responseMapper,
            UserRealtimeEventPublisher realtimePublisher, Clock clock) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.realtimePublisher = realtimePublisher;
        this.clock = clock;
    }

    @Override
    public TripDropoffVerificationResponse verifyCurrentDropoff(Long actorId, Long tripId,
            TripDropoffVerificationRequest request) {
        validate(actorId, tripId, request);
        TripDropoffVerificationCommitResult committed = repository.commit(new TripDropoffVerificationCommand(
                actorId, tripId, request.dropoffCode(), TimePolicy.now(clock)));
        realtimePublisher.publish(committed.realtimeRecipientUserId(),
                RealtimeNotificationEventFactory.passengerDroppedOff(committed.tripId(), committed.routeId(),
                        committed.rideRequestId(),
                        committed.dropoffStopId(), committed.dropoffStopOrder(), committed.droppedOffAt()));
        return responseMapper.toResponse(committed);
    }

    private static void validate(Long actorId, Long tripId, TripDropoffVerificationRequest request) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0)
            throw validation("tripId phải là số dương.");
        if (request == null || request.dropoffCode() == null || !request.dropoffCode().matches("[0-9]{6}"))
            throw validation("dropoffCode phải gồm đúng 6 chữ số.");
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
