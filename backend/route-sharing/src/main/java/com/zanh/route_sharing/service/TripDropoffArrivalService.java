package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.dropoffarrival.TripDropoffArrivalRequest;
import com.zanh.route_sharing.dto.trip.dropoffarrival.TripDropoffArrivalResponse;

public interface TripDropoffArrivalService {
    TripDropoffArrivalResponse confirmArrival(Long actorId, Long tripId, TripDropoffArrivalRequest request);
}
