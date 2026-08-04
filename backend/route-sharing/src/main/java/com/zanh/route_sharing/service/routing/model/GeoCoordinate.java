package com.zanh.route_sharing.service.routing.model;

import java.math.BigDecimal;
import java.util.Objects;

public record GeoCoordinate(
        BigDecimal latitude,
        BigDecimal longitude) {

    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    public GeoCoordinate {
        Objects.requireNonNull(latitude, "latitude không được trống");
        Objects.requireNonNull(longitude, "longitude không được trống");

        if (latitude.compareTo(MIN_LATITUDE) < 0
                || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw new IllegalArgumentException("latitude phải trong đoạn -90..90");
        }
        if (longitude.compareTo(MIN_LONGITUDE) < 0
                || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new IllegalArgumentException("longitude phải trong đoạn -180..180");
        }
    }
}
