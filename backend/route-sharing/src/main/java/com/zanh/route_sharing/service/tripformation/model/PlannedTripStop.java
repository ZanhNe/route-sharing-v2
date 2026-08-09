package com.zanh.route_sharing.service.tripformation.model;

import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import org.locationtech.jts.geom.Point;

public record PlannedTripStop(
        int order,
        LoaiDiemDung type,
        Long rideRequestId,
        Point point,
        String address,
        double routeIndex) {
}
