package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.dto.trip.detail.TripDetailResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripquery.TripDetailQueryRepository;
import com.zanh.route_sharing.service.TripDetailQueryService;
import com.zanh.route_sharing.service.tripdetail.TripDetailResponseMapper;
import com.zanh.route_sharing.service.tripdetail.TripDetailSnapshotValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class TripDetailQueryServiceImpl implements TripDetailQueryService {

    private final TripDetailQueryRepository repository;
    private final TripDetailSnapshotValidator validator;
    private final TripDetailResponseMapper responseMapper;
    private final Clock clock;

    public TripDetailQueryServiceImpl(
            TripDetailQueryRepository repository,
            TripDetailSnapshotValidator validator,
            TripDetailResponseMapper responseMapper,
            Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.responseMapper = responseMapper;
        this.clock = clock;
    }

    @Override
    public TripDetailResponse getTripDetail(Long actorUserId, Long tripId) {
        requirePositive(actorUserId, "actorUserId");
        requirePositive(tripId, "tripId");
        var snapshot = repository.findDetail(actorUserId, tripId)
                .orElseThrow(TripDetailQueryServiceImpl::tripNotFound);
        validator.validate(snapshot);
        return responseMapper.toResponse(snapshot, TimePolicy.now(clock));
    }

    private static void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TRIP_QUERY", name + " phải là số dương.");
        }
    }

    private static BusinessException tripNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi.");
    }
}
