package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.config.properties.GoongProperties;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.dto.riderequest.RideRequestResponse;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.RideRequestCreationRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCriteria;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluation;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluationStatus;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestGeoPoint;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPreparation;
import com.zanh.route_sharing.service.LocationLabelResolver;
import com.zanh.route_sharing.service.riderequest.RideRequestResponseMapper;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.service.realtime.model.RealtimeEventEnvelope;
import com.zanh.route_sharing.service.riderequest.RideRequestSnapshotCalculator;
import com.zanh.route_sharing.service.riderequest.model.LocationLabel;
import com.zanh.route_sharing.service.routing.RoutePlanSegmentExtractor;
import com.zanh.route_sharing.service.routing.RoutePlanner;
import com.zanh.route_sharing.service.routing.model.RouteBounds;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanLeg;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import com.zanh.route_sharing.testsupport.riderequest.CreateRideRequestRequestBuilder;
import com.zanh.route_sharing.testsupport.riderequest.RideRequestMother;
import com.zanh.route_sharing.testsupport.routing.PassengerRoutePlanMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideRequestCreationServiceImplTest {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private RideRequestCreationRepository repository;
    @Mock
    private RoutePlanner routePlanner;
    @Mock
    private LocationLabelResolver locationLabelResolver;
    @Mock
    private UserRealtimeEventPublisher realtimeEventPublisher;

    private RideRequestCreationServiceImpl sut;
    private CreateRideRequestRequest request;

    @BeforeEach
    void setUp() {
        GoongProperties properties = new GoongProperties();
        properties.setDuplicateWaypointToleranceMeters(new BigDecimal("2.00"));
        sut = new RideRequestCreationServiceImpl(
                repository,
                routePlanner,
                locationLabelResolver,
                new RideRequestSnapshotCalculator(
                        new RoutePlanSegmentExtractor(),
                        GEOMETRY_FACTORY),
                new RideRequestResponseMapper(),
                properties,
                Clock.fixed(RideRequestMother.NOW, ZoneOffset.UTC),
                realtimeEventPublisher);
        request = new CreateRideRequestRequestBuilder().build();
    }

    @Test
    void givenEligibleSegmentCommand_whenCreating_thenSnapshotIsCommittedAndSeatRemainsUnreserved() {
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.eligible(RideRequestMother.segmentPreparation()));
        when(routePlanner.plan(any())).thenAnswer(invocation -> {
            RoutePlanRequest planned = invocation.getArgument(0);
            return planned.waypoints().get(0).role() == RouteWaypointRole.PICKUP_PROJECTION
                    ? pickupDeviationPlan()
                    : PassengerRoutePlanMother.plan();
        });
        when(locationLabelResolver.resolve(any()))
                .thenReturn(new LocationLabel("Địa chỉ điểm thả"));
        when(repository.commit(any()))
                .thenReturn(RideRequestMother.persistedView());

        RideRequestResponse result = sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                request);

        assertThat(result.status()).isEqualTo(TrangThaiYeuCau.PENDING);
        assertThat(result.seatReserved()).isFalse();
        assertThat(result.agreedSupportAmount()).isNull();

        ArgumentCaptor<RideRequestCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(RideRequestCriteria.class);
        verify(repository).evaluate(criteriaCaptor.capture());
        RideRequestCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.actorUserId()).isEqualTo(RideRequestMother.ACTOR_ID);
        assertThat(criteria.schoolId()).isEqualTo(request.schoolId());
        assertThat(criteria.routeId()).isEqualTo(RideRequestMother.ROUTE_ID);
        assertThat(criteria.now()).isEqualTo(RideRequestMother.NOW);

        ArgumentCaptor<RideRequestCommitCommand> commandCaptor =
                ArgumentCaptor.forClass(RideRequestCommitCommand.class);
        verify(repository).commit(commandCaptor.capture());
        RideRequestCommitCommand command = commandCaptor.getValue();
        assertThat(command.sentAt()).isEqualTo(RideRequestMother.NOW);
        assertThat(command.snapshot().proposedSupportAmount()).isEqualByComparingTo("25000.00");
        assertThat(command.snapshot().pickupDeviationMeters()).isEqualByComparingTo("100.00");
        assertThat(command.snapshot().pickupDeviationSeconds()).isEqualTo(60L);
        assertThat(command.snapshot().proposedDropoff().address())
                .isEqualTo("Địa chỉ điểm thả");
        assertThat(command.snapshot().agreedSupportAmount()).isNull();

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<RealtimeEventEnvelope> realtimeCaptor =
                ArgumentCaptor.forClass(RealtimeEventEnvelope.class);
        verify(realtimeEventPublisher).publish(
                org.mockito.ArgumentMatchers.eq(RideRequestMother.DRIVER_ID),
                realtimeCaptor.capture());
        assertThat(realtimeCaptor.getValue().eventType()).isEqualTo("BOOKING_REQUEST");
        assertThat(realtimeCaptor.getValue().eventVersion()).isEqualTo(1);
        assertThat(realtimeCaptor.getValue().resource().type()).isEqualTo("RIDE_REQUEST");
        assertThat(realtimeCaptor.getValue().resource().id()).isEqualTo(501L);
    }

    @Test
    void givenSameDestinationMatch_whenCreating_thenPassengerDestinationAddressIsReusedWithoutGeocoding() {
        RideRequestPreparation preparation = sameDestinationPreparation();
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.eligible(preparation));
        when(routePlanner.plan(any())).thenAnswer(invocation -> {
            RoutePlanRequest planned = invocation.getArgument(0);
            return planned.waypoints().get(0).role() == RouteWaypointRole.PICKUP_PROJECTION
                    ? pickupDeviationPlan()
                    : sameDestinationPlan();
        });
        when(repository.commit(any()))
                .thenReturn(RideRequestMother.persistedView());

        sut.create(RideRequestMother.ACTOR_ID, RideRequestMother.ROUTE_ID, request);

        ArgumentCaptor<RideRequestCommitCommand> commandCaptor =
                ArgumentCaptor.forClass(RideRequestCommitCommand.class);
        verify(repository).commit(commandCaptor.capture());
        assertThat(commandCaptor.getValue().snapshot().matchType())
                .isEqualTo(LoaiGhepTuyen.CUNG_DIEM_DEN);
        assertThat(commandCaptor.getValue().snapshot().dropoffType())
                .isEqualTo(LoaiDiemTha.DIEM_DICH_CUOI_CUNG);
        assertThat(commandCaptor.getValue().snapshot().proposedDropoff().address())
                .isEqualTo(request.passengerDestination().address());
        assertThat(commandCaptor.getValue().snapshot().remainingDistanceMeters())
                .isZero();
        verifyNoInteractions(locationLabelResolver);
    }

    @Test
    void givenPickupProjectionWithinTolerance_whenCreating_thenDeviationIsZeroAndNoDeviationRouteIsRequested() {
        RideRequestPreparation preparation = preparationWithPickupProjection(
                request.pickup().latitude(),
                request.pickup().longitude());
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.eligible(preparation));
        when(routePlanner.plan(any())).thenReturn(PassengerRoutePlanMother.plan());
        when(locationLabelResolver.resolve(any()))
                .thenReturn(new LocationLabel("Địa chỉ điểm thả"));
        when(repository.commit(any()))
                .thenReturn(RideRequestMother.persistedView());

        sut.create(RideRequestMother.ACTOR_ID, RideRequestMother.ROUTE_ID, request);

        ArgumentCaptor<RoutePlanRequest> routeRequestCaptor =
                ArgumentCaptor.forClass(RoutePlanRequest.class);
        verify(routePlanner, times(1)).plan(routeRequestCaptor.capture());
        assertThat(routeRequestCaptor.getValue().waypoints())
                .extracting(waypoint -> waypoint.role())
                .containsExactly(
                        RouteWaypointRole.PASSENGER_PICKUP,
                        RouteWaypointRole.PROPOSED_DROPOFF,
                        RouteWaypointRole.PASSENGER_DESTINATION);

        ArgumentCaptor<RideRequestCommitCommand> commandCaptor =
                ArgumentCaptor.forClass(RideRequestCommitCommand.class);
        verify(repository).commit(commandCaptor.capture());
        assertThat(commandCaptor.getValue().snapshot().pickupDeviationMeters()).isZero();
        assertThat(commandCaptor.getValue().snapshot().pickupDeviationSeconds()).isZero();
    }

    @ParameterizedTest(name = "{0} -> {1} {2}")
    @MethodSource("ineligibleEvaluations")
    void givenIneligibleEvaluation_whenCreating_thenStableBusinessErrorIsReturnedWithoutExternalCalls(
            RideRequestEvaluationStatus status,
            HttpStatus expectedStatus,
            String expectedCode) {
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.ineligible(status));

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(expectedStatus);
                    assertThat(exception.getCode()).isEqualTo(expectedCode);
                });
        verifyNoInteractions(routePlanner, locationLabelResolver);
        verify(repository, never()).commit(any());
    }

    @Test
    void givenUnfinishedRequest_whenCreating_thenExistingRequestDetailsAreReturnedWithoutExternalCalls() {
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.unfinished(400L, TrangThaiYeuCau.ACCEPTED));

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo("UNFINISHED_RIDE_REQUEST_ALREADY_EXISTS");
                    assertThat(exception.getErrors())
                            .containsEntry("existingRideRequestId", "400")
                            .containsEntry("status", "ACCEPTED");
                });
        verifyNoInteractions(routePlanner, locationLabelResolver);
        verify(repository, never()).commit(any());
    }

    @Test
    void givenDriverCooldown_whenCreating_thenCooldownUntilIsReturnedWithoutExternalCalls() {
        Instant cooldownUntil = RideRequestMother.NOW.plusSeconds(3600);
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.cooldown(cooldownUntil));

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode())
                            .isEqualTo("RIDE_REQUEST_REJECTION_COOLDOWN_ACTIVE");
                    assertThat(exception.getErrors())
                            .containsEntry("cooldownUntil", cooldownUntil.toString());
                });
        verifyNoInteractions(routePlanner, locationLabelResolver);
        verify(repository, never()).commit(any());
    }

    @Test
    void givenPlannerRejectsDuplicateWaypoints_whenCreating_thenInvalidRideRequestIsReturned() {
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.eligible(RideRequestMother.segmentPreparation()));
        when(routePlanner.plan(any())).thenThrow(new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_ROUTE_WAYPOINTS",
                "provider-neutral validation"));

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("INVALID_RIDE_REQUEST");
                });
        verify(repository, never()).commit(any());
    }

    @Test
    void givenPlannerCannotFindRoute_whenCreating_thenRideRequestRouteNotComputableIsReturned() {
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.eligible(RideRequestMother.segmentPreparation()));
        when(routePlanner.plan(any())).thenThrow(new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "ROUTE_NOT_FOUND",
                "provider-neutral route not found"));

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(exception.getCode()).isEqualTo("RIDE_REQUEST_ROUTE_NOT_COMPUTABLE");
                });
        verify(repository, never()).commit(any());
    }

    @Test
    void givenPlannerInfrastructureFailure_whenCreating_thenOriginalProviderErrorIsPropagated() {
        BusinessException providerFailure = new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ROUTING_PROVIDER_UNAVAILABLE",
                "Dịch vụ định tuyến không khả dụng.");
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.eligible(RideRequestMother.segmentPreparation()));
        when(routePlanner.plan(any())).thenThrow(providerFailure);

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                request)).isSameAs(providerFailure);
        verify(repository, never()).commit(any());
    }

    @Test
    void givenLocationLabelProviderFailure_whenCreatingSegmentRequest_thenFailureIsPropagatedWithoutCommit() {
        BusinessException providerFailure = new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "LOCATION_LABEL_PROVIDER_UNAVAILABLE",
                "Dịch vụ địa chỉ bản đồ không khả dụng.");
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.eligible(RideRequestMother.segmentPreparation()));
        when(routePlanner.plan(any())).thenAnswer(invocation -> {
            RoutePlanRequest planned = invocation.getArgument(0);
            return planned.waypoints().get(0).role() == RouteWaypointRole.PICKUP_PROJECTION
                    ? pickupDeviationPlan()
                    : PassengerRoutePlanMother.plan();
        });
        when(locationLabelResolver.resolve(any())).thenThrow(providerFailure);

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                request)).isSameAs(providerFailure);
        verify(repository, never()).commit(any());
    }

    @Test
    void givenPickupAndDestinationAreTheSame_whenCreating_thenInvalidRequestIsReturnedBeforeDependencies() {
        CreateRideRequestRequest sameEndpoints = new CreateRideRequestRequestBuilder()
                .withDestination(request.pickup())
                .build();

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                sameEndpoints))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("INVALID_RIDE_REQUEST");
                });
        verifyNoInteractions(repository, routePlanner, locationLabelResolver);
    }

    @Test
    void givenMissingAuthenticatedActor_whenCreating_thenUnauthorizedErrorIsReturnedBeforeDependencies() {
        assertThatThrownBy(() -> sut.create(null, RideRequestMother.ROUTE_ID, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getCode()).isEqualTo("AUTHENTICATED_USER_REQUIRED");
                });
        verifyNoInteractions(repository, routePlanner, locationLabelResolver);
    }

    @Test
    void givenInvalidRouteId_whenCreating_thenInvalidRideRequestIsReturnedBeforeDependencies() {
        assertThatThrownBy(() -> sut.create(RideRequestMother.ACTOR_ID, 0L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("INVALID_RIDE_REQUEST");
                });
        verifyNoInteractions(repository, routePlanner, locationLabelResolver);
    }

    @Test
    void givenNullRequest_whenCreating_thenInvalidRideRequestIsReturnedBeforeDependencies() {
        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_RIDE_REQUEST"));
        verifyNoInteractions(repository, routePlanner, locationLabelResolver);
    }

    @Test
    void givenInvalidRequestCalledOutsideHttpBoundary_whenCreating_thenApplicationGuardRejectsIt() {
        CreateRideRequestRequest invalid = new CreateRideRequestRequestBuilder()
                .withProposedSupportAmount(new BigDecimal("-0.01"))
                .build();

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                invalid))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_RIDE_REQUEST"));
        verifyNoInteractions(repository, routePlanner, locationLabelResolver);
    }

    @Test
    void givenCoincidentPickupAndDestination_whenCreating_thenInvalidRideRequestIsReturnedBeforeDependencies() {
        RouteEndpointRequest pickup = request.pickup();
        CreateRideRequestRequest coincident = new CreateRideRequestRequestBuilder()
                .withDestination(new RouteEndpointRequest(
                        pickup.latitude(),
                        pickup.longitude(),
                        "Cùng tọa độ nhưng địa chỉ khác"))
                .build();

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                coincident))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("INVALID_RIDE_REQUEST");
                    assertThat(exception.getMessage()).contains("phải khác nhau");
                });
        verifyNoInteractions(repository, routePlanner, locationLabelResolver);
    }

    private void givenEligibleSegmentFlow() {
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.eligible(RideRequestMother.segmentPreparation()));
        when(routePlanner.plan(any())).thenAnswer(invocation -> {
            RoutePlanRequest planned = invocation.getArgument(0);
            return planned.waypoints().get(0).role() == RouteWaypointRole.PICKUP_PROJECTION
                    ? pickupDeviationPlan()
                    : PassengerRoutePlanMother.plan();
        });
        when(locationLabelResolver.resolve(any()))
                .thenReturn(new LocationLabel("Địa chỉ điểm thả"));
    }

    private static Stream<Arguments> ineligibleEvaluations() {
        return Stream.of(
                Arguments.of(
                        RideRequestEvaluationStatus.NOT_FOUND_OR_INACCESSIBLE,
                        HttpStatus.NOT_FOUND,
                        "SHARED_ROUTE_NOT_FOUND"),
                Arguments.of(
                        RideRequestEvaluationStatus.ROUTE_UNAVAILABLE,
                        HttpStatus.CONFLICT,
                        "SHARED_ROUTE_UNAVAILABLE"),
                Arguments.of(
                        RideRequestEvaluationStatus.SELF_ROUTE,
                        HttpStatus.CONFLICT,
                        "SHARED_ROUTE_UNAVAILABLE"),
                Arguments.of(
                        RideRequestEvaluationStatus.DRIVER_OR_VEHICLE_INELIGIBLE,
                        HttpStatus.CONFLICT,
                        "SHARED_ROUTE_UNAVAILABLE"),
                Arguments.of(
                        RideRequestEvaluationStatus.NO_LONGER_MATCHES,
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "SHARED_ROUTE_NO_LONGER_MATCHES"));
    }

    private static RideRequestPreparation preparationWithPickupProjection(
            BigDecimal latitude,
            BigDecimal longitude) {
        RideRequestPreparation source = RideRequestMother.segmentPreparation();
        return new RideRequestPreparation(
                source.routeId(),
                source.routeVersion(),
                source.driverId(),
                source.vehicleType(),
                source.expectedDepartureTime(),
                source.remainingSeats(),
                source.suggestedSupportPerKm(),
                source.matchType(),
                source.dropoffType(),
                new RideRequestGeoPoint(latitude, longitude),
                source.proposedDropoff(),
                source.policy(),
                source.consistencyToken());
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

    private static RoutePlan pickupDeviationPlan() {
        LineString geometry = GEOMETRY_FACTORY.createLineString(new Coordinate[] {
                new Coordinate(106.700900, 10.776500),
                new Coordinate(106.700981, 10.776530)
        });
        geometry.setSRID(4326);
        return new RoutePlan(
                geometry,
                new BigDecimal("100.00"),
                60L,
                List.of(new RoutePlanLeg(
                        1,
                        RouteWaypointRole.PICKUP_PROJECTION,
                        RouteWaypointRole.PASSENGER_PICKUP,
                        new BigDecimal("100.00"),
                        60L,
                        false)),
                List.of(),
                new RouteBounds(
                        new BigDecimal("106.700900"),
                        new BigDecimal("10.776500"),
                        new BigDecimal("106.700981"),
                        new BigDecimal("10.776530")));
    }

    private static RoutePlan sameDestinationPlan() {
        LineString geometry = GEOMETRY_FACTORY.createLineString(new Coordinate[] {
                new Coordinate(106.700981, 10.776530),
                new Coordinate(106.706000, 10.779000),
                new Coordinate(106.712450, 10.782120)
        });
        geometry.setSRID(4326);
        return new RoutePlan(
                geometry,
                new BigDecimal("3900.00"),
                550L,
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
                                BigDecimal.ZERO,
                                0L,
                                true)),
                List.of(),
                new RouteBounds(
                        new BigDecimal("106.700981"),
                        new BigDecimal("10.776530"),
                        new BigDecimal("106.712450"),
                        new BigDecimal("10.782120")));
    }
}
