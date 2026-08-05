package com.zanh.route_sharing.service.routing;

import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoutePlanSegmentExtractor {

    public LineString extract(
            RoutePlanRequest request,
            RoutePlan plan,
            RouteWaypointRole fromRole,
            RouteWaypointRole toRole) {
        if (request == null || plan == null || fromRole == null || toRole == null) {
            throw new IllegalArgumentException("Dữ liệu trích đoạn tuyến không được trống");
        }

        List<RouteWaypoint> waypoints = request.waypoints();
        int fromPosition = positionOf(waypoints, fromRole);
        int toPosition = positionOf(waypoints, toRole);
        if (fromPosition >= toPosition) {
            throw new IllegalArgumentException("Vai trò điểm cuối phải nằm sau điểm đầu");
        }

        double[] indices = orderedWaypointIndices(waypoints, plan.geometry());
        double fromIndex = indices[fromPosition];
        double toIndex = indices[toPosition];
        if (toIndex <= fromIndex) {
            throw new IllegalArgumentException("Đoạn tuyến được phục vụ phải có độ dài dương");
        }

        Geometry extracted = new LengthIndexedLine(plan.geometry()).extractLine(fromIndex, toIndex);
        if (!(extracted instanceof LineString lineString)
                || lineString.isEmpty()
                || lineString.getNumPoints() < 2
                || lineString.getLength() == 0.0d) {
            throw new IllegalArgumentException("Không thể trích LineString cho đoạn tuyến được phục vụ");
        }
        lineString.setSRID(4326);
        return lineString;
    }

    private static int positionOf(List<RouteWaypoint> waypoints, RouteWaypointRole role) {
        for (int index = 0; index < waypoints.size(); index++) {
            if (waypoints.get(index).role() == role) {
                return index;
            }
        }
        throw new IllegalArgumentException("Route plan không chứa waypoint " + role);
    }

    private static double[] orderedWaypointIndices(
            List<RouteWaypoint> waypoints,
            LineString geometry) {
        double[] indices = new double[waypoints.size()];
        indices[0] = 0.0d;
        double previous = 0.0d;
        for (int index = 1; index < waypoints.size() - 1; index++) {
            RouteWaypoint waypoint = waypoints.get(index);
            Coordinate coordinate = new Coordinate(
                    waypoint.coordinate().longitude().doubleValue(),
                    waypoint.coordinate().latitude().doubleValue());
            RoutePlanValidator.Projection projection = RoutePlanValidator.projectOnSuffix(
                    geometry,
                    coordinate,
                    previous);
            if (projection == null) {
                throw new IllegalArgumentException("Không thể định vị waypoint trên tuyến");
            }
            indices[index] = projection.index();
            previous = projection.index();
        }
        indices[waypoints.size() - 1] = geometry.getLength();
        return indices;
    }
}
