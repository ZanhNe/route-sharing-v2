package com.zanh.route_sharing.integration.goong;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;

import java.math.BigDecimal;
import java.util.Objects;

public record RouteCoordinate(BigDecimal latitude, BigDecimal longitude) {

    public RouteCoordinate {
        Objects.requireNonNull(latitude, "latitude không được trống");
        Objects.requireNonNull(longitude, "longitude không được trống");

        if (!Wgs84Coordinates.isValidLatitude(latitude)) {
            throw new IllegalArgumentException("Latitude phải nằm trong khoảng -90..90.");
        }
        if (!Wgs84Coordinates.isValidLongitude(longitude)) {
            throw new IllegalArgumentException("Longitude phải nằm trong khoảng -180..180.");
        }
    }

    public String toGoongParameter() {
        return latitude.toPlainString() + "," + longitude.toPlainString();
    }
}
