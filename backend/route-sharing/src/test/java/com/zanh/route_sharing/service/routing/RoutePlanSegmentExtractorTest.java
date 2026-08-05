package com.zanh.route_sharing.service.routing;

import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import com.zanh.route_sharing.testsupport.routing.PassengerRoutePlanMother;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;

import static org.assertj.core.api.Assertions.assertThat;

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

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }

}
