package com.zanh.route_sharing.security.boarding.model;

public record BoardingCodeBinding(Long tripId, Long rideRequestId, Long pickupStopId) {
    public BoardingCodeBinding {
        if (tripId == null || tripId <= 0
                || rideRequestId == null || rideRequestId <= 0
                || pickupStopId == null || pickupStopId <= 0) {
            throw new IllegalArgumentException("BoardingCodeBinding không hợp lệ.");
        }
    }
}
