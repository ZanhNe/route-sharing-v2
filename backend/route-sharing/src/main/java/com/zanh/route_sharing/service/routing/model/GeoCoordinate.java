package com.zanh.route_sharing.service.routing.model;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;

import java.math.BigDecimal;
import java.util.Objects;

public record GeoCoordinate(
        BigDecimal latitude,
        BigDecimal longitude) {

    public GeoCoordinate {
        Objects.requireNonNull(latitude, "latitude không được trống");
        Objects.requireNonNull(longitude, "longitude không được trống");

        if (!Wgs84Coordinates.isValidLatitude(latitude)) {
            throw new IllegalArgumentException("latitude phải trong đoạn -90..90");
        }
        if (!Wgs84Coordinates.isValidLongitude(longitude)) {
            throw new IllegalArgumentException("longitude phải trong đoạn -180..180");
        }
    }
}
