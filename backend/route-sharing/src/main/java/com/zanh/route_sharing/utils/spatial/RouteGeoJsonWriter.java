package com.zanh.route_sharing.utils.spatial;

import com.zanh.route_sharing.dto.sharedroute.preview.GeoJsonLineStringResponse;
import com.zanh.route_sharing.exception.BusinessException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class RouteGeoJsonWriter {

    private static final String LINE_STRING = "LineString";

    private final JsonMapper jsonMapper;

    public RouteGeoJsonWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public GeoJsonLineStringResponse readStoredLineString(String geoJson) {
        try {
            GeoJsonLineStringResponse geometry = jsonMapper.readValue(
                    geoJson,
                    GeoJsonLineStringResponse.class);
            validate(geometry);
            return geometry;
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INVALID_STORED_ROUTE_GEOMETRY",
                    "Tuyến gốc đang lưu không thể dùng để tạo preview.");
        }
    }

    public GeoJsonLineStringResponse writePreviewLineString(LineString lineString) {
        if (lineString == null || lineString.isEmpty() || lineString.getNumPoints() < 2) {
            throw invalidProviderGeometry();
        }

        List<List<BigDecimal>> coordinates = Arrays.stream(lineString.getCoordinates())
                .map(RouteGeoJsonWriter::coordinate)
                .toList();

        GeoJsonLineStringResponse geometry = new GeoJsonLineStringResponse(
                LINE_STRING,
                coordinates);

        try {
            validate(geometry);
            return geometry;
        } catch (RuntimeException exception) {
            throw invalidProviderGeometry();
        }
    }

    private static List<BigDecimal> coordinate(Coordinate coordinate) {
        if (coordinate == null
                || !Double.isFinite(coordinate.x)
                || !Double.isFinite(coordinate.y)) {
            throw new IllegalArgumentException("Tọa độ phải là số hữu hạn");
        }

        return List.of(
                BigDecimal.valueOf(coordinate.x),
                BigDecimal.valueOf(coordinate.y));
    }

    private static void validate(GeoJsonLineStringResponse geometry) {
        if (geometry == null
                || !LINE_STRING.equals(geometry.type())
                || geometry.coordinates() == null
                || geometry.coordinates().size() < 2) {
            throw new IllegalArgumentException("Invalid GeoJSON LineString");
        }

        for (List<BigDecimal> pair : geometry.coordinates()) {
            if (pair == null
                    || pair.size() != 2
                    || pair.get(0) == null
                    || pair.get(1) == null
                    || pair.get(0).compareTo(BigDecimal.valueOf(-180)) < 0
                    || pair.get(0).compareTo(BigDecimal.valueOf(180)) > 0
                    || pair.get(1).compareTo(BigDecimal.valueOf(-90)) < 0
                    || pair.get(1).compareTo(BigDecimal.valueOf(90)) > 0) {
                throw new IllegalArgumentException("Invalid GeoJSON coordinate");
            }
        }
    }

    private static BusinessException invalidProviderGeometry() {
        return new BusinessException(
                HttpStatus.BAD_GATEWAY,
                "MAP_PROVIDER_INVALID_RESPONSE",
                "Dịch vụ bản đồ trả về geometry không hợp lệ.");
    }
}