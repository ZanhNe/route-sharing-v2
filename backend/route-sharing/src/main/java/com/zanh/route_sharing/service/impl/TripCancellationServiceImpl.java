package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.trip.cancellation.CancelTripBeforeStartRequest;
import com.zanh.route_sharing.dto.trip.cancellation.CancelTripBeforeStartResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripcancellation.TripCancellationRepository;
import com.zanh.route_sharing.repository.sharedroute.tripcancellation.model.TripCancellationCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.tripcancellation.model.TripCancellationCommitResult;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.TripCancellationService;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.service.tripcancellation.TripCancellationResponseMapper;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class TripCancellationServiceImpl implements TripCancellationService {

    private final TripCancellationRepository repository;
    private final TripCancellationResponseMapper responseMapper;
    private final UserRealtimeEventPublisher realtimeEventPublisher;
    private final Clock clock;

    public TripCancellationServiceImpl(
            TripCancellationRepository repository,
            TripCancellationResponseMapper responseMapper,
            UserRealtimeEventPublisher realtimeEventPublisher,
            Clock clock) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.clock = clock;
    }

    @Override
    public CancelTripBeforeStartResponse cancelBeforeStart(
            Long actorId,
            Long tripId,
            CancelTripBeforeStartRequest request) {
        validateInput(actorId, tripId, request);
        String normalizedReason = request.reason().trim();
        Instant cancelledAt = TimePolicy.now(clock);

        TripCancellationCommitResult committed = repository.commit(new TripCancellationCommitCommand(
                actorId,
                tripId,
                normalizedReason,
                cancelledAt));
        publishAfterCommit(committed);
        return responseMapper.toResponse(committed);
    }

    private void publishAfterCommit(TripCancellationCommitResult committed) {
        for (int index = 0; index < committed.realtimeRecipientUserIds().size(); index++) {
            realtimeEventPublisher.publish(
                    committed.realtimeRecipientUserIds().get(index),
                    RealtimeNotificationEventFactory.tripCancelledBeforeStart(
                            committed.tripId(),
                            committed.routeId(),
                            committed.realtimeRideRequestIds().get(index),
                            committed.cancelledAt()));
        }
    }

    private static void validateInput(Long actorId, Long tripId, CancelTripBeforeStartRequest request) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0) {
            throw validation("tripId phải là số dương.");
        }
        if (request == null || request.reason() == null
                || request.reason().isBlank()
                || request.reason().trim().length() > 2000) {
            throw validation("Lý do hủy phải có từ 1 đến 2000 ký tự.");
        }
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
