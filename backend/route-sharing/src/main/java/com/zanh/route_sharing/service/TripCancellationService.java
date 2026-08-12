package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.cancellation.CancelTripBeforeStartRequest;
import com.zanh.route_sharing.dto.trip.cancellation.CancelTripBeforeStartResponse;

public interface TripCancellationService {
    CancelTripBeforeStartResponse cancelBeforeStart(
            Long actorId,
            Long tripId,
            CancelTripBeforeStartRequest request);
}
