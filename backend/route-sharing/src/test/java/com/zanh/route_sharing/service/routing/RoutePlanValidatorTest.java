package com.zanh.route_sharing.service.routing;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.service.routing.model.RouteBounds;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanLeg;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.zanh.route_sharing.service.routing.RoutePlanValidator;

class RoutePlanValidatorTest {

        private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

        @Test
        void givenOrderedWaypointsAndConsistentLegs_whenValidating_thenPlanIsAccepted() {
                RoutePlanRequest request = request();
                RoutePlan plan = plan(new Coordinate[] {
                                new Coordinate(106.68, 10.77),
                                new Coordinate(106.69, 10.77),
                                new Coordinate(106.705, 10.77),
                                new Coordinate(106.72, 10.77)
                });

                assertThatCode(() -> validator().validate(request, plan))
                                .doesNotThrowAnyException();
        }

        @Test
        void givenRouteLoopsBackNearOrigin_whenValidating_thenOriginRemainsAnchoredAtStart() {
                RoutePlanRequest request = request();
                RoutePlan plan = plan(new Coordinate[] {
                                new Coordinate(106.68, 10.77),
                                new Coordinate(106.69, 10.77),
                                new Coordinate(106.68, 10.77001),
                                new Coordinate(106.705, 10.77),
                                new Coordinate(106.72, 10.77)
                });

                assertThatCode(() -> validator().validate(request, plan))
                                .doesNotThrowAnyException();
        }


        @Test
        void givenProviderGeometryWithinSnapTolerance_whenValidating_thenExactCoordinateEqualityIsNotRequired() {
                RoutePlanRequest request = request();
                RoutePlan plan = plan(new Coordinate[] {
                                new Coordinate(106.68, 10.77),
                                new Coordinate(106.685, 10.7701),
                                new Coordinate(106.695, 10.7701),
                                new Coordinate(106.710, 10.7701),
                                new Coordinate(106.715, 10.7701),
                                new Coordinate(106.72, 10.77)
                });

                assertThatCode(() -> validator().validate(request, plan))
                                .doesNotThrowAnyException();
        }

        @Test
        void givenProviderGeometryOutsideSnapTolerance_whenValidating_thenPlanIsRejected() {
                RoutePlanRequest request = request();
                RoutePlan plan = plan(new Coordinate[] {
                                new Coordinate(106.68, 10.77),
                                new Coordinate(106.685, 10.7704),
                                new Coordinate(106.695, 10.7704),
                                new Coordinate(106.710, 10.7704),
                                new Coordinate(106.715, 10.7704),
                                new Coordinate(106.72, 10.77)
                });

                assertThatThrownBy(() -> validator().validate(request, plan))
                                .isInstanceOf(BusinessException.class)
                                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                                                .isEqualTo("MAP_PROVIDER_INVALID_RESPONSE"));
        }

        @Test
        void givenCollapsedLegBetweenDistantWaypoints_whenValidating_thenPlanIsRejected() {
                RoutePlanRequest request = request();
                RoutePlan plan = new RoutePlan(
                                line(new Coordinate[] {
                                                new Coordinate(106.68, 10.77),
                                                new Coordinate(106.69, 10.77),
                                                new Coordinate(106.705, 10.77),
                                                new Coordinate(106.72, 10.77)
                                }),
                                new BigDecimal("400"),
                                40L,
                                List.of(
                                                new RoutePlanLeg(1, RouteWaypointRole.DRIVER_ORIGIN,
                                                                RouteWaypointRole.PASSENGER_PICKUP,
                                                                new BigDecimal("100"), 10L, false),
                                                new RoutePlanLeg(2, RouteWaypointRole.PASSENGER_PICKUP,
                                                                RouteWaypointRole.PROPOSED_DROPOFF,
                                                                BigDecimal.ZERO, 0L, true),
                                                new RoutePlanLeg(3, RouteWaypointRole.PROPOSED_DROPOFF,
                                                                RouteWaypointRole.DRIVER_DESTINATION,
                                                                new BigDecimal("300"), 30L, false)),
                                List.of(),
                                bounds());

                assertThatThrownBy(() -> validator().validate(request, plan))
                                .isInstanceOf(BusinessException.class)
                                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                                                .isEqualTo("MAP_PROVIDER_INVALID_RESPONSE"));
        }

        @Test
        void givenLegRolesDoNotMatchRequest_whenValidating_thenPlanIsRejected() {
                RoutePlanRequest request = request();
                LineString geometry = line(new Coordinate[] {
                                new Coordinate(106.68, 10.77),
                                new Coordinate(106.69, 10.77),
                                new Coordinate(106.705, 10.77),
                                new Coordinate(106.72, 10.77)
                });
                RoutePlan plan = new RoutePlan(
                                geometry,
                                new BigDecimal("600"),
                                60L,
                                List.of(
                                                new RoutePlanLeg(1, RouteWaypointRole.DRIVER_ORIGIN,
                                                                RouteWaypointRole.PROPOSED_DROPOFF,
                                                                new BigDecimal("100"), 10L, false),
                                                new RoutePlanLeg(2, RouteWaypointRole.PASSENGER_PICKUP,
                                                                RouteWaypointRole.PROPOSED_DROPOFF,
                                                                new BigDecimal("200"), 20L, false),
                                                new RoutePlanLeg(3, RouteWaypointRole.PROPOSED_DROPOFF,
                                                                RouteWaypointRole.DRIVER_DESTINATION,
                                                                new BigDecimal("300"), 30L, false)),
                                List.of(),
                                bounds());

                assertThatThrownBy(() -> validator().validate(request, plan))
                                .isInstanceOf(BusinessException.class);
        }

        private static RoutePlanValidator validator() {
                return new RoutePlanValidator(new RoutePlanningPolicy(
                                new BigDecimal("2"),
                                new BigDecimal("20")));
        }

        private static RoutePlanRequest request() {
                return new RoutePlanRequest(
                                List.of(
                                                waypoint(RouteWaypointRole.DRIVER_ORIGIN, "10.77", "106.68"),
                                                waypoint(RouteWaypointRole.PASSENGER_PICKUP, "10.77", "106.69"),
                                                waypoint(RouteWaypointRole.PROPOSED_DROPOFF, "10.77", "106.705"),
                                                waypoint(RouteWaypointRole.DRIVER_DESTINATION, "10.77", "106.72")),
                                LoaiPhuongTien.XE_MAY,
                                false);
        }

        private static RouteWaypoint waypoint(
                        RouteWaypointRole role,
                        String latitude,
                        String longitude) {
                return new RouteWaypoint(
                                role,
                                new GeoCoordinate(new BigDecimal(latitude), new BigDecimal(longitude)));
        }

        private static RoutePlan plan(Coordinate[] coordinates) {
                return new RoutePlan(
                                line(coordinates),
                                new BigDecimal("600"),
                                60L,
                                List.of(
                                                new RoutePlanLeg(1, RouteWaypointRole.DRIVER_ORIGIN,
                                                                RouteWaypointRole.PASSENGER_PICKUP,
                                                                new BigDecimal("100"), 10L, false),
                                                new RoutePlanLeg(2, RouteWaypointRole.PASSENGER_PICKUP,
                                                                RouteWaypointRole.PROPOSED_DROPOFF,
                                                                new BigDecimal("200"), 20L, false),
                                                new RoutePlanLeg(3, RouteWaypointRole.PROPOSED_DROPOFF,
                                                                RouteWaypointRole.DRIVER_DESTINATION,
                                                                new BigDecimal("300"), 30L, false)),
                                List.of(),
                                bounds());
        }

        private static LineString line(Coordinate[] coordinates) {
                LineString line = GEOMETRY_FACTORY.createLineString(coordinates);
                line.setSRID(4326);
                return line;
        }

        private static RouteBounds bounds() {
                return new RouteBounds(
                                new BigDecimal("106.68"),
                                new BigDecimal("10.77"),
                                new BigDecimal("106.72"),
                                new BigDecimal("10.77"));
        }
}
