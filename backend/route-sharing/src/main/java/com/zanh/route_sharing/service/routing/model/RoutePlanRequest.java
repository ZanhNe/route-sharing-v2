package com.zanh.route_sharing.service.routing.model;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RoutePlanRequest(
        List<RouteWaypoint> waypoints,
        LoaiPhuongTien vehicleType,
        boolean alternatives) {

    public RoutePlanRequest {
        Objects.requireNonNull(waypoints, "waypoints không được trống");
        Objects.requireNonNull(vehicleType, "vehicleType không được trống");
        waypoints = List.copyOf(waypoints);
        if (waypoints.size() < 2) {
            throw new IllegalArgumentException("Route plan phải chứa ít nhất 2 điểm");
        }
        Set<RouteWaypointRole> roles = new HashSet<>();
        for (RouteWaypoint waypoint : waypoints) {
            Objects.requireNonNull(waypoint, "waypoint không được trống");
            if (!roles.add(waypoint.role())) {
                throw new IllegalArgumentException("Waypoint roles phải chứa các điểm dừng");
            }
        }
    }

    public static RoutePlanRequest direct(
            GeoCoordinate origin,
            GeoCoordinate destination,
            LoaiPhuongTien vehicleType) {
        return new RoutePlanRequest(
                List.of(
                        new RouteWaypoint(RouteWaypointRole.DRIVER_ORIGIN, origin),
                        new RouteWaypoint(RouteWaypointRole.DRIVER_DESTINATION, destination)),
                vehicleType,
                false);
    }
}
