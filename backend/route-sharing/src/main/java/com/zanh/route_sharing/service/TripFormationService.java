package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.formation.TripFormationResponse;

public interface TripFormationService {
    TripFormationResponse formTrip(Long actorId, Long routeId);
}
