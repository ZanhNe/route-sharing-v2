package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.utils.PaginationPolicy;
import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.riderequest.passengerquery.PassengerRideRequestDetailResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.PassengerRideRequestQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestDetailRow;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestPageSnapshot;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestQueryCriteria;
import com.zanh.route_sharing.service.PassengerRideRequestQueryService;
import com.zanh.route_sharing.service.riderequest.passengerquery.PassengerRideRequestResponseMapper;
import com.zanh.route_sharing.service.riderequest.passengerquery.model.PassengerRideRequestPageResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class PassengerRideRequestQueryServiceImpl implements PassengerRideRequestQueryService {

    private final PassengerRideRequestQueryRepository repository;
    private final PassengerRideRequestResponseMapper responseMapper;
    private final Clock clock;

    public PassengerRideRequestQueryServiceImpl(
            PassengerRideRequestQueryRepository repository,
            PassengerRideRequestResponseMapper responseMapper,
            Clock clock) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.clock = clock;
    }

    @Override
    public PassengerRideRequestPageResult listOwnRideRequests(
            Long actorUserId,
            TrangThaiYeuCau status,
            int page,
            int size) {
        requirePositive(actorUserId, "actorUserId");
        requirePage(page, size);

        PassengerRideRequestQueryCriteria criteria = new PassengerRideRequestQueryCriteria(
                actorUserId,
                status,
                page,
                size);
        Instant readAt = TimePolicy.now(clock);
        PassengerRideRequestPageSnapshot snapshot = repository.findPage(criteria);
        return responseMapper.toPage(snapshot, readAt);
    }

    @Override
    public PassengerRideRequestDetailResponse getOwnRideRequestDetail(
            Long actorUserId,
            Long rideRequestId) {
        requirePositive(actorUserId, "actorUserId");
        requirePositive(rideRequestId, "rideRequestId");

        Instant readAt = TimePolicy.now(clock);
        PassengerRideRequestDetailRow row = repository.findDetail(actorUserId, rideRequestId)
                .orElseThrow(PassengerRideRequestQueryServiceImpl::rideRequestNotFound);
        return responseMapper.toDetail(row, readAt);
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
        if (size < 1 || size > PaginationPolicy.MAX_SIZE) {
            throw invalidQuery("size phải nằm trong khoảng từ 1 đến " + PaginationPolicy.MAX_SIZE + ".");
        }
    }

    private static BusinessException invalidQuery(String message) {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_RIDE_REQUEST_QUERY",
                message);
    }

    private static BusinessException rideRequestNotFound() {
        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "RIDE_REQUEST_NOT_FOUND",
                "Không tìm thấy yêu cầu đi chung.");
    }
}
