package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewPointRequest;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.preview.SharedRoutePreviewResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewEvaluation;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewEvaluationStatus;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewGeoPoint;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewCriteria;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewPreparation;
import com.zanh.route_sharing.repository.sharedroute.preview.SharedRoutePreviewRepository;
import com.zanh.route_sharing.service.SharedRoutePreviewService;
import com.zanh.route_sharing.service.preview.PreviewResponseMapper;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.RoutePlanner;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class SharedRoutePreviewServiceImpl implements SharedRoutePreviewService {

    private final SharedRoutePreviewRepository repository;
    private final RoutePlanner routePlanner;
    private final PreviewResponseMapper responseMapper;
    private final Clock clock;

    public SharedRoutePreviewServiceImpl(
            SharedRoutePreviewRepository repository,
            RoutePlanner routePlanner,
            PreviewResponseMapper responseMapper,
            Clock clock) {
        this.repository = repository;
        this.routePlanner = routePlanner;
        this.responseMapper = responseMapper;
        this.clock = clock;
    }

    @Override
    public SharedRoutePreviewResponse preview(
            Long actorUserId,
            Long sharedRouteId,
            PreviewSharedRouteRequest request) {
        AuthenticatedPrincipalValidator.requireUserId(actorUserId);
        requireRouteId(sharedRouteId);
        requireRequest(request);
        requireDistinctEndpoints(request.pickup(), request.passengerDestination());

        Instant now = TimePolicy.now(clock);
        SharedRoutePreviewCriteria criteria = new SharedRoutePreviewCriteria(
                actorUserId,
                request.schoolId(),
                sharedRouteId,
                request.pickup().latitude(),
                request.pickup().longitude(),
                request.passengerDestination().latitude(),
                request.passengerDestination().longitude(),
                now);

        PreviewEvaluation evaluation = repository.evaluate(criteria);
        if (evaluation.status() != PreviewEvaluationStatus.ELIGIBLE) {
            throw evaluationError(evaluation.status());
        }
        SharedRoutePreviewPreparation preparation = evaluation.requirePreparation();

        RoutePlanRequest routeRequest = new RoutePlanRequest(
                List.of(
                        waypoint(
                                RouteWaypointRole.DRIVER_ORIGIN,
                                preparation.route().origin().latitude(),
                                preparation.route().origin().longitude()),
                        waypoint(
                                RouteWaypointRole.PASSENGER_PICKUP,
                                request.pickup().latitude(),
                                request.pickup().longitude()),
                        waypoint(
                                RouteWaypointRole.PROPOSED_DROPOFF,
                                preparation.match().proposedDropoff().latitude(),
                                preparation.match().proposedDropoff().longitude()),
                        waypoint(
                                RouteWaypointRole.DRIVER_DESTINATION,
                                preparation.route().driverDestination().latitude(),
                                preparation.route().driverDestination().longitude())),
                preparation.vehicle().vehicleType(),
                false);

        RoutePlan routePlan = routePlanner.plan(routeRequest);

        Instant checkedAt = TimePolicy.now(clock);
        if (!repository.remainsCurrent(preparation.consistencyToken(), checkedAt)) {
            throw error(
                    HttpStatus.CONFLICT,
                    "SHARED_ROUTE_PREVIEW_STALE",
                    "Lộ trình đã thay đổi trong lúc tính phương án. Vui lòng tải lại và thử lại.");
        }

        return responseMapper.toResponse(preparation, request, routePlan, checkedAt);
    }

    private static RouteWaypoint waypoint(
            RouteWaypointRole role,
            BigDecimal latitude,
            BigDecimal longitude) {
        return new RouteWaypoint(role, new GeoCoordinate(latitude, longitude));
    }

    private static BusinessException evaluationError(PreviewEvaluationStatus status) {
        return switch (status) {
            case NOT_FOUND_OR_INACCESSIBLE -> error(
                    HttpStatus.NOT_FOUND,
                    "SHARED_ROUTE_NOT_FOUND",
                    "Không tìm thấy lộ trình chia sẻ phù hợp.");
            case ROUTE_UNAVAILABLE, SELF_ROUTE, DRIVER_OR_VEHICLE_INELIGIBLE -> error(
                    HttpStatus.CONFLICT,
                    "SHARED_ROUTE_UNAVAILABLE",
                    "Lộ trình không còn khả dụng để xem trước phương án đi chung.");
            case NO_LONGER_MATCHES -> error(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "SHARED_ROUTE_NO_LONGER_MATCHES",
                    "Lộ trình không còn phù hợp với điểm đón và điểm đến hiện tại.");
            case ELIGIBLE -> throw new IllegalArgumentException("ELIGIBLE is not an error status");
        };
    }

    private static void requireRouteId(Long sharedRouteId) {
        if (sharedRouteId == null || sharedRouteId <= 0) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SHARED_ROUTE_PREVIEW_REQUEST",
                    "routeId phải là số dương.");
        }
    }

    private static void requireRequest(PreviewSharedRouteRequest request) {
        if (request == null
                || request.schoolId() == null
                || request.schoolId() <= 0
                || !isValidPoint(request.pickup())
                || !isValidPoint(request.passengerDestination())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SHARED_ROUTE_PREVIEW_REQUEST",
                    "Dữ liệu xem trước phương án chưa đầy đủ hoặc không hợp lệ.");
        }
    }

    private static boolean isValidPoint(PreviewPointRequest point) {
        if (point == null
                || point.latitude() == null
                || point.longitude() == null
                || point.address() == null
                || point.address().isBlank()
                || point.address().length() > 500) {
            return false;
        }
        return Wgs84Coordinates.isValid(point.latitude(), point.longitude());
    }

    private static void requireDistinctEndpoints(
            PreviewPointRequest pickup,
            PreviewPointRequest destination) {
        if (Wgs84Coordinates.same(
                pickup.latitude(),
                pickup.longitude(),
                destination.latitude(),
                destination.longitude())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SHARED_ROUTE_PREVIEW_REQUEST",
                    "Điểm đón và điểm đến hành khách phải khác nhau.");
        }
    }

    private static BusinessException error(
            HttpStatus status,
            String code,
            String message) {
        return new BusinessException(status, code, message);
    }
}
