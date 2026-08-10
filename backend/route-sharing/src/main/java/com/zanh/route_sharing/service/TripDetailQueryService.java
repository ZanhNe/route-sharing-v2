package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.detail.TripDetailResponse;

public interface TripDetailQueryService {
    TripDetailResponse getTripDetail(Long actorUserId, Long tripId);
}
