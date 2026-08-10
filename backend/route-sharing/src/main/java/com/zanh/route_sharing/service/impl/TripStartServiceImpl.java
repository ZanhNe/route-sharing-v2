package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.dto.trip.start.TripStartRequest;
import com.zanh.route_sharing.dto.trip.start.TripStartResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripstart.TripStartRepository;
import com.zanh.route_sharing.repository.sharedroute.tripstart.model.TripStartCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.tripstart.model.TripStartCommitResult;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.TripStartService;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.service.tripstart.TripStartResponseMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class TripStartServiceImpl implements TripStartService {

    private final TripStartRepository repository;
    private final TripStartResponseMapper responseMapper;
    private final UserRealtimeEventPublisher realtimeEventPublisher;
    private final GeometryFactory geometryFactory;
    private final Clock clock;

    public TripStartServiceImpl(
            TripStartRepository repository,
            TripStartResponseMapper responseMapper,
            UserRealtimeEventPublisher realtimeEventPublisher,
            GeometryFactory geometryFactory,
            Clock clock) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.geometryFactory = geometryFactory;
        this.clock = clock;
    }

    @Override
    public TripStartResponse startTrip(Long actorId, Long tripId, TripStartRequest request) {
        validateInput(actorId, tripId, request);
        Point currentLocation = point(request);
        Instant startedAt = TimePolicy.now(clock);
        TripStartCommitResult committed = repository.commit(new TripStartCommitCommand(
                actorId,
                tripId,
                currentLocation,
                startedAt));
        publishAfterCommit(committed);
        return responseMapper.toResponse(committed);
    }

    private void publishAfterCommit(TripStartCommitResult committed) {
        for (int index = 0; index < committed.realtimeRecipientUserIds().size(); index++) {
            realtimeEventPublisher.publish(
                    committed.realtimeRecipientUserIds().get(index),
                    RealtimeNotificationEventFactory.tripStarted(
                            committed.tripId(),
                            committed.routeId(),
                            committed.realtimeRideRequestIds().get(index),
                            committed.startedAt()));
        }
    }

    private Point point(TripStartRequest request) {
        Point point = geometryFactory.createPoint(new Coordinate(
                request.currentLocation().longitude().doubleValue(),
                request.currentLocation().latitude().doubleValue()));
        point.setSRID(Wgs84Coordinates.SRID);
        return point;
    }

    private static void validateInput(Long actorId, Long tripId, TripStartRequest request) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "tripId phải là số dương.");
        }
        if (request == null || request.currentLocation() == null
                || request.currentLocation().latitude() == null
                || request.currentLocation().longitude() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "currentLocation không hợp lệ.");
        }
    }
}
