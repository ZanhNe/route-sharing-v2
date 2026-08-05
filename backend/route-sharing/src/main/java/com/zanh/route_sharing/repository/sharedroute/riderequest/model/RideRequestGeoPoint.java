package com.zanh.route_sharing.repository.sharedroute.riderequest.model;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;

import java.math.BigDecimal;
import java.util.Objects;

public record RideRequestGeoPoint(
        BigDecimal latitude,
        BigDecimal longitude) {

    public RideRequestGeoPoint {
        Objects.requireNonNull(latitude, "latitude không được trống");
        Objects.requireNonNull(longitude, "longitude không được trống");
        if (!Wgs84Coordinates.isValid(latitude, longitude)) {
            throw new IllegalArgumentException("Tọa độ phải thuộc WGS84");
        }
    }
}
