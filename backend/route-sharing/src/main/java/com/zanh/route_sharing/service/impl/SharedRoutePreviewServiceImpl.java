package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.sharedroute.preview.PreviewPointRequest;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.preview.SharedRoutePreviewResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.PreviewEvaluation;
import com.zanh.route_sharing.repository.PreviewEvaluationStatus;
import com.zanh.route_sharing.repository.PreviewGeoPoint;
import com.zanh.route_sharing.repository.SharedRoutePreviewCriteria;
import com.zanh.route_sharing.repository.SharedRoutePreviewPreparation;
import com.zanh.route_sharing.repository.SharedRoutePreviewRepository;
import com.zanh.route_sharing.service.SharedRoutePreviewService;
import com.zanh.route_sharing.service.preview.PreviewResponseMapper;
import com.zanh.route_sharing.service.preview.RoutePlanValidator;
import com.zanh.route_sharing.service.routing.GeoCoordinate;
import com.zanh.route_sharing.service.routing.RoutePlan;
import com.zanh.route_sharing.service.routing.RoutePlanner;
import com.zanh.route_sharing.service.routing.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.RouteWaypoint;
import com.zanh.route_sharing.service.routing.RouteWaypointRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class SharedRoutePreviewServiceImpl implements SharedRoutePreviewService {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final SharedRoutePreviewRepository repository;
    private final RoutePlanner routePlanner;
    private final RoutePlanValidator routePlanValidator;
    private final PreviewResponseMapper responseMapper;
    private final Clock clock;

    public SharedRoutePreviewServiceImpl(
            SharedRoutePreviewRepository repository,
            RoutePlanner routePlanner,
            RoutePlanValidator routePlanValidator,
            PreviewResponseMapper responseMapper,
            Clock clock) {
        this.repository = repository;
        this.routePlanner = routePlanner;
        this.routePlanValidator = routePlanValidator;
        this.responseMapper = responseMapper;
        this.clock = clock;
    }

    @Override
    public SharedRoutePreviewResponse preview(
            Long actorUserId,
            Long sharedRouteId,
            PreviewSharedRouteRequest request) {
        requireActor(actorUserId);
        requireRouteId(sharedRouteId);
        requireRequest(request);
        requireDistinctEndpoints(request.pickup(), request.passengerDestination());

        Instant now = clock.instant();
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
                        waypoint(RouteWaypointRole.DRIVER_ORIGIN, preparation.route().origin()),
                        waypoint(RouteWaypointRole.PASSENGER_PICKUP, request.pickup()),
                        waypoint(RouteWaypointRole.PROPOSED_DROPOFF, preparation.match().proposedDropoff()),
                        waypoint(RouteWaypointRole.DRIVER_DESTINATION,
                                preparation.route().driverDestination())),
                preparation.vehicle().vehicleType(),
                false);

        RoutePlan routePlan = routePlanner.plan(routeRequest);
        routePlanValidator.validate(routeRequest, routePlan);

        Instant checkedAt = clock.instant();
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
            PreviewGeoPoint point) {
        return new RouteWaypoint(
                role,
                new GeoCoordinate(point.latitude(), point.longitude()));
    }

    private static RouteWaypoint waypoint(
            RouteWaypointRole role,
            PreviewPointRequest point) {
        return new RouteWaypoint(
                role,
                new GeoCoordinate(point.latitude(), point.longitude()));
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

    private static void requireActor(Long actorUserId) {
        if (actorUserId == null || actorUserId <= 0) {
            throw error(
                    HttpStatus.UNAUTHORIZED,
                    "AUTHENTICATED_USER_REQUIRED",
                    "Không xác định được người dùng đang đăng nhập.");
        }
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
        return between(point.latitude(), MIN_LATITUDE, MAX_LATITUDE)
                && between(point.longitude(), MIN_LONGITUDE, MAX_LONGITUDE);
    }

    private static boolean between(
            BigDecimal value,
            BigDecimal minimum,
            BigDecimal maximum) {
        return value.compareTo(minimum) >= 0 && value.compareTo(maximum) <= 0;
    }

    private static void requireDistinctEndpoints(
            PreviewPointRequest pickup,
            PreviewPointRequest destination) {
        if (pickup.latitude().compareTo(destination.latitude()) == 0
                && pickup.longitude().compareTo(destination.longitude()) == 0) {
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
