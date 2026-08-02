package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.integration.goong.GoongApiGateway;
import com.zanh.route_sharing.integration.goong.GoongDirectionsResponse;
import com.zanh.route_sharing.integration.goong.RouteCalculation;
import com.zanh.route_sharing.integration.goong.RouteCoordinate;
import com.zanh.route_sharing.service.GoongRouteService;
import com.zanh.route_sharing.utils.PolylineUtils;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoongRouteServiceImpl implements GoongRouteService {
    private static final String DIRECTIONS_PATH = "/v2/direction";

    private final GoongApiGateway goongApiGateway;

    @Override
    public RouteCalculation calculate(
            RouteCoordinate origin,
            RouteCoordinate destination,
            LoaiPhuongTien vehicleType) {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("origin", origin.toGoongParameter());
        query.add("destination", destination.toGoongParameter());
        query.add("vehicle", toGoongVehicle(vehicleType));
        query.add("alternatives", "false");

        GoongDirectionsResponse response = goongApiGateway.get(
                DIRECTIONS_PATH,
                query,
                GoongDirectionsResponse.class);

        return mapResponse(response);
    }

    private static RouteCalculation mapResponse(GoongDirectionsResponse response) {
        if (response == null
                || response.routes() == null) {
            throw error(
                    HttpStatus.BAD_GATEWAY,
                    "MAP_PROVIDER_INVALID_RESPONSE",
                    "Dịch vụ bản đồ trả về dữ liệu không hợp lệ.");
        }

        if (response.routes().isEmpty()) {
            throw error(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "ROUTE_NOT_FOUND",
                    "Không tìm thấy tuyến đường phù hợp.");
        }

        GoongDirectionsResponse.RouteDto selectedRoute = response.routes().get(0);

        if (selectedRoute == null) {
            throw error(
                    HttpStatus.BAD_GATEWAY,
                    "MAP_PROVIDER_INVALID_RESPONSE",
                    "Dịch vụ bản đồ trả về tuyến đường không hợp lệ.");
        }

        GoongDirectionsResponse.RouteDto route = firstRoute(response);

        String encodedPolyline = route.overviewPolyline() == null
                ? null
                : route.overviewPolyline().points();

        if (encodedPolyline == null || encodedPolyline.isBlank()) {
            throw invalidProviderResponse("Dịch vụ bản đồ không trả về đường biểu diễn tuyến.");
        }

        long distanceMeters = sumLegValues(route.legs(), true);
        long durationSeconds = sumLegValues(route.legs(), false);

        if (distanceMeters <= 0 || durationSeconds <= 0) {
            throw invalidProviderResponse("Khoảng cách hoặc thời lượng của tuyến không hợp lệ.");
        }

        try {
            LineString decoded = PolylineUtils.decodeToLineString(encodedPolyline);
            List<RouteCoordinate> path = Arrays.stream(decoded.getCoordinates())
                    .map(GoongRouteServiceImpl::toRouteCoordinate)
                    .toList();

            return new RouteCalculation(
                    path,
                    BigDecimal.valueOf(distanceMeters),
                    durationSeconds);
        } catch (IllegalArgumentException exception) {
            throw invalidProviderResponse("Dịch vụ bản đồ trả về polyline không hợp lệ.");
        }
    }

    private static GoongDirectionsResponse.RouteDto firstRoute(GoongDirectionsResponse response) {
        if (response == null
                || response.routes() == null
                || response.routes().isEmpty()
                || response.routes().get(0) == null) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "ROUTE_NOT_FOUND",
                    "Không tìm thấy tuyến đường phù hợp giữa hai địa điểm.");
        }
        return response.routes().get(0);
    }

    private static long sumLegValues(
            List<GoongDirectionsResponse.LegDto> legs,
            boolean distance) {
        if (legs == null || legs.isEmpty()) {
            throw invalidProviderResponse("Dịch vụ bản đồ không trả về thông tin chặng đường.");
        }

        long total = 0;
        for (GoongDirectionsResponse.LegDto leg : legs) {
            GoongDirectionsResponse.ValueDto value = leg == null
                    ? null
                    : distance ? leg.distance() : leg.duration();

            if (value == null || value.value() == null || value.value() < 0) {
                throw invalidProviderResponse("Dịch vụ bản đồ trả về dữ liệu chặng đường không hợp lệ.");
            }

            try {
                total = Math.addExact(total, value.value());
            } catch (ArithmeticException exception) {
                throw invalidProviderResponse("Dữ liệu khoảng cách hoặc thời lượng vượt giới hạn.");
            }
        }
        return total;
    }

    private static RouteCoordinate toRouteCoordinate(Coordinate coordinate) {
        return new RouteCoordinate(
                BigDecimal.valueOf(coordinate.y),
                BigDecimal.valueOf(coordinate.x));
    }

    private static String toGoongVehicle(LoaiPhuongTien vehicleType) {
        if (vehicleType == null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "VEHICLE_TYPE_NOT_CONFIGURED",
                    "Phương tiện chưa được cấu hình loại phương tiện.");
        }

        return switch (vehicleType) {
            case XE_MAY -> "bike";
            case O_TO -> "car";
        };
    }

    private static BusinessException invalidProviderResponse(String message) {
        return new BusinessException(
                HttpStatus.BAD_GATEWAY,
                "MAP_PROVIDER_INVALID_RESPONSE",
                message);
    }

    private static BusinessException error(
            HttpStatus status,
            String code,
            String message) {
        return new BusinessException(status, code, message);
    }
}
