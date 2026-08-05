package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.config.properties.GoongProperties;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.RideRequestCreationRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.IdempotencyRecord;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCommitResult;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluation;
import com.zanh.route_sharing.service.LocationLabelResolver;
import com.zanh.route_sharing.service.riderequest.RideRequestExpiryPolicy;
import com.zanh.route_sharing.service.riderequest.RideRequestFingerprint;
import com.zanh.route_sharing.service.riderequest.RideRequestResponseMapper;
import com.zanh.route_sharing.service.riderequest.RideRequestSnapshotCalculator;
import com.zanh.route_sharing.service.riderequest.model.LocationLabel;
import com.zanh.route_sharing.service.riderequest.model.RideRequestCreationResult;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideRequestCreationServiceImplTest {

    private static final String KEY = "booking-501";
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private RideRequestCreationRepository repository;
    @Mock
    private RoutePlanner routePlanner;
    @Mock
    private LocationLabelResolver locationLabelResolver;

    private RideRequestFingerprint fingerprint;
    private RideRequestCreationServiceImpl sut;
    private CreateRideRequestRequest request;

    @BeforeEach
    void setUp() {
        fingerprint = new RideRequestFingerprint();
        GoongProperties properties = new GoongProperties();
        properties.setDuplicateWaypointToleranceMeters(new BigDecimal("2.00"));
        sut = new RideRequestCreationServiceImpl(
                repository,
                routePlanner,
                locationLabelResolver,
                new RideRequestSnapshotCalculator(
                        new RoutePlanSegmentExtractor(),
                        GEOMETRY_FACTORY),
                new RideRequestExpiryPolicy(),
                fingerprint,
                new RideRequestResponseMapper(),
                properties,
                Clock.fixed(RideRequestMother.NOW, ZoneOffset.UTC));
        request = new CreateRideRequestRequestBuilder().build();
    }

    @Test
    void givenEligibleCommand_whenCreating_thenSnapshotIsCommittedAndSeatRemainsUnreserved() {
        when(repository.findReplay(RideRequestMother.ACTOR_ID, KEY))
                .thenReturn(Optional.empty());
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
                .thenReturn(RideRequestCommitResult.created(RideRequestMother.persistedView()));

        RideRequestCreationResult result = sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                KEY,
                request);

        assertThat(result.replayed()).isFalse();
        assertThat(result.response().status()).isEqualTo(TrangThaiYeuCau.PENDING);
        assertThat(result.response().seatReserved()).isFalse();
        assertThat(result.response().agreedSupportAmount()).isNull();

        ArgumentCaptor<RideRequestCommitCommand> commandCaptor =
                ArgumentCaptor.forClass(RideRequestCommitCommand.class);
        verify(repository).commit(commandCaptor.capture());
        RideRequestCommitCommand command = commandCaptor.getValue();
        assertThat(command.sentAt()).isEqualTo(RideRequestMother.NOW);
        assertThat(command.expiresAt()).isEqualTo(RideRequestMother.NOW.plusSeconds(900));
        assertThat(command.snapshot().proposedSupportAmount()).isEqualByComparingTo("25000.00");
        assertThat(command.snapshot().pickupDeviationMeters()).isEqualByComparingTo("100.00");
        assertThat(command.snapshot().pickupDeviationSeconds()).isEqualTo(60L);
        assertThat(command.snapshot().proposedDropoff().address())
                .isEqualTo("Địa chỉ điểm thả");
    }

    @Test
    void givenEquivalentReplay_whenCreating_thenPersistedRepresentationIsReturnedWithoutExternalCalls() {
        String expectedFingerprint = fingerprint.calculate(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                request);
        when(repository.findReplay(RideRequestMother.ACTOR_ID, KEY))
                .thenReturn(Optional.of(new IdempotencyRecord(
                        RideRequestMother.ACTOR_ID,
                        KEY,
                        expectedFingerprint,
                        RideRequestMother.persistedView())));

        RideRequestCreationResult result = sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                KEY,
                request);

        assertThat(result.replayed()).isTrue();
        assertThat(result.response().rideRequestId()).isEqualTo(501L);
        verify(repository, never()).evaluate(any());
        verify(repository, never()).commit(any());
        verifyNoInteractions(routePlanner, locationLabelResolver);
    }

    @Test
    void givenSameKeyWithDifferentIntent_whenCreating_thenKeyReuseIsRejectedBeforeEvaluation() {
        when(repository.findReplay(RideRequestMother.ACTOR_ID, KEY))
                .thenReturn(Optional.of(new IdempotencyRecord(
                        RideRequestMother.ACTOR_ID,
                        KEY,
                        "0".repeat(64),
                        RideRequestMother.persistedView())));

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                KEY,
                request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED"));
        verify(repository, never()).evaluate(any());
        verifyNoInteractions(routePlanner, locationLabelResolver);
    }

    @Test
    void givenUnfinishedRequest_whenCreating_thenExternalProviderIsNotCalled() {
        when(repository.findReplay(RideRequestMother.ACTOR_ID, KEY))
                .thenReturn(Optional.empty());
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.unfinished(400L, TrangThaiYeuCau.ACCEPTED));

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                KEY,
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
        when(repository.findReplay(RideRequestMother.ACTOR_ID, KEY))
                .thenReturn(Optional.empty());
        when(repository.evaluate(any()))
                .thenReturn(RideRequestEvaluation.cooldown(cooldownUntil));

        assertThatThrownBy(() -> sut.create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                KEY,
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
}
