package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.trip.formation.TripFormationResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripformation.TripFormationRepository;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationCommitResult;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationPreparation;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.TripFormationService;
import com.zanh.route_sharing.service.realtime.RealtimeNotificationEventFactory;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.service.routing.RoutePlanner;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import com.zanh.route_sharing.service.tripformation.TripFormationResponseMapper;
import com.zanh.route_sharing.service.tripformation.TripStopOrderingPolicy;
import com.zanh.route_sharing.service.tripformation.model.PlannedTripStop;
import com.zanh.route_sharing.service.tripformation.model.TripFormationBookingSnapshot;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class TripFormationServiceImpl implements TripFormationService {

    private final TripFormationRepository repository;
    private final TripStopOrderingPolicy stopOrderingPolicy;
    private final RoutePlanner routePlanner;
    private final TripFormationResponseMapper responseMapper;
    private final UserRealtimeEventPublisher realtimeEventPublisher;
    private final Clock clock;

    public TripFormationServiceImpl(
            TripFormationRepository repository,
            TripStopOrderingPolicy stopOrderingPolicy,
            RoutePlanner routePlanner,
            TripFormationResponseMapper responseMapper,
            UserRealtimeEventPublisher realtimeEventPublisher,
            Clock clock) {
        this.repository = repository;
        this.stopOrderingPolicy = stopOrderingPolicy;
        this.routePlanner = routePlanner;
        this.responseMapper = responseMapper;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.clock = clock;
    }

    @Override
    public TripFormationResponse formTrip(Long actorId, Long routeId) {
        validateInput(actorId, routeId);
        TripFormationPreparation preparation = repository.prepare(actorId, routeId)
                .orElseThrow(TripFormationServiceImpl::notFound);

        if (preparation.routeStatus() == TrangThaiLoTrinh.LOCKED) {
            if (preparation.existingFormation() == null) {
                throw invariantViolation();
            }
            return responseMapper.toResponse(preparation.existingFormation(), "ALREADY_FORMED");
        }
        if (preparation.routeStatus() != TrangThaiLoTrinh.OPEN) {
            throw notOpen();
        }
        if (preparation.existingFormation() != null) {
            throw invariantViolation();
        }

        List<TripFormationBookingSnapshot> pending = preparation.activeRequests().stream()
                .filter(snapshot -> snapshot.status() == TrangThaiYeuCau.PENDING)
                .toList();
        if (!pending.isEmpty()) {
            throw pendingRemains();
        }
        List<TripFormationBookingSnapshot> accepted = preparation.activeRequests().stream()
                .filter(snapshot -> snapshot.status() == TrangThaiYeuCau.ACCEPTED)
                .toList();
        if (accepted.isEmpty()) {
            throw noAcceptedBookings();
        }
        if (preparation.arrivalRadiusMeters() == null
                || preparation.arrivalRadiusMeters().signum() <= 0) {
            throw invariantViolation();
        }
        if (preparation.eligibility() == null || !preparation.eligibility().eligible()) {
            throw ineligible();
        }

        final List<PlannedTripStop> orderedStops;
        try {
            orderedStops = stopOrderingPolicy.order(
                    preparation.originalRoute(),
                    preparation.origin(),
                    preparation.originAddress(),
                    preparation.driverDestination(),
                    preparation.driverDestinationAddress(),
                    accepted);
        } catch (IllegalArgumentException exception) {
            throw planInconsistent(exception.getMessage());
        }

        RoutePlanRequest planRequest;
        try {
            planRequest = RoutePlanRequest.multiPassenger(
                    orderedStops.stream().map(TripFormationServiceImpl::routeWaypoint).toList(),
                    preparation.vehicleType(),
                    false);
        } catch (IllegalArgumentException exception) {
            throw planInconsistent(exception.getMessage());
        }

        RoutePlan operationalPlan = planOperationalRoute(planRequest);
        Instant formedAt = TimePolicy.now(clock);
        TripFormationCommitResult committed = repository.commit(new TripFormationCommitCommand(
                actorId,
                routeId,
                formedAt,
                preparation,
                orderedStops,
                operationalPlan));

        if (committed.created()) {
            publishAfterCommit(committed);
        }
        return responseMapper.toResponse(
                committed.view(),
                committed.created() ? "CREATED" : "ALREADY_FORMED");
    }

    private RoutePlan planOperationalRoute(RoutePlanRequest request) {
        try {
            return routePlanner.plan(request);
        } catch (BusinessException exception) {
            if (exception.getStatus() == HttpStatus.BAD_REQUEST
                    && ("INVALID_ROUTE_WAYPOINTS".equals(exception.getCode())
                            || "INVALID_ROUTE_PLAN_REQUEST".equals(exception.getCode()))) {
                throw planInconsistent(exception.getMessage());
            }
            throw exception;
        }
    }

    private void publishAfterCommit(TripFormationCommitResult committed) {
        for (int index = 0; index < committed.realtimeRecipientUserIds().size(); index++) {
            realtimeEventPublisher.publish(
                    committed.realtimeRecipientUserIds().get(index),
                    RealtimeNotificationEventFactory.tripFormed(
                            committed.view().tripId(),
                            committed.view().routeId(),
                            committed.realtimeRideRequestIds().get(index),
                            committed.view().formedAt(),
                            committed.view().expectedDepartureTime()));
        }
    }

    private static RouteWaypoint routeWaypoint(PlannedTripStop stop) {
        Coordinate coordinate = stop.point().getCoordinate();
        GeoCoordinate point = new GeoCoordinate(
                BigDecimal.valueOf(coordinate.y),
                BigDecimal.valueOf(coordinate.x));
        return new RouteWaypoint(routeRole(stop.type()), point);
    }

    private static RouteWaypointRole routeRole(LoaiDiemDung type) {
        return switch (type) {
            case DRIVER_START -> RouteWaypointRole.DRIVER_ORIGIN;
            case PICKUP -> RouteWaypointRole.PASSENGER_PICKUP;
            case DROPOFF -> RouteWaypointRole.PROPOSED_DROPOFF;
            case DRIVER_END -> RouteWaypointRole.DRIVER_DESTINATION;
        };
    }

    private static void validateInput(Long actorId, Long routeId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "routeId phải là số dương.");
        }
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "SHARED_ROUTE_NOT_FOUND",
                "Không tìm thấy lộ trình chia sẻ.");
    }

    private static BusinessException notOpen() {
        return new BusinessException(HttpStatus.CONFLICT, "SHARED_ROUTE_NOT_OPEN",
                "Lộ trình không còn mở để hình thành chuyến đi.");
    }

    private static BusinessException pendingRemains() {
        return new BusinessException(HttpStatus.CONFLICT, "SHARED_ROUTE_PENDING_REQUESTS_REMAIN",
                "Lộ trình vẫn còn yêu cầu PENDING cần được xử lý.");
    }

    private static BusinessException noAcceptedBookings() {
        return new BusinessException(HttpStatus.CONFLICT, "SHARED_ROUTE_NO_ACCEPTED_BOOKINGS",
                "Lộ trình chưa có booking ACCEPTED để hình thành chuyến đi.");
    }

    private static BusinessException ineligible() {
        return new BusinessException(HttpStatus.CONFLICT, "DRIVER_OR_VEHICLE_INELIGIBLE",
                "Tài xế hoặc phương tiện không còn đủ điều kiện hình thành chuyến đi.");
    }

    private static BusinessException invariantViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_FORMATION_INVARIANT_VIOLATION",
                "Trạng thái lộ trình và chuyến đi đang không nhất quán.");
    }

    private static BusinessException planInconsistent(String detail) {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "TRIP_FORMATION_PLAN_INCONSISTENT",
                detail == null || detail.isBlank()
                        ? "Kế hoạch điểm dừng không nhất quán với lộ trình."
                        : detail);
    }
}
