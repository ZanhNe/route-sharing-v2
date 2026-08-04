package com.zanh.route_sharing.repository.sharedroute.preview.model;

import java.math.BigDecimal;
import java.util.Objects;

public record PreviewGeoPoint(
        BigDecimal latitude,
        BigDecimal longitude,
        String address) {

    public PreviewGeoPoint {
        Objects.requireNonNull(latitude, "latitude không được trống");
        Objects.requireNonNull(longitude, "longitude không được trống");
    }
}
