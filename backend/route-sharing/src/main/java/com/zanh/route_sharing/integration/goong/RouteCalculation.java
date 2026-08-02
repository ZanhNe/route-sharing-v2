package com.zanh.route_sharing.integration.goong;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record RouteCalculation(
        List<RouteCoordinate> path,
        BigDecimal distanceMeters,
        long durationSeconds) {
    public RouteCalculation {
        Objects.requireNonNull(path, "path không được trống");
        Objects.requireNonNull(distanceMeters, "distanceMeters không được trống");
        path = List.copyOf(path);

        if (path.size() < 2) {
            throw new IllegalArgumentException("Tuyến đường phải có ít nhất hai tọa độ.");
        }
        if (distanceMeters.signum() <= 0) {
            throw new IllegalArgumentException("Khoảng cách dự kiến phải lớn hơn 0.");
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("Thời lượng dự kiến phải lớn hơn 0.");
        }
    }
}
