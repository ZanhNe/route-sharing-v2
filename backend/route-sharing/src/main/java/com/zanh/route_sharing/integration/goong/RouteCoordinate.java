package com.zanh.route_sharing.integration.goong;

import java.math.BigDecimal;
import java.util.Objects;

public record RouteCoordinate(BigDecimal latitude, BigDecimal longitude) {
    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    public RouteCoordinate {
        Objects.requireNonNull(latitude, "latitude không được trống");
        Objects.requireNonNull(longitude, "longitude không được trống");

        if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw new IllegalArgumentException("Latitude phải nằm trong khoảng -90..90.");
        }
        if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new IllegalArgumentException("Longitude phải nằm trong khoảng -180..180.");
        }
    }

    public String toGoongParameter() {
        return latitude.toPlainString() + "," + longitude.toPlainString();
    }
}
