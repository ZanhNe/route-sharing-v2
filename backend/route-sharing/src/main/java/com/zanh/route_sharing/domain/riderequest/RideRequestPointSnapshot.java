package com.zanh.route_sharing.domain.riderequest;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import org.locationtech.jts.geom.Point;

import java.util.Objects;

public record RideRequestPointSnapshot(
        Point point,
        String address) {

    public RideRequestPointSnapshot {
        Objects.requireNonNull(point, "point không được trống");
        if (point.isEmpty()
                || point.getSRID() != Wgs84Coordinates.SRID
                || !Wgs84Coordinates.isValidLongitudeLatitude(point.getX(), point.getY())) {
            throw new IllegalArgumentException("point phải là Point WGS84 SRID 4326 hợp lệ");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address không được trống");
        }
        address = address.trim();
        if (address.length() > 500) {
            throw new IllegalArgumentException("address không được vượt quá 500 ký tự");
        }
    }
}
