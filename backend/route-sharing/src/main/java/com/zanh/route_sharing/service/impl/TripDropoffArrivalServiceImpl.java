package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.trip.dropoffarrival.TripDropoffArrivalRequest;
import com.zanh.route_sharing.dto.trip.dropoffarrival.TripDropoffArrivalResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.dropoffarrival.TripDropoffArrivalRepository;
import com.zanh.route_sharing.repository.sharedroute.dropoffarrival.model.TripDropoffArrivalCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.dropoffarrival.model.TripDropoffArrivalCommitResult;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.TripDropoffArrivalService;
import com.zanh.route_sharing.service.dropoffarrival.TripDropoffArrivalResponseMapper;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class TripDropoffArrivalServiceImpl implements TripDropoffArrivalService {
    private final TripDropoffArrivalRepository repository;
    private final TripDropoffArrivalResponseMapper responseMapper;
    private final UserRealtimeEventPublisher realtimeEventPublisher;
    private final GeometryFactory geometryFactory;
    private final Clock clock;

    public TripDropoffArrivalServiceImpl(TripDropoffArrivalRepository repository,
            TripDropoffArrivalResponseMapper responseMapper, UserRealtimeEventPublisher realtimeEventPublisher,
            GeometryFactory geometryFactory, Clock clock) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.geometryFactory = geometryFactory;
        this.clock = clock;
    }

    @Override
    public TripDropoffArrivalResponse confirmArrival(Long actorId, Long tripId, TripDropoffArrivalRequest request) {
        validateInput(actorId, tripId, request);
        Point currentLocation = point(request);
        Instant arrivedAt = TimePolicy.now(clock);
        TripDropoffArrivalCommitResult committed = repository.commit(
                new TripDropoffArrivalCommitCommand(actorId, tripId, currentLocation, arrivedAt));
        realtimeEventPublisher.publish(committed.realtimeRecipientUserId(),
                RealtimeNotificationEventFactory.driverArrivedDropoff(
                        committed.tripId(), committed.routeId(), committed.rideRequestId(),
                        committed.dropoffStopId(), committed.dropoffStopOrder(), committed.arrivedAt()));
        return responseMapper.toResponse(committed);
    }

    private Point point(TripDropoffArrivalRequest request) {
        Point point = geometryFactory.createPoint(new Coordinate(
                request.currentLocation().longitude().doubleValue(),
                request.currentLocation().latitude().doubleValue()));
        point.setSRID(Wgs84Coordinates.SRID);
        return point;
    }

    private static void validateInput(Long actorId, Long tripId, TripDropoffArrivalRequest request) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0)
            throw validation("tripId phải là số dương.");
        if (request == null || request.currentLocation() == null
                || request.currentLocation().latitude() == null || request.currentLocation().longitude() == null) {
            throw validation("currentLocation không hợp lệ.");
        }
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
