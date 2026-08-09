package com.zanh.route_sharing.service.riderequest;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.riderequest.RideRequestSnapshot;
import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestGeoPoint;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPreparation;
import com.zanh.route_sharing.service.riderequest.model.PickupDeviation;
import com.zanh.route_sharing.service.routing.RoutePlanSegmentExtractor;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RouteBounds;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanLeg;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import com.zanh.route_sharing.testsupport.riderequest.CreateRideRequestRequestBuilder;
import com.zanh.route_sharing.testsupport.riderequest.RideRequestMother;
import com.zanh.route_sharing.testsupport.routing.PassengerRoutePlanMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideRequestSnapshotCalculatorTest {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final RideRequestSnapshotCalculator sut = new RideRequestSnapshotCalculator(
            new RoutePlanSegmentExtractor(),
            GEOMETRY_FACTORY);

    @Test
    void givenEligibleSegmentMatch_whenCalculating_thenCompleteImmutableSnapshotIsProduced() {
        RideRequestPreparation preparation = RideRequestMother.segmentPreparation();
        CreateRideRequestRequest request = new CreateRideRequestRequestBuilder().build();

        RoutePlan originalPlan = PassengerRoutePlanMother.plan();
        RideRequestSnapshot result = calculate(
                preparation,
                PassengerRoutePlanMother.request(),
                originalPlan,
                new PickupDeviation(new BigDecimal("100.004"), 60L));

        assertThat(result.matchType()).isEqualTo(LoaiGhepTuyen.TRUNG_DOAN_TUYEN);
        assertThat(result.dropoffType()).isEqualTo(LoaiDiemTha.DIEM_THA_TRUNG_GIAN);
        assertThat(result.passengerDesiredDistanceMeters()).isEqualByComparingTo("4200.00");
        assertThat(result.servedDistanceMeters()).isEqualByComparingTo("3900.00");
        assertThat(result.remainingDistanceMeters()).isEqualByComparingTo("300.00");
        assertThat(result.convenienceRatioPercent()).isEqualByComparingTo("92.86");
        assertThat(result.pickupDeviationMeters()).isEqualByComparingTo("100.00");
        assertThat(result.proposedSupportAmount()).isEqualByComparingTo("25000.00");
        assertThat(result.agreedSupportAmount()).isNull();
        assertThat(result.proposedDropoff().address()).isEqualTo("Địa chỉ điểm thả");
        assertThat(result.passengerDesiredRoute()).isNotSameAs(originalPlan.geometry());
        assertThat(result.servedRouteSegment().getCoordinateN(
                result.servedRouteSegment().getNumPoints() - 1).x)
                .isLessThan(result.passengerDesiredRoute().getCoordinateN(
                        result.passengerDesiredRoute().getNumPoints() - 1).x);
    }

    @Test
    void givenSameDestinationAndCollapsedFinalLeg_whenCalculating_thenValidFinalDestinationSnapshotIsProduced() {
        RideRequestPreparation preparation = sameDestinationPreparation();
        RoutePlanRequest request = sameDestinationRequest();
        RoutePlan plan = plan("3900.00", "3900.00", "0.00");

        RideRequestSnapshot result = calculate(
                preparation,
                request,
                plan,
                new PickupDeviation(BigDecimal.ZERO, 0L));

        assertThat(result.remainingDistanceMeters()).isZero();
        assertThat(result.convenienceRatioPercent()).isEqualByComparingTo("100.00");
        assertThat(result.matchType()).isEqualTo(LoaiGhepTuyen.CUNG_DIEM_DEN);
        assertThat(result.dropoffType()).isEqualTo(LoaiDiemTha.DIEM_DICH_CUOI_CUNG);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("routeCalculationFailures")
    void givenInvalidProviderOrMatchingResult_whenCalculating_thenStableBusinessErrorIsReturned(
            String description,
            RideRequestPreparation preparation,
            RoutePlanRequest request,
            RoutePlan plan,
            PickupDeviation deviation,
            String expectedCode) {
        assertThatThrownBy(() -> calculate(preparation, request, plan, deviation))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(422);
                    assertThat(exception.getCode()).isEqualTo(expectedCode);
                });
    }

    @ParameterizedTest
    @MethodSource("missingArguments")
    void givenMissingRequiredInput_whenCalculating_thenNullIsRejected(
            RideRequestPreparation preparation,
            RoutePlanRequest planRequest,
            RoutePlan plan,
            PickupDeviation deviation,
            BigDecimal support) {
        CreateRideRequestRequest request = new CreateRideRequestRequestBuilder().build();
        assertThatThrownBy(() -> sut.calculate(
                preparation,
                request.pickup(),
                request.passengerDestination(),
                planRequest,
                plan,
                deviation,
                "Địa chỉ điểm thả",
                support))
                .isInstanceOf(NullPointerException.class);
    }

    private RideRequestSnapshot calculate(
            RideRequestPreparation preparation,
            RoutePlanRequest planRequest,
            RoutePlan plan,
            PickupDeviation deviation) {
        CreateRideRequestRequest request = new CreateRideRequestRequestBuilder().build();
        return sut.calculate(
                preparation,
                request.pickup(),
                request.passengerDestination(),
                planRequest,
                plan,
                deviation,
                "Địa chỉ điểm thả",
                request.proposedSupportAmount());
    }

    private static Stream<Arguments> routeCalculationFailures() {
        RideRequestPreparation segment = RideRequestMother.segmentPreparation();
        return Stream.of(
                Arguments.of(
                        "missing semantic leg",
                        segment,
                        PassengerRoutePlanMother.request(),
                        planWithLegs(List.of(
                                leg(RouteWaypointRole.PASSENGER_PICKUP,
                                        RouteWaypointRole.PASSENGER_DESTINATION, "4200", 600))),
                        new PickupDeviation(BigDecimal.ZERO, 0L),
                        "RIDE_REQUEST_ROUTE_NOT_COMPUTABLE"),
                Arguments.of(
                        "ambiguous duplicate semantic leg",
                        segment,
                        PassengerRoutePlanMother.request(),
                        planWithLegs(new BigDecimal("4200.00"), List.of(
                                leg(RouteWaypointRole.PASSENGER_PICKUP,
                                        RouteWaypointRole.PROPOSED_DROPOFF, "1950", 250),
                                leg(RouteWaypointRole.PASSENGER_PICKUP,
                                        RouteWaypointRole.PROPOSED_DROPOFF, "1950", 250),
                                leg(RouteWaypointRole.PROPOSED_DROPOFF,
                                        RouteWaypointRole.PASSENGER_DESTINATION, "300", 100))),
                        new PickupDeviation(BigDecimal.ZERO, 0L),
                        "RIDE_REQUEST_ROUTE_NOT_COMPUTABLE"),
                Arguments.of(
                        "leg totals differ from provider total",
                        segment,
                        PassengerRoutePlanMother.request(),
                        plan("4201.00", "3900.00", "300.00"),
                        new PickupDeviation(BigDecimal.ZERO, 0L),
                        "RIDE_REQUEST_ROUTE_NOT_COMPUTABLE"),
                Arguments.of(
                        "convenience ratio below school threshold",
                        segment,
                        PassengerRoutePlanMother.request(),
                        plan("4200.00", "2500.00", "1700.00"),
                        new PickupDeviation(BigDecimal.ZERO, 0L),
                        "SHARED_ROUTE_NO_LONGER_MATCHES"),
                Arguments.of(
                        "pickup distance above threshold",
                        segment,
                        PassengerRoutePlanMother.request(),
                        PassengerRoutePlanMother.plan(),
                        new PickupDeviation(new BigDecimal("150.01"), 0L),
                        "SHARED_ROUTE_NO_LONGER_MATCHES"),
                Arguments.of(
                        "pickup duration above threshold",
                        segment,
                        PassengerRoutePlanMother.request(),
                        PassengerRoutePlanMother.plan(),
                        new PickupDeviation(BigDecimal.ZERO, 901L),
                        "SHARED_ROUTE_NO_LONGER_MATCHES"),
                Arguments.of(
                        "same destination still has remaining leg",
                        preparation(LoaiGhepTuyen.CUNG_DIEM_DEN,
                                LoaiDiemTha.DIEM_DICH_CUOI_CUNG),
                        PassengerRoutePlanMother.request(),
                        PassengerRoutePlanMother.plan(),
                        new PickupDeviation(BigDecimal.ZERO, 0L),
                        "RIDE_REQUEST_ROUTE_NOT_COMPUTABLE"),
                Arguments.of(
                        "segment match has collapsed final leg",
                        segment,
                        PassengerRoutePlanMother.request(),
                        plan("3900.00", "3900.00", "0.00"),
                        new PickupDeviation(BigDecimal.ZERO, 0L),
                        "RIDE_REQUEST_ROUTE_NOT_COMPUTABLE"),
                Arguments.of(
                        "request waypoint order cannot extract served segment",
                        segment,
                        reversedSemanticRequest(),
                        PassengerRoutePlanMother.plan(),
                        new PickupDeviation(BigDecimal.ZERO, 0L),
                        "RIDE_REQUEST_ROUTE_NOT_COMPUTABLE"));
    }

    private static Stream<Arguments> missingArguments() {
        RideRequestPreparation preparation = RideRequestMother.segmentPreparation();
        RoutePlanRequest request = PassengerRoutePlanMother.request();
        RoutePlan plan = PassengerRoutePlanMother.plan();
        PickupDeviation deviation = new PickupDeviation(BigDecimal.ZERO, 0L);
        BigDecimal support = BigDecimal.ZERO;
        return Stream.of(
                Arguments.of(null, request, plan, deviation, support),
                Arguments.of(preparation, null, plan, deviation, support),
                Arguments.of(preparation, request, null, deviation, support),
                Arguments.of(preparation, request, plan, null, support),
                Arguments.of(preparation, request, plan, deviation, null));
    }


    private static RideRequestPreparation sameDestinationPreparation() {
        RideRequestPreparation source = RideRequestMother.segmentPreparation();
        return new RideRequestPreparation(
                source.routeId(),
                source.routeVersion(),
                source.driverId(),
                source.vehicleType(),
                source.expectedDepartureTime(),
                source.remainingSeats(),
                source.suggestedSupportPerKm(),
                LoaiGhepTuyen.CUNG_DIEM_DEN,
                LoaiDiemTha.DIEM_DICH_CUOI_CUNG,
                source.pickupProjection(),
                new RideRequestGeoPoint(
                        new BigDecimal("10.782120"),
                        new BigDecimal("106.712450")),
                source.policy(),
                source.consistencyToken());
    }

    private static RoutePlanRequest sameDestinationRequest() {
        return new RoutePlanRequest(
                List.of(
                        waypoint(RouteWaypointRole.PASSENGER_PICKUP, "10.776530", "106.700981"),
                        waypoint(RouteWaypointRole.PROPOSED_DROPOFF, "10.782120", "106.712450"),
                        waypoint(RouteWaypointRole.PASSENGER_DESTINATION, "10.782120", "106.712450")),
                RideRequestMother.segmentPreparation().vehicleType(),
                false);
    }

    private static RideRequestPreparation preparation(
            LoaiGhepTuyen matchType,
            LoaiDiemTha dropoffType) {
        RideRequestPreparation source = RideRequestMother.segmentPreparation();
        return new RideRequestPreparation(
                source.routeId(),
                source.routeVersion(),
                source.driverId(),
                source.vehicleType(),
                source.expectedDepartureTime(),
                source.remainingSeats(),
                source.suggestedSupportPerKm(),
                matchType,
                dropoffType,
                source.pickupProjection(),
                source.proposedDropoff(),
                source.policy(),
                source.consistencyToken());
    }

    private static RoutePlan plan(String total, String served, String remaining) {
        return planWithLegs(
                new BigDecimal(total),
                List.of(
                        leg(RouteWaypointRole.PASSENGER_PICKUP,
                                RouteWaypointRole.PROPOSED_DROPOFF, served, 550),
                        leg(RouteWaypointRole.PROPOSED_DROPOFF,
                                RouteWaypointRole.PASSENGER_DESTINATION, remaining,
                                new BigDecimal(remaining).signum() == 0 ? 0 : 50)));
    }

    private static RoutePlan planWithLegs(List<RoutePlanLeg> legs) {
        return planWithLegs(new BigDecimal("4200.00"), legs);
    }

    private static RoutePlan planWithLegs(BigDecimal total, List<RoutePlanLeg> legs) {
        LineString geometry = GEOMETRY_FACTORY.createLineString(new Coordinate[] {
                new Coordinate(106.700981, 10.776530),
                new Coordinate(106.706000, 10.779000),
                new Coordinate(106.711900, 10.781800),
                new Coordinate(106.712450, 10.782120)
        });
        geometry.setSRID(4326);
        return new RoutePlan(
                geometry,
                total,
                600L,
                legs,
                List.of(),
                new RouteBounds(
                        new BigDecimal("106.700981"),
                        new BigDecimal("10.776530"),
                        new BigDecimal("106.712450"),
                        new BigDecimal("10.782120")));
    }

    private static RoutePlanLeg leg(
            RouteWaypointRole from,
            RouteWaypointRole to,
            String distance,
            long duration) {
        return new RoutePlanLeg(1, from, to, new BigDecimal(distance), duration,
                new BigDecimal(distance).signum() == 0);
    }

    private static RoutePlanRequest reversedSemanticRequest() {
        return new RoutePlanRequest(
                List.of(
                        waypoint(RouteWaypointRole.PROPOSED_DROPOFF, "10.781800", "106.711900"),
                        waypoint(RouteWaypointRole.PASSENGER_PICKUP, "10.776530", "106.700981"),
                        waypoint(RouteWaypointRole.PASSENGER_DESTINATION, "10.782120", "106.712450")),
                RideRequestMother.segmentPreparation().vehicleType(),
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
}
