package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.riderequest.RideRequestSnapshot;
import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.dto.riderequest.RideRequestResponse;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.RideRequestCreationRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCriteria;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluation;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluationStatus;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestGeoPoint;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPreparation;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.LocationLabelResolver;
import com.zanh.route_sharing.service.RideRequestCreationService;
import com.zanh.route_sharing.service.riderequest.RideRequestResponseMapper;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.service.riderequest.RideRequestSnapshotCalculator;
import com.zanh.route_sharing.service.riderequest.model.PickupDeviation;
import com.zanh.route_sharing.service.routing.RoutePlanner;
import com.zanh.route_sharing.service.routing.RoutePlanningPolicy;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import com.zanh.route_sharing.utils.GeoDistanceUtils;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class RideRequestCreationServiceImpl implements RideRequestCreationService {

    private final RideRequestCreationRepository repository;
    private final RoutePlanner routePlanner;
    private final LocationLabelResolver locationLabelResolver;
    private final RideRequestSnapshotCalculator snapshotCalculator;
    private final RideRequestResponseMapper responseMapper;
    private final RoutePlanningPolicy routePlanningPolicy;
    private final Clock clock;
    private final UserRealtimeEventPublisher realtimeEventPublisher;

    public RideRequestCreationServiceImpl(
            RideRequestCreationRepository repository,
            RoutePlanner routePlanner,
            LocationLabelResolver locationLabelResolver,
            RideRequestSnapshotCalculator snapshotCalculator,
            RideRequestResponseMapper responseMapper,
            RoutePlanningPolicy routePlanningPolicy,
            Clock clock,
            UserRealtimeEventPublisher realtimeEventPublisher) {
        this.repository = repository;
        this.routePlanner = routePlanner;
        this.locationLabelResolver = locationLabelResolver;
        this.snapshotCalculator = snapshotCalculator;
        this.responseMapper = responseMapper;
        this.routePlanningPolicy = routePlanningPolicy;
        this.clock = clock;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Override
    public RideRequestResponse create(
            Long actorUserId,
            Long routeId,
            CreateRideRequestRequest request) {
        AuthenticatedPrincipalValidator.requireUserId(actorUserId);
        requireRouteId(routeId);
        requireRequest(request);
        requireDistinctEndpoints(request.pickup(), request.passengerDestination());

        Instant evaluatedAt = TimePolicy.now(clock);
        RideRequestCriteria criteria = new RideRequestCriteria(
                actorUserId,
                request.schoolId(),
                routeId,
                request.pickup().latitude(),
                request.pickup().longitude(),
                request.passengerDestination().latitude(),
                request.passengerDestination().longitude(),
                evaluatedAt);

        RideRequestEvaluation evaluation = repository.evaluate(criteria);
        if (evaluation.status() != RideRequestEvaluationStatus.ELIGIBLE) {
            throw evaluationError(evaluation);
        }
        RideRequestPreparation preparation = evaluation.requirePreparation();

        RoutePlanRequest passengerPlanRequest = passengerPlanRequest(
                preparation,
                request.pickup(),
                request.passengerDestination());
        RoutePlan passengerPlan = plan(passengerPlanRequest);
        PickupDeviation pickupDeviation = pickupDeviation(preparation, request.pickup());
        String proposedDropoffAddress = proposedDropoffAddress(
                preparation,
                request.passengerDestination());

        Instant sentAt = TimePolicy.now(clock);

        RideRequestSnapshot snapshot = snapshotCalculator.calculate(
                preparation,
                request.pickup(),
                request.passengerDestination(),
                passengerPlanRequest,
                passengerPlan,
                pickupDeviation,
                proposedDropoffAddress,
                request.proposedSupportAmount());

        var persisted = repository.commit(
                new RideRequestCommitCommand(
                        actorUserId,
                        routeId,
                        sentAt,
                        snapshot,
                        request.note(),
                        preparation.consistencyToken()));

        realtimeEventPublisher.publish(
                preparation.driverId(),
                RealtimeNotificationEventFactory.bookingRequest(
                        persisted.rideRequestId(),
                        persisted.routeId(),
                        persisted.sentAt()));

        return responseMapper.toResponse(persisted);
    }

    private RoutePlanRequest passengerPlanRequest(
            RideRequestPreparation preparation,
            RouteEndpointRequest pickup,
            RouteEndpointRequest destination) {
        return new RoutePlanRequest(
                List.of(
                        waypoint(RouteWaypointRole.PASSENGER_PICKUP, pickup),
                        waypoint(RouteWaypointRole.PROPOSED_DROPOFF, preparation.proposedDropoff()),
                        waypoint(RouteWaypointRole.PASSENGER_DESTINATION, destination)),
                preparation.vehicleType(),
                false);
    }

    private PickupDeviation pickupDeviation(
            RideRequestPreparation preparation,
            RouteEndpointRequest pickup) {
        GeoCoordinate projection = coordinate(preparation.pickupProjection());
        GeoCoordinate pickupCoordinate = coordinate(pickup);
        if (GeoDistanceUtils.distanceMeters(projection, pickupCoordinate)
                .compareTo(routePlanningPolicy.duplicateWaypointToleranceMeters()) <= 0) {
            return PickupDeviation.zero();
        }

        RoutePlanRequest request = new RoutePlanRequest(
                List.of(
                        new RouteWaypoint(RouteWaypointRole.PICKUP_PROJECTION, projection),
                        new RouteWaypoint(RouteWaypointRole.PASSENGER_PICKUP, pickupCoordinate)),
                preparation.vehicleType(),
                false);
        RoutePlan plan = plan(request);
        return new PickupDeviation(plan.distanceMeters(), plan.durationSeconds());
    }

    private String proposedDropoffAddress(
            RideRequestPreparation preparation,
            RouteEndpointRequest passengerDestination) {
        if (preparation.dropoffType() == LoaiDiemTha.DIEM_DICH_CUOI_CUNG) {
            return passengerDestination.address();
        }
        return locationLabelResolver.resolve(coordinate(preparation.proposedDropoff()))
                .formattedAddress();
    }

    private RoutePlan plan(RoutePlanRequest request) {
        try {
            return routePlanner.plan(request);
        } catch (BusinessException exception) {
            if (exception.getStatus() == HttpStatus.BAD_REQUEST
                    && "INVALID_ROUTE_WAYPOINTS".equals(exception.getCode())) {
                throw invalid("Điểm đón và điểm đến hành khách phải đủ khác nhau để tính tuyến.");
            }
            if (exception.getStatus() == HttpStatus.UNPROCESSABLE_ENTITY
                    && "ROUTE_NOT_FOUND".equals(exception.getCode())) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "RIDE_REQUEST_ROUTE_NOT_COMPUTABLE",
                        "Không thể tính tuyến đường cần thiết để gửi yêu cầu đi chung.");
            }
            throw exception;
        }
    }

    private static RouteWaypoint waypoint(
            RouteWaypointRole role,
            RouteEndpointRequest point) {
        return new RouteWaypoint(role, coordinate(point));
    }

    private static RouteWaypoint waypoint(
            RouteWaypointRole role,
            RideRequestGeoPoint point) {
        return new RouteWaypoint(role, coordinate(point));
    }

    private static GeoCoordinate coordinate(RouteEndpointRequest point) {
        return new GeoCoordinate(point.latitude(), point.longitude());
    }

    private static GeoCoordinate coordinate(RideRequestGeoPoint point) {
        return new GeoCoordinate(point.latitude(), point.longitude());
    }

    private static BusinessException evaluationError(RideRequestEvaluation evaluation) {
        return switch (evaluation.status()) {
            case NOT_FOUND_OR_INACCESSIBLE -> error(
                    HttpStatus.NOT_FOUND,
                    "SHARED_ROUTE_NOT_FOUND",
                    "Không tìm thấy lộ trình chia sẻ phù hợp.");
            case ROUTE_UNAVAILABLE, SELF_ROUTE, DRIVER_OR_VEHICLE_INELIGIBLE -> error(
                    HttpStatus.CONFLICT,
                    "SHARED_ROUTE_UNAVAILABLE",
                    "Lộ trình không còn khả dụng để nhận yêu cầu đi chung.");
            case NO_LONGER_MATCHES -> error(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "SHARED_ROUTE_NO_LONGER_MATCHES",
                    "Lộ trình không còn phù hợp với điểm đón và điểm đến hiện tại.");
            case UNFINISHED_REQUEST_EXISTS -> new BusinessException(
                    HttpStatus.CONFLICT,
                    "UNFINISHED_RIDE_REQUEST_ALREADY_EXISTS",
                    "Bạn đang có một yêu cầu hoặc chuyến đi chưa kết thúc.",
                    Map.of(
                            "existingRideRequestId", evaluation.existingRideRequestId().toString(),
                            "status", evaluation.existingStatus().name()));
            case REJECTION_COOLDOWN_ACTIVE -> new BusinessException(
                    HttpStatus.CONFLICT,
                    "RIDE_REQUEST_REJECTION_COOLDOWN_ACTIVE",
                    "Bạn cần chờ hết thời gian tạm nghỉ trước khi gửi lại yêu cầu tới tài xế này.",
                    Map.of("cooldownUntil", evaluation.cooldownUntil().toString()));
            case ELIGIBLE -> throw new IllegalArgumentException("ELIGIBLE không phải trạng thái lỗi");
        };
    }

    private static void requireRouteId(Long routeId) {
        if (routeId == null || routeId <= 0) {
            throw invalid("routeId phải là số dương.");
        }
    }

    private static void requireRequest(CreateRideRequestRequest request) {
        if (request == null
                || request.schoolId() == null
                || request.schoolId() <= 0
                || !validPoint(request.pickup())
                || !validPoint(request.passengerDestination())
                || !validMoney(request.proposedSupportAmount())
                || (request.note() != null && request.note().length() > 1000)) {
            throw invalid("Dữ liệu gửi yêu cầu đi chung chưa đầy đủ hoặc không hợp lệ.");
        }
    }

    private static boolean validPoint(RouteEndpointRequest point) {
        return point != null
                && point.latitude() != null
                && point.longitude() != null
                && point.address() != null
                && !point.address().isBlank()
                && point.address().length() <= 500
                && Wgs84Coordinates.isValid(point.latitude(), point.longitude());
    }

    private static boolean validMoney(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return false;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        int fractionDigits = Math.max(normalized.scale(), 0);
        int integerDigits = Math.max(normalized.precision() - normalized.scale(), 0);
        return fractionDigits <= 2 && integerDigits <= 13;
    }

    private static void requireDistinctEndpoints(
            RouteEndpointRequest pickup,
            RouteEndpointRequest destination) {
        if (Wgs84Coordinates.same(
                pickup.latitude(),
                pickup.longitude(),
                destination.latitude(),
                destination.longitude())) {
            throw invalid("Điểm đón và điểm đến hành khách phải khác nhau.");
        }
    }

    private static BusinessException invalid(String message) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_RIDE_REQUEST", message);
    }

    private static BusinessException error(
            HttpStatus status,
            String code,
            String message) {
        return new BusinessException(status, code, message);
    }
}
