package com.zanh.route_sharing.service.routing;

import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import com.zanh.route_sharing.testsupport.routing.PassengerRoutePlanMother;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutePlanSegmentExtractorTest {

    private final RoutePlanSegmentExtractor sut = new RoutePlanSegmentExtractor();

    @Test
    void givenPassengerPlan_whenExtractingPickupToDropoff_thenOnlyServedGeometryIsReturned() {
        RoutePlanRequest request = PassengerRoutePlanMother.request();
        RoutePlan plan = PassengerRoutePlanMother.plan();

        LineString result = sut.extract(
                request,
                plan,
                RouteWaypointRole.PASSENGER_PICKUP,
                RouteWaypointRole.PROPOSED_DROPOFF);

        assertThat(result.getSRID()).isEqualTo(4326);
        assertThat(result.getCoordinateN(0).x).isEqualTo(106.700981d);
        assertThat(result.getCoordinateN(result.getNumPoints() - 1).x)
                .isCloseTo(106.711900d, within(1.0e-9d));
        assertThat(result.getCoordinateN(result.getNumPoints() - 1).x)
                .isLessThan(plan.geometry().getCoordinateN(plan.geometry().getNumPoints() - 1).x);
    }

    @Test
    void givenMultiPassengerPlan_whenLookupRoleIsRepeated_thenExtractorFailsExplicitlyInsteadOfChoosingFirstOccurrence() {
        RoutePlanRequest request = RoutePlanRequest.multiPassenger(
                List.of(
                        waypoint(RouteWaypointRole.DRIVER_ORIGIN, "10.7700", "106.6800"),
                        waypoint(RouteWaypointRole.PASSENGER_PICKUP, "10.7700", "106.6900"),
                        waypoint(RouteWaypointRole.PASSENGER_PICKUP, "10.7700", "106.6950"),
                        waypoint(RouteWaypointRole.PROPOSED_DROPOFF, "10.7700", "106.7050"),
                        waypoint(RouteWaypointRole.PROPOSED_DROPOFF, "10.7700", "106.7150"),
                        waypoint(RouteWaypointRole.DRIVER_DESTINATION, "10.7700", "106.7200")),
                LoaiPhuongTien.XE_MAY,
                false);
        RoutePlan plan = com.zanh.route_sharing.testsupport.tripformation.TripFormationMother.routePlan();

        assertThatThrownBy(() -> sut.extract(
                request,
                plan,
                RouteWaypointRole.PASSENGER_PICKUP,
                RouteWaypointRole.DRIVER_DESTINATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mơ hồ");
    }

    private static RouteWaypoint waypoint(
            RouteWaypointRole role,
            String latitude,
            String longitude) {
        return new RouteWaypoint(role, new GeoCoordinate(
                new BigDecimal(latitude), new BigDecimal(longitude)));
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }

}
