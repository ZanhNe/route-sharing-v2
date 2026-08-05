package com.zanh.route_sharing.service.routing;

import com.zanh.route_sharing.config.properties.GoongProperties;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanLeg;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.utils.GeoDistanceUtils;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RoutePlanValidator {

    private static final double INDEX_EPSILON = 1.0e-9d;

    private final GoongProperties properties;

    public RoutePlanValidator(GoongProperties properties) {
        this.properties = properties;
    }

    public void validate(RoutePlanRequest request, RoutePlan plan) {
        if (request == null || plan == null) {
            throw invalidProviderResponse("Dịch vụ bản đồ không tạo được phương án tuyến hợp lệ.");
        }

        validateGeometry(plan.geometry());
        validateLegs(request.waypoints(), plan);
        validateWaypointSequence(request.waypoints(), plan.geometry());
    }

    private static void validateGeometry(LineString geometry) {
        if (geometry == null
                || geometry.isEmpty()
                || geometry.getNumPoints() < 2
                || geometry.getLength() == 0.0d
                || geometry.getSRID() != 4326) {
            throw invalidProviderResponse("Dịch vụ bản đồ trả về LineString không hợp lệ.");
        }

        for (Coordinate coordinate : geometry.getCoordinates()) {
            if (!Wgs84Coordinates.isValidLongitudeLatitude(coordinate.x, coordinate.y)) {
                throw invalidProviderResponse("Dịch vụ bản đồ trả về tọa độ ngoài WGS84.");
            }
        }
    }

    private void validateLegs(
            List<RouteWaypoint> waypoints,
            RoutePlan plan) {
        List<RoutePlanLeg> legs = plan.legs();
        if (legs.size() != waypoints.size() - 1) {
            throw invalidProviderResponse("Số chặng đường không khớp thứ tự điểm dừng.");
        }

        BigDecimal distance = BigDecimal.ZERO;
        long duration = 0L;
        for (int index = 0; index < legs.size(); index++) {
            RoutePlanLeg leg = legs.get(index);
            RouteWaypoint from = waypoints.get(index);
            RouteWaypoint to = waypoints.get(index + 1);

            if (leg.sequence() != index + 1
                    || leg.fromRole() != from.role()
                    || leg.toRole() != to.role()) {
                throw invalidProviderResponse("Vai trò chặng đường không khớp thứ tự điểm dừng.");
            }

            if (leg.collapsed()
                    && GeoDistanceUtils.distanceMeters(from.coordinate(), to.coordinate())
                            .compareTo(properties.getDuplicateWaypointToleranceMeters()) > 0) {
                throw invalidProviderResponse(
                        "Chặng đường bị co lại dù hai điểm dừng không trùng nhau.");
            }

            distance = distance.add(leg.distanceMeters());
            try {
                duration = Math.addExact(duration, leg.durationSeconds());
            } catch (ArithmeticException exception) {
                throw invalidProviderResponse("Thời lượng tuyến vượt giới hạn xử lý.");
            }
        }

        if (distance.compareTo(plan.distanceMeters()) != 0
                || duration != plan.durationSeconds()) {
            throw invalidProviderResponse("Tổng khoảng cách hoặc thời lượng không khớp các chặng.");
        }
    }

    private void validateWaypointSequence(
            List<RouteWaypoint> waypoints,
            LineString geometry) {
        double tolerance = properties.getWaypointSnapToleranceMeters().doubleValue();
        Coordinate first = geometry.getCoordinateN(0);
        Coordinate last = geometry.getCoordinateN(geometry.getNumPoints() - 1);
        Coordinate expectedFirst = toJts(waypoints.get(0));
        Coordinate expectedLast = toJts(waypoints.get(waypoints.size() - 1));

        if (GeoDistanceUtils.distanceMeters(first, expectedFirst) > tolerance
                || GeoDistanceUtils.distanceMeters(last, expectedLast) > tolerance) {
            throw invalidProviderResponse("Điểm đầu hoặc điểm cuối của tuyến không đúng yêu cầu.");
        }

        double previousIndex = 0.0d;
        for (int waypointIndex = 1; waypointIndex < waypoints.size() - 1; waypointIndex++) {
            RouteWaypoint waypoint = waypoints.get(waypointIndex);
            Projection projection = projectOnSuffix(
                    geometry,
                    toJts(waypoint),
                    previousIndex);

            if (projection == null || projection.distanceMeters() > tolerance) {
                throw invalidProviderResponse(
                        "Tuyến trả về không đi đủ gần điểm dừng " + waypoint.role() + ".");
            }
            if (projection.index() + INDEX_EPSILON < previousIndex) {
                throw invalidProviderResponse("Dịch vụ bản đồ đảo thứ tự các điểm dừng.");
            }
            previousIndex = projection.index();
        }

        double routeEndIndex = geometry.getLength();
        if (routeEndIndex + INDEX_EPSILON < previousIndex) {
            throw invalidProviderResponse("Dịch vụ bản đồ đảo thứ tự các điểm dừng.");
        }
    }

    static Projection projectOnSuffix(
            LineString geometry,
            Coordinate expected,
            double minimumIndex) {
        Coordinate[] coordinates = geometry.getCoordinates();
        LengthIndexedLine indexedLine = new LengthIndexedLine(geometry);
        double cumulativeIndex = 0.0d;
        Projection best = null;

        for (int index = 0; index < coordinates.length - 1; index++) {
            Coordinate start = coordinates[index];
            Coordinate end = coordinates[index + 1];
            double segmentLength = start.distance(end);
            double segmentEndIndex = cumulativeIndex + segmentLength;

            if (segmentEndIndex + INDEX_EPSILON < minimumIndex) {
                cumulativeIndex = segmentEndIndex;
                continue;
            }

            double minimumFraction;
            if (segmentLength <= INDEX_EPSILON) {
                minimumFraction = minimumIndex <= cumulativeIndex + INDEX_EPSILON
                        ? 0.0d
                        : 1.0d;
            } else {
                minimumFraction = clamp(
                        (minimumIndex - cumulativeIndex) / segmentLength,
                        0.0d,
                        1.0d);
            }

            double projectedFraction = segmentLength <= INDEX_EPSILON
                    ? 0.0d
                    : projectionFraction(start, end, expected);
            double fraction = Math.max(minimumFraction, projectedFraction);
            double candidateIndex = cumulativeIndex + fraction * segmentLength;
            Coordinate candidate = indexedLine.extractPoint(candidateIndex);
            double distanceMeters = GeoDistanceUtils.distanceMeters(expected, candidate);

            if (best == null || distanceMeters < best.distanceMeters()) {
                best = new Projection(candidateIndex, distanceMeters);
            }
            cumulativeIndex = segmentEndIndex;
        }
        return best;
    }

    private static double projectionFraction(
            Coordinate start,
            Coordinate end,
            Coordinate point) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double denominator = dx * dx + dy * dy;
        if (denominator <= INDEX_EPSILON) {
            return 0.0d;
        }
        double fraction = ((point.x - start.x) * dx + (point.y - start.y) * dy)
                / denominator;
        return clamp(fraction, 0.0d, 1.0d);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static Coordinate toJts(RouteWaypoint waypoint) {
        return new Coordinate(
                waypoint.coordinate().longitude().doubleValue(),
                waypoint.coordinate().latitude().doubleValue());
    }

    private static BusinessException invalidProviderResponse(String message) {
        return new BusinessException(
                HttpStatus.BAD_GATEWAY,
                "MAP_PROVIDER_INVALID_RESPONSE",
                message);
    }

    record Projection(double index, double distanceMeters) {
    }
}
