package com.zanh.route_sharing.service.routing.model;

import java.math.BigDecimal;
import java.util.Objects;

public record RouteBounds(
        BigDecimal southWestLongitude,
        BigDecimal southWestLatitude,
        BigDecimal northEastLongitude,
        BigDecimal northEastLatitude) {

    public RouteBounds {
        Objects.requireNonNull(southWestLongitude, "southWestLongitude không được trống");
        Objects.requireNonNull(southWestLatitude, "southWestLatitude không được trống");
        Objects.requireNonNull(northEastLongitude, "northEastLongitude không được trống");
        Objects.requireNonNull(northEastLatitude, "northEastLatitude không được trống");
    }
}
