package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.trip.completion.TripCompletionRequest;
import com.zanh.route_sharing.dto.trip.completion.TripCompletionResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripcompletion.TripCompletionRepository;
import com.zanh.route_sharing.repository.sharedroute.tripcompletion.model.TripCompletionCommitCommand;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.TripCompletionService;
import com.zanh.route_sharing.service.tripcompletion.TripCompletionResponseMapper;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TripCompletionServiceImpl implements TripCompletionService {
    private final TripCompletionRepository repository;
    private final TripCompletionResponseMapper responseMapper;
    private final GeometryFactory geometryFactory;

    public TripCompletionServiceImpl(
            TripCompletionRepository repository,
            TripCompletionResponseMapper responseMapper,
            GeometryFactory geometryFactory) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.geometryFactory = geometryFactory;
    }

    @Override
    public TripCompletionResponse completeTrip(Long actorId, Long tripId, TripCompletionRequest request) {
        validateInput(actorId, tripId, request);
        Point currentLocation = point(request);
        return responseMapper.toResponse(repository.commit(
                new TripCompletionCommitCommand(actorId, tripId, currentLocation)));
    }

    private Point point(TripCompletionRequest request) {
        Point point = geometryFactory.createPoint(new Coordinate(
                request.currentLocation().longitude().doubleValue(),
                request.currentLocation().latitude().doubleValue()));
        point.setSRID(Wgs84Coordinates.SRID);
        return point;
    }

    private static void validateInput(Long actorId, Long tripId, TripCompletionRequest request) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0) {
            throw validation("tripId phải là số dương.");
        }
        if (request == null || request.currentLocation() == null
                || request.currentLocation().latitude() == null
                || request.currentLocation().longitude() == null
                || !Wgs84Coordinates.isValid(
                        request.currentLocation().latitude(),
                        request.currentLocation().longitude())) {
            throw validation("currentLocation không hợp lệ.");
        }
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
