package com.zanh.route_sharing.utils;

import org.locationtech.jts.geom.Coordinate;

import com.zanh.route_sharing.service.routing.model.GeoCoordinate;

import java.math.BigDecimal;

public final class GeoDistanceUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_008.8d;

    public static BigDecimal distanceMeters(
            GeoCoordinate left,
            GeoCoordinate right) {
        return BigDecimal.valueOf(distanceMeters(
                left.latitude().doubleValue(),
                left.longitude().doubleValue(),
                right.latitude().doubleValue(),
                right.longitude().doubleValue()));
    }

    public static double distanceMeters(
            Coordinate left,
            Coordinate right) {
        return distanceMeters(left.y, left.x, right.y, right.x);
    }

    public static double distanceMeters(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2) {
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        double deltaLat = Math.toRadians(latitude2 - latitude1);
        double deltaLon = Math.toRadians(longitude2 - longitude1);

        double sinLat = Math.sin(deltaLat / 2.0d);
        double sinLon = Math.sin(deltaLon / 2.0d);
        double a = sinLat * sinLat
                + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        double normalized = Math.max(0.0d, Math.min(1.0d, a));
        double c = 2.0d * Math.atan2(Math.sqrt(normalized), Math.sqrt(1.0d - normalized));
        return EARTH_RADIUS_METERS * c;
    }

    private GeoDistanceUtils() {
    }
}
