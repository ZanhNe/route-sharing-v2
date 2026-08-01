package com.zanh.route_sharing.utils;

import com.zanh.route_sharing.exception.BusinessException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;

public final class SpatialValidator {
    private static final int WGS84_SRID = 4326;

    private SpatialValidator() {
    }

    public static void validateWgs84Point(Point point, String name) {
        if (point == null || point.isEmpty()) {
            throw invalid("Thiếu hoặc rỗng: " + name);
        }
        validateSrid(point, name);
        validateWgs84Coordinate(point.getX(), point.getY(), name);
    }

    public static void validateWgs84LineString(LineString lineString, String name) {
        if (lineString == null || lineString.isEmpty() || lineString.getNumPoints() < 2 || lineString.getLength() == 0.0) {
            throw invalid(name + " phải chứa ít nhất hai điểm.");
        }
        validateSrid(lineString, name);
        for (Coordinate coordinate : lineString.getCoordinates()) {
            validateWgs84Coordinate(coordinate.x, coordinate.y, name);
        }
    }

    public static void validateWgs84Coordinate(double longitude, double latitude, String name) {
        if (!Double.isFinite(longitude) || !Double.isFinite(latitude)
                || longitude < -180 || longitude > 180
                || latitude < -90 || latitude > 90) {
            throw invalid("Tọa độ " + name + " không hợp lệ theo WGS84.");
        }
    }

    private static void validateSrid(Geometry geometry, String name) {
        if (geometry.getSRID() != WGS84_SRID) {
            throw invalid(name + " phải dùng SRID 4326.");
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_GEOMETRY", message);
    }
}
