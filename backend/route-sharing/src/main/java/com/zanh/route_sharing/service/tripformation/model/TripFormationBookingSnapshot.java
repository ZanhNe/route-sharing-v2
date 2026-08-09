package com.zanh.route_sharing.service.tripformation.model;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import org.locationtech.jts.geom.Point;

public record TripFormationBookingSnapshot(
        Long rideRequestId,
        Long rideRequestVersion,
        Long passengerId,
        TrangThaiYeuCau status,
        Point pickup,
        String pickupAddress,
        Point dropoff,
        String dropoffAddress) {
}
