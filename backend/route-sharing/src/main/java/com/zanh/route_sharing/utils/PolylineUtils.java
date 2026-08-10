package com.zanh.route_sharing.utils;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.ArrayList;
import java.util.List;

public final class PolylineUtils {
    private static final int DEFAULT_PRECISION = 5;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(),
            Wgs84Coordinates.SRID);

    private PolylineUtils() {
    }

    public static LineString decodeToLineString(String encoded) {
        return decodeToLineString(encoded, DEFAULT_PRECISION);
    }

    public static LineString decodeToLineString(String encoded, int precision) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException("Chuỗi polyline không được để trống.");
        }
        if (precision < 0 || precision > 8) {
            throw new IllegalArgumentException("Polyline precision phải nằm trong khoảng 0..8.");
        }

        double factor = Math.pow(10, precision);
        List<Coordinate> coordinates = new ArrayList<>();
        int[] cursor = { 0 };
        long latitude = 0;
        long longitude = 0;

        while (cursor[0] < encoded.length()) {
            try {
                latitude = Math.addExact(latitude, decodeDelta(encoded, cursor));
                longitude = Math.addExact(longitude, decodeDelta(encoded, cursor));
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Polyline coordinate vượt giới hạn.", exception);
            }
            double lat = latitude / factor;
            double lng = longitude / factor;
            SpatialValidator.validateWgs84Coordinate(lng, lat, "polyline");
            coordinates.add(new Coordinate(lng, lat));
        }
        if (coordinates.size() < 2) {
            throw new IllegalArgumentException("Polyline phải chứa ít nhất hai tọa độ.");
        }
        LineString lineString = GEOMETRY_FACTORY.createLineString(coordinates.toArray(Coordinate[]::new));
        lineString.setSRID(Wgs84Coordinates.SRID);
        return lineString;
    }

    private static long decodeDelta(String encoded, int[] cursor) {
        long result = 0;
        int shift = 0;
        int value;
        do {
            if (cursor[0] >= encoded.length()) {
                throw new IllegalArgumentException("Polyline bị cắt hoặc sai định dạng.");
            }
            value = encoded.charAt(cursor[0]++) - 63;
            if (value < 0 || value > 63) {
                throw new IllegalArgumentException("Polyline chứa ký tự không hợp lệ.");
            }
            if (shift > 55) {
                throw new IllegalArgumentException("Polyline delta vượt giới hạn.");
            }
            result |= (long) (value & 0x1f) << shift;
            shift += 5;
        } while (value >= 0x20);
        return (result & 1L) != 0 ? ~(result >> 1) : result >> 1;
    }
}
