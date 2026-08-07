package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.riderequest.cancellation.RideRequestCancellationResponse;

public interface RideRequestCancellationService {
    RideRequestCancellationResponse cancelByPassenger(Long actorId, Long rideRequestId, String reason);
}
