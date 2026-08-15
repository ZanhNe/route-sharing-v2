package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.location.TripLocationRequest;
import com.zanh.route_sharing.dto.trip.location.TripLocationResponse;

public interface TripLocationService {
    TripLocationResponse submitLocation(Long actorId, Long tripId, TripLocationRequest request);
}
