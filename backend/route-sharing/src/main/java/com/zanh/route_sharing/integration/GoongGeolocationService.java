package com.zanh.route_sharing.integration;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.zanh.route_sharing.config.GoongConfig;
import com.zanh.route_sharing.dto.response.GeoCoordinateResponseDTO;
import com.zanh.route_sharing.dto.response.GeoDirectionResponseDTO;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.utils.ExternalApiUtil;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoongGeolocationService {

    private final ExternalApiUtil externalApiUtil;
    private final GoongConfig goongConfig;

    // SRID 4326
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public Point getPointFromAddress(String address) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("address", address);
            params.put("api_key", goongConfig.getApiKey());

            GeoCoordinateResponseDTO response = externalApiUtil.sendGetRequest(
                    goongConfig.getBaseUrl() + "geocode", params, GeoCoordinateResponseDTO.class);

            if ("OK".equals(response.status()) && response.results().length > 0) {
                var location = response.results()[0].geometry().location();
                return geometryFactory.createPoint(new Coordinate(location.longitude(), location.latitude()));
            }
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Không tìm thấy tọa độ cho địa chỉ: " + address);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi kết nối API Geocode.");
        }
    }

    public GeoDirectionResponseDTO getDirection(Point origin, Point destination, List<Point> waypoints,
            String vehicle) {
        try {
            Map<String, Object> params = new HashMap<>();

            params.put("origin", origin.getY() + "," + origin.getX());

            List<String> destList = new ArrayList<>();
            if (waypoints != null && !waypoints.isEmpty()) {
                for (Point p : waypoints) {
                    destList.add(p.getY() + "," + p.getX());
                }
            }

            destList.add(destination.getY() + "," + destination.getX());

            // lat1,lng1;lat2,lng2
            String destinationParam = String.join(";", destList);
            params.put("destination", destinationParam);

            params.put("vehicle", vehicle != null ? vehicle : "bike");
            params.put("api_key", goongConfig.getApiKey());

            String finalUrl = goongConfig.getBaseUrl() + "Direction";

            GeoDirectionResponseDTO response = externalApiUtil.sendGetRequest(
                    finalUrl, params, GeoDirectionResponseDTO.class);

            if (response.routes() != null && response.routes().length > 0) {
                return response;
            }
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Không thể vẽ được lộ trình liên tuyến giữa các điểm này.");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi kết nối API Direction.");
        }
    }
}