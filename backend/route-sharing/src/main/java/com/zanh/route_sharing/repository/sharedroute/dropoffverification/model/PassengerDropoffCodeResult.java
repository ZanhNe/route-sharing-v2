package com.zanh.route_sharing.repository.sharedroute.dropoffverification.model;

public record PassengerDropoffCodeResult(Long tripId, Long rideRequestId, Long dropoffStopId, Integer dropoffStopOrder,
        String dropoffCode) {
}
