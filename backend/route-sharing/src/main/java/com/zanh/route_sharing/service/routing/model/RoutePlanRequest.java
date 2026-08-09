package com.zanh.route_sharing.service.routing.model;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record RoutePlanRequest(
        List<RouteWaypoint> waypoints,
        LoaiPhuongTien vehicleType,
        boolean alternatives,
        RoutePlanRequestMode mode) {

    public RoutePlanRequest(
            List<RouteWaypoint> waypoints,
            LoaiPhuongTien vehicleType,
            boolean alternatives) {
        this(waypoints, vehicleType, alternatives, RoutePlanRequestMode.STRICT);
    }

    public RoutePlanRequest {
        Objects.requireNonNull(waypoints, "waypoints không được trống");
        Objects.requireNonNull(vehicleType, "vehicleType không được trống");
        Objects.requireNonNull(mode, "mode không được trống");
        waypoints = List.copyOf(waypoints);
        if (waypoints.size() < 2) {
            throw new IllegalArgumentException("Route plan phải chứa ít nhất 2 điểm");
        }
        for (RouteWaypoint waypoint : waypoints) {
            Objects.requireNonNull(waypoint, "waypoint không được trống");
        }
        if (mode == RoutePlanRequestMode.STRICT) {
            requireStrictUniqueRoles(waypoints);
        } else {
            requireMultiPassengerCardinality(waypoints);
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

    public static RoutePlanRequest multiPassenger(
            List<RouteWaypoint> waypoints,
            LoaiPhuongTien vehicleType,
            boolean alternatives) {
        return new RoutePlanRequest(
                waypoints,
                vehicleType,
                alternatives,
                RoutePlanRequestMode.MULTI_PASSENGER);
    }

    private static void requireStrictUniqueRoles(List<RouteWaypoint> waypoints) {
        Set<RouteWaypointRole> roles = new HashSet<>();
        for (RouteWaypoint waypoint : waypoints) {
            if (!roles.add(waypoint.role())) {
                throw new IllegalArgumentException("Waypoint roles phải chứa các điểm dừng duy nhất");
            }
        }
    }

    private static void requireMultiPassengerCardinality(List<RouteWaypoint> waypoints) {
        Map<RouteWaypointRole, Integer> counts = new EnumMap<>(RouteWaypointRole.class);
        for (RouteWaypoint waypoint : waypoints) {
            counts.merge(waypoint.role(), 1, Integer::sum);
        }

        requireExactlyOne(counts, RouteWaypointRole.DRIVER_ORIGIN);
        requireExactlyOne(counts, RouteWaypointRole.DRIVER_DESTINATION);
        requireAtLeastOne(counts, RouteWaypointRole.PASSENGER_PICKUP);
        requireAtLeastOne(counts, RouteWaypointRole.PROPOSED_DROPOFF);

        for (RouteWaypointRole role : RouteWaypointRole.values()) {
            int count = counts.getOrDefault(role, 0);
            if (role == RouteWaypointRole.PASSENGER_PICKUP
                    || role == RouteWaypointRole.PROPOSED_DROPOFF) {
                continue;
            }
            if (count > 1) {
                throw new IllegalArgumentException("Waypoint role " + role + " không được lặp");
            }
        }

        if (waypoints.get(0).role() != RouteWaypointRole.DRIVER_ORIGIN
                || waypoints.get(waypoints.size() - 1).role() != RouteWaypointRole.DRIVER_DESTINATION) {
            throw new IllegalArgumentException("Multi-passenger route phải bắt đầu ở DRIVER_ORIGIN và kết thúc ở DRIVER_DESTINATION");
        }
    }

    private static void requireExactlyOne(
            Map<RouteWaypointRole, Integer> counts,
            RouteWaypointRole role) {
        if (counts.getOrDefault(role, 0) != 1) {
            throw new IllegalArgumentException("Waypoint role " + role + " phải xuất hiện đúng một lần");
        }
    }

    private static void requireAtLeastOne(
            Map<RouteWaypointRole, Integer> counts,
            RouteWaypointRole role) {
        if (counts.getOrDefault(role, 0) < 1) {
            throw new IllegalArgumentException("Waypoint role " + role + " phải xuất hiện ít nhất một lần");
        }
    }
}
