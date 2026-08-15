package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.trip.safety.TripSafetyInterventionResponse;
import com.zanh.route_sharing.dto.trip.safety.TripSafetySafeExitRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.TripSafetyInterventionRepository;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyInterventionCommitResult;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.TripSafetyInterventionService;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.service.tripsafety.TripSafetyInterventionResponseMapper;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class TripSafetyInterventionServiceImpl implements TripSafetyInterventionService {
    private final TripSafetyInterventionRepository repository;
    private final TripSafetyInterventionResponseMapper mapper;
    private final UserRealtimeEventPublisher realtimePublisher;
    private final GeometryFactory geometryFactory;
    private final Clock clock;

    public TripSafetyInterventionServiceImpl(TripSafetyInterventionRepository repository,
            TripSafetyInterventionResponseMapper mapper,
            UserRealtimeEventPublisher realtimePublisher,
            GeometryFactory geometryFactory,
            Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.realtimePublisher = realtimePublisher;
        this.geometryFactory = geometryFactory;
        this.clock = clock;
    }

    @Override
    public TripSafetyInterventionResponse confirmSafeExit(Long actorId, Long tripId, Long interventionId,
            TripSafetySafeExitRequest request) {
        validateIds(actorId, tripId, interventionId);
        if (request == null || request.location() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SAFETY_SAFE_EXIT_LOCATION_REQUIRED",
                    "location không được trống.");
        }
        if (!Wgs84Coordinates.isValid(request.location().latitude(), request.location().longitude())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SAFETY_SAFE_EXIT_INVALID_LOCATION",
                    "Safe-exit latitude/longitude không hợp lệ.");
        }
        Point p = geometryFactory.createPoint(new Coordinate(request.location().longitude().doubleValue(),
                request.location().latitude().doubleValue()));
        p.setSRID(Wgs84Coordinates.SRID);
        var result = repository.confirmSafeExit(actorId, tripId, interventionId, p, TimePolicy.now(clock));
        publish(result);
        return mapper.toResponse(result.snapshot());
    }

    @Override
    public TripSafetyInterventionResponse abortTripFromHold(Long actorId, Long tripId, Long interventionId) {
        validateIds(actorId, tripId, interventionId);
        var result = repository.abortTripFromHold(actorId, tripId, interventionId, TimePolicy.now(clock));
        publish(result);
        return mapper.toResponse(result.snapshot());
    }

    @Override
    public TripSafetyInterventionResponse abortTripBySafety(Long actorId, Long incidentId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (incidentId == null || incidentId <= 0)
            throw validation();
        Instant now = TimePolicy.now(clock);
        var result = repository.abortTripBySafety(actorId, incidentId, now,
                LocalDate.ofInstant(now, TimePolicy.BUSINESS_ZONE));
        publish(result);
        return mapper.toResponse(result.snapshot());
    }

    private void publish(TripSafetyInterventionCommitResult r) {
        if (!r.changed())
            return;
        var s = r.snapshot();
        if (!r.tripStateRealtimeRecipientUserIds().isEmpty()) {
            var event = RealtimeNotificationEventFactory.tripSafetyStateChanged(s.tripId(), s.interventionId(),
                    s.changeType(), s.tripStatus(), s.changedAt());
            r.tripStateRealtimeRecipientUserIds().forEach(id -> realtimePublisher.publish(id, event));
        }
        if (!r.participantRealtimeRecipientUserIds().isEmpty() && s.targetRideRequestId() != null) {
            var event = RealtimeNotificationEventFactory.tripParticipantSafetyChanged(s.tripId(), s.interventionId(),
                    s.targetRideRequestId(), s.targetBookingStatus(), s.changedAt());
            r.participantRealtimeRecipientUserIds().forEach(id -> realtimePublisher.publish(id, event));
        }
    }

    private static void validateIds(Long actorId, Long tripId, Long interventionId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0 || interventionId == null || interventionId <= 0)
            throw validation();
    }

    private static BusinessException validation() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id không hợp lệ.");
    }
}
