package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.start.TripStartRequest;
import com.zanh.route_sharing.dto.trip.start.TripStartResponse;

public interface TripStartService {
    TripStartResponse startTrip(Long actorId, Long tripId, TripStartRequest request);
}
