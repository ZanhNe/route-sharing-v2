package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.noshow.TripNoShowResponse;

public interface TripNoShowService {
    TripNoShowResponse confirmNoShow(Long actorId, Long tripId);
}
