package com.zanh.route_sharing.service.routing.model;

import org.locationtech.jts.geom.LineString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record RoutePlan(
        LineString geometry,
        BigDecimal distanceMeters,
        long durationSeconds,
        List<RoutePlanLeg> legs,
        List<String> warnings,
        RouteBounds bounds) {

    public RoutePlan {
        Objects.requireNonNull(geometry, "geometry không được trống");
        Objects.requireNonNull(distanceMeters, "distanceMeters không được trống");
        Objects.requireNonNull(legs, "legs không được trống");
        Objects.requireNonNull(warnings, "warnings không được trống");
        Objects.requireNonNull(bounds, "bounds không được trống");
        legs = List.copyOf(legs);
        warnings = warnings.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        if (geometry.isEmpty() || geometry.getNumPoints() < 2 || geometry.getSRID() != 4326) {
            throw new IllegalArgumentException("geometry phải là LineString SRID 4326 không được trống");
        }
        if (distanceMeters.signum() <= 0 || durationSeconds <= 0) {
            throw new IllegalArgumentException("Tổng route phải hợp lệ");
        }
        if (legs.isEmpty()) {
            throw new IllegalArgumentException("Route plan phải chứa các segment hợp lệ");
        }
    }
}
