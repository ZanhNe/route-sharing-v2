package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestDetailResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.RouteRideRequestQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestDetailLookup;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestPageSnapshot;
import com.zanh.route_sharing.service.RouteRideRequestQueryService;
import com.zanh.route_sharing.service.riderequest.query.RouteRideRequestResponseMapper;
import com.zanh.route_sharing.service.riderequest.query.model.RouteRideRequestPageResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class RouteRideRequestQueryServiceImpl implements RouteRideRequestQueryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final RouteRideRequestQueryRepository repository;
    private final RouteRideRequestResponseMapper responseMapper;
    private final Clock clock;

    public RouteRideRequestQueryServiceImpl(
            RouteRideRequestQueryRepository repository,
            RouteRideRequestResponseMapper responseMapper,
            Clock clock) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.clock = clock;
    }

    @Override
    public RouteRideRequestPageResult listPending(
            Long actorUserId,
            Long routeId,
            int page,
            int size) {
        requirePositive(actorUserId, "actorUserId");
        requirePositive(routeId, "routeId");
        requirePage(page, size);

        PendingRideRequestPageSnapshot snapshot = repository.findPendingPage(
                        actorUserId,
                        routeId,
                        page,
                        size)
                .orElseThrow(RouteRideRequestQueryServiceImpl::routeNotFound);
        Instant readAt = clock.instant();
        return responseMapper.toPage(snapshot, readAt);
    }

    @Override
    public RouteRideRequestDetailResponse getPendingDetail(
            Long actorUserId,
            Long routeId,
            Long rideRequestId) {
        requirePositive(actorUserId, "actorUserId");
        requirePositive(routeId, "routeId");
        requirePositive(rideRequestId, "rideRequestId");

        PendingRideRequestDetailLookup lookup = repository.findPendingDetail(
                actorUserId,
                routeId,
                rideRequestId);
        if (lookup.status() == PendingRideRequestDetailLookup.Status.ROUTE_NOT_FOUND_OR_NOT_OWNED) {
            throw routeNotFound();
        }
        if (lookup.status() == PendingRideRequestDetailLookup.Status.REQUEST_NOT_FOUND_OR_NOT_PENDING) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "RIDE_REQUEST_NOT_FOUND",
                    "Không tìm thấy yêu cầu đi chung đang chờ xử lý trong lộ trình này.");
        }
        return responseMapper.toDetail(lookup, clock.instant());
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw invalidQuery(fieldName + " phải là số dương.");
        }
    }

    private static void requirePage(int page, int size) {
        if (page < 0) {
            throw invalidQuery("page phải lớn hơn hoặc bằng 0.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw invalidQuery("size phải nằm trong khoảng từ 1 đến 50.");
        }
    }

    private static BusinessException invalidQuery(String message) {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_RIDE_REQUEST_QUERY",
                message);
    }

    private static BusinessException routeNotFound() {
        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "SHARED_ROUTE_NOT_FOUND",
                "Không tìm thấy lộ trình chia sẻ.");
    }
}
