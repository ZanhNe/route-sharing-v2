package com.zanh.route_sharing.service.routing.model;

import java.util.Objects;

public record RouteWaypoint(
        RouteWaypointRole role,
        GeoCoordinate coordinate) {

    public RouteWaypoint {
        Objects.requireNonNull(role, "role route không được trống");
        Objects.requireNonNull(coordinate, "tọa độ không được trống");
    }
}
