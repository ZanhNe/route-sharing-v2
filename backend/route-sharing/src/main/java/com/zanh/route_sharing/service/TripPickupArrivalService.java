package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.pickuparrival.TripPickupArrivalRequest;
import com.zanh.route_sharing.dto.trip.pickuparrival.TripPickupArrivalResponse;

public interface TripPickupArrivalService {
    TripPickupArrivalResponse confirmArrival(Long actorId, Long tripId, TripPickupArrivalRequest request);
}
