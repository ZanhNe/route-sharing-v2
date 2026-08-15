package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.trip.location.TripLocationRequest;
import com.zanh.route_sharing.dto.trip.location.TripLocationResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.triplocation.TripLocationRepository;
import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCommitOutcome;
import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCommitResult;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.TripLocationService;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.service.triplocation.TripLocationObserverAccess;
import com.zanh.route_sharing.service.triplocation.TripLocationResponseMapper;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
public class TripLocationServiceImpl implements TripLocationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TripLocationServiceImpl.class);
    private static final String LOCATION_DESTINATION_TEMPLATE = "/queue/trips/%d/location";

    private final TripLocationRepository repository;
    private final TripLocationResponseMapper responseMapper;
    private final GeometryFactory geometryFactory;
    private final Clock clock;
    private final TripLocationObserverAccess observerAccess;
    private final UserRealtimeEventPublisher realtimePublisher;

    public TripLocationServiceImpl(
            TripLocationRepository repository,
            TripLocationResponseMapper responseMapper,
            GeometryFactory geometryFactory,
            Clock clock,
            TripLocationObserverAccess observerAccess,
            UserRealtimeEventPublisher realtimePublisher) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.geometryFactory = geometryFactory;
        this.clock = clock;
        this.observerAccess = observerAccess;
        this.realtimePublisher = realtimePublisher;
    }

    @Override
    public TripLocationResponse submitLocation(Long actorId, Long tripId, TripLocationRequest request) {
        validateInput(actorId, tripId, request);
        Instant observedAt = TimePolicy.databasePrecision(request.observedAt());
        Instant receivedAt = TimePolicy.now(clock);
        Point position = point(request.position().latitude(), request.position().longitude());
        TripLocationCommitResult result = repository.record(new TripLocationCommitCommand(
                actorId,
                tripId,
                position,
                observedAt,
                receivedAt,
                request.accuracyMeters()));
        TripLocationResponse response = responseMapper.toResponse(result);
        publishCurrentLocationBestEffort(result);
        return response;
    }

    private void publishCurrentLocationBestEffort(TripLocationCommitResult result) {
        if (result.outcome() != TripLocationCommitOutcome.CURRENT_RECORDED
                || !result.currentLocationUpdated()
                || result.currentLocationFact() == null) {
            return;
        }
        var fact = result.currentLocationFact();
        try {
            var recipients = observerAccess.findEligiblePassengerUserIds(fact.tripId()).stream()
                    .filter(recipientUserId -> recipientUserId != null && recipientUserId > 0)
                    .distinct()
                    .toList();
            if (recipients.isEmpty()) {
                return;
            }
            var event = RealtimeNotificationEventFactory.tripLocationUpdated(
                    fact.tripId(),
                    fact.latitude(),
                    fact.longitude(),
                    fact.observedAt(),
                    fact.receivedAt(),
                    fact.accuracyMeters(),
                    fact.locationSequence());
            String destination = LOCATION_DESTINATION_TEMPLATE.formatted(fact.tripId());
            recipients.forEach(recipientUserId -> publishRecipientBestEffort(
                    recipientUserId, destination, event, fact.tripId(), fact.locationSequence()));
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Trip location realtime dispatch skipped after committed location: tripId={}, locationSequence={}",
                    fact.tripId(),
                    fact.locationSequence(),
                    exception);
        }
    }

    private void publishRecipientBestEffort(
            Long recipientUserId,
            String destination,
            com.zanh.route_sharing.service.realtime.model.RealtimeEventEnvelope<?> event,
            Long tripId,
            Long locationSequence) {
        try {
            realtimePublisher.publish(recipientUserId, destination, event);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Trip location realtime recipient dispatch failed: tripId={}, locationSequence={}, recipientUserId={}",
                    tripId,
                    locationSequence,
                    recipientUserId,
                    exception);
        }
    }

    private Point point(BigDecimal latitude, BigDecimal longitude) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude.doubleValue(), latitude.doubleValue()));
        point.setSRID(Wgs84Coordinates.SRID);
        return point;
    }

    private static void validateInput(Long actorId, Long tripId, TripLocationRequest request) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0) {
            throw validation("tripId phải là số dương.");
        }
        if (request == null || request.position() == null
                || !Wgs84Coordinates.isValid(request.position().latitude(), request.position().longitude())
                || request.observedAt() == null
                || (request.accuracyMeters() != null && request.accuracyMeters().signum() < 0)) {
            throw validation("Location observation không hợp lệ.");
        }
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
