package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.trip.completion.TripCompletionRequest;
import com.zanh.route_sharing.dto.trip.completion.TripCompletionResponse;

public interface TripCompletionService {
    TripCompletionResponse completeTrip(Long actorId, Long tripId, TripCompletionRequest request);
}
