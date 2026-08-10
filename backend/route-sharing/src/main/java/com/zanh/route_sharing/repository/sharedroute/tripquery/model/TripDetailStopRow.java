package com.zanh.route_sharing.repository.sharedroute.tripquery.model;

import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;

import java.math.BigDecimal;
import java.util.Objects;

public record TripDetailStopRow(
        Long stopId,
        Integer order,
        LoaiDiemDung type,
        TrangThaiDiemDung status,
        Long rideRequestId,
        BigDecimal latitude,
        BigDecimal longitude,
        String address) {
    public TripDetailStopRow {
        Objects.requireNonNull(stopId);
        Objects.requireNonNull(order);
        Objects.requireNonNull(type);
        Objects.requireNonNull(status);
        Objects.requireNonNull(latitude);
        Objects.requireNonNull(longitude);
        Objects.requireNonNull(address);
    }
}
