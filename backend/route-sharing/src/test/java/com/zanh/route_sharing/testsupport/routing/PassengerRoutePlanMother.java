package com.zanh.route_sharing.testsupport.routing;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RouteBounds;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanLeg;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.util.List;

public final class PassengerRoutePlanMother {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private PassengerRoutePlanMother() {
    }

    public static RoutePlanRequest request() {
        return new RoutePlanRequest(
                List.of(
                        waypoint(RouteWaypointRole.PASSENGER_PICKUP, "10.776530", "106.700981"),
                        waypoint(RouteWaypointRole.PROPOSED_DROPOFF, "10.781800", "106.711900"),
                        waypoint(RouteWaypointRole.PASSENGER_DESTINATION, "10.782120", "106.712450")),
                LoaiPhuongTien.XE_MAY,
                false);
    }

    public static RoutePlan plan() {
        LineString geometry = GEOMETRY_FACTORY.createLineString(new Coordinate[] {
                new Coordinate(106.700981, 10.776530),
                new Coordinate(106.706000, 10.779000),
                new Coordinate(106.711900, 10.781800),
                new Coordinate(106.712450, 10.782120)
        });
        geometry.setSRID(4326);
        return new RoutePlan(
                geometry,
                new BigDecimal("4200.00"),
                600L,
                List.of(
                        new RoutePlanLeg(
                                1,
                                RouteWaypointRole.PASSENGER_PICKUP,
                                RouteWaypointRole.PROPOSED_DROPOFF,
                                new BigDecimal("3900.00"),
                                550L,
                                false),
                        new RoutePlanLeg(
                                2,
                                RouteWaypointRole.PROPOSED_DROPOFF,
                                RouteWaypointRole.PASSENGER_DESTINATION,
                                new BigDecimal("300.00"),
                                50L,
                                false)),
                List.of(),
                new RouteBounds(
                        new BigDecimal("106.700981"),
                        new BigDecimal("10.776530"),
                        new BigDecimal("106.712450"),
                        new BigDecimal("10.782120")));
    }

    private static RouteWaypoint waypoint(
            RouteWaypointRole role,
            String latitude,
            String longitude) {
        return new RouteWaypoint(
                role,
                new GeoCoordinate(
                        new BigDecimal(latitude),
                        new BigDecimal(longitude)));
    }
}
