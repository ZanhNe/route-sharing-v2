package com.zanh.route_sharing.repository.sharedroute.tripformation.model;

import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;

public record TripFormationStopView(
        Long stopId,
        Integer order,
        LoaiDiemDung type,
        TrangThaiDiemDung status,
        Long rideRequestId,
        Point point,
        String address,
        BigDecimal arrivalRadiusMeters) {
}
