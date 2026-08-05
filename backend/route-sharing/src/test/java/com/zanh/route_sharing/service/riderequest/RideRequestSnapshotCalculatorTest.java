package com.zanh.route_sharing.service.riderequest;

import com.zanh.route_sharing.domain.riderequest.RideRequestSnapshot;
import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPreparation;
import com.zanh.route_sharing.service.riderequest.model.PickupDeviation;
import com.zanh.route_sharing.service.routing.RoutePlanSegmentExtractor;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.testsupport.riderequest.CreateRideRequestRequestBuilder;
import com.zanh.route_sharing.testsupport.riderequest.RideRequestMother;
import com.zanh.route_sharing.testsupport.routing.PassengerRoutePlanMother;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RideRequestSnapshotCalculatorTest {

    private final RideRequestSnapshotCalculator sut = new RideRequestSnapshotCalculator(
            new RoutePlanSegmentExtractor(),
            new GeometryFactory(new PrecisionModel(), 4326));

    @Test
    void givenEligibleSegmentMatch_whenCalculating_thenCompleteImmutableSnapshotIsProduced() {
        RideRequestPreparation preparation = RideRequestMother.segmentPreparation();
        CreateRideRequestRequest request = new CreateRideRequestRequestBuilder().build();
        RoutePlanRequest planRequest = PassengerRoutePlanMother.request();
        RoutePlan plan = PassengerRoutePlanMother.plan();

        RideRequestSnapshot result = sut.calculate(
                preparation,
                request.pickup(),
                request.passengerDestination(),
                planRequest,
                plan,
                new PickupDeviation(new BigDecimal("100.00"), 60L),
                "Địa chỉ điểm thả",
                request.proposedSupportAmount());

        assertThat(result.matchType()).isEqualTo(preparation.matchType());
        assertThat(result.passengerDesiredDistanceMeters()).isEqualByComparingTo("4200.00");
        assertThat(result.servedDistanceMeters()).isEqualByComparingTo("3900.00");
        assertThat(result.remainingDistanceMeters()).isEqualByComparingTo("300.00");
        assertThat(result.convenienceRatioPercent()).isEqualByComparingTo("92.86");
        assertThat(result.proposedSupportAmount()).isEqualByComparingTo("25000.00");
        assertThat(result.agreedSupportAmount()).isNull();
        assertThat(result.proposedDropoff().address()).isEqualTo("Địa chỉ điểm thả");
        assertThat(result.servedRouteSegment().getCoordinateN(
                result.servedRouteSegment().getNumPoints() - 1).x)
                .isLessThan(result.passengerDesiredRoute().getCoordinateN(
                        result.passengerDesiredRoute().getNumPoints() - 1).x);
    }
}
