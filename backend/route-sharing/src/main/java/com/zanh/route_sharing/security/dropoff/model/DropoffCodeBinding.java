package com.zanh.route_sharing.security.dropoff.model;

public record DropoffCodeBinding(Long tripId, Long rideRequestId, Long dropoffStopId) {
    public DropoffCodeBinding {
        if (tripId == null || tripId <= 0 || rideRequestId == null || rideRequestId <= 0 || dropoffStopId == null || dropoffStopId <= 0) {
            throw new IllegalArgumentException("DropoffCodeBinding không hợp lệ.");
        }
    }
}
