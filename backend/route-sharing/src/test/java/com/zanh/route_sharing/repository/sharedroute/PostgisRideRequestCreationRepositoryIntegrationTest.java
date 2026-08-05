package com.zanh.route_sharing.repository.sharedroute;

import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.RideRequestCreationRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCommitResult;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCriteria;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluation;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPreparation;
import com.zanh.route_sharing.service.riderequest.RideRequestSnapshotCalculator;
import com.zanh.route_sharing.service.riderequest.model.PickupDeviation;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RouteBounds;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanLeg;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture.Scenario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PostgisRideRequestCreationRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-05T03:00:00Z");
    private static final Instant DEPARTURE = NOW.plusSeconds(3600);

    @Autowired
    private RideRequestCreationRepository sut;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private RideRequestSnapshotCalculator snapshotCalculator;

    private SharedRouteSearchDatabaseFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new SharedRouteSearchDatabaseFixture(entityManager);
    }

    @Test
    void givenEligibleCommand_whenCommittingAndReplaying_thenExactlyOneAggregateIsPersistedAndSeatsStayUnchanged() {
        Scenario scenario = createScenario();
        RideRequestCommitCommand command = command(scenario, scenario.routeId(), "integration-key-001", "a");

        RideRequestCommitResult created = sut.commit(command);
        transactionTemplate.executeWithoutResult(status -> entityManager.createQuery(
                        "update YeuCauDiChung request "
                                + "set request.trangThaiYeuCau = :accepted, "
                                + "request.mucHoTroDaThoaThuan = :agreed "
                                + "where request.id = :requestId")
                .setParameter("accepted", TrangThaiYeuCau.ACCEPTED)
                .setParameter("agreed", new BigDecimal("27000.00"))
                .setParameter("requestId", created.persistedView().rideRequestId())
                .executeUpdate());
        RideRequestCommitResult replayed = sut.commit(command);

        assertThat(created.created()).isTrue();
        assertThat(replayed.created()).isFalse();
        assertThat(replayed.persistedView().rideRequestId())
                .isEqualTo(created.persistedView().rideRequestId());
        assertThat(replayed.persistedView().status()).isEqualTo(TrangThaiYeuCau.PENDING);
        assertThat(replayed.persistedView().agreedSupportAmount()).isNull();
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(count(
                    "select count(request) from YeuCauDiChung request "
                            + "where request.hanhKhach.id = :actorId",
                    scenario.actorId())).isEqualTo(1L);
            assertThat(count(
                    "select count(event) from NhatKyTrangThaiYeuCau event "
                            + "where event.yeuCauDiChung.id = :actorId",
                    created.persistedView().rideRequestId())).isEqualTo(1L);
            assertThat(count(
                    "select count(notification) from ThongBao notification "
                            + "where notification.doiTuongLienQuanId = :actorId "
                            + "and notification.loaiDoiTuongLienQuan = 'YEU_CAU_DI_CHUNG'",
                    created.persistedView().rideRequestId())).isEqualTo(1L);
            assertThat(fixture.remainingSeats(scenario.routeId())).isEqualTo(2);
        });
    }

    @Test
    void givenTwoConcurrentCommandsForDifferentRoutes_whenCommitting_thenOnlyOneBlockingRequestIsCreated()
            throws Exception {
        Scenario scenario = createScenario();
        Long secondRouteId = transactionTemplate.execute(status ->
                fixture.createAdditionalEquivalentRoute(scenario, DEPARTURE.plusSeconds(60)));
        RideRequestCommitCommand first = command(
                scenario,
                scenario.routeId(),
                "integration-key-101",
                "b");
        RideRequestCommitCommand second = command(
                scenario,
                secondRouteId,
                "integration-key-102",
                "c");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Object> firstResult = executor.submit(() -> commitAfterBarrier(first, ready, start));
            Future<Object> secondResult = executor.submit(() -> commitAfterBarrier(second, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> outcomes = List.of(
                    firstResult.get(20, TimeUnit.SECONDS),
                    secondResult.get(20, TimeUnit.SECONDS));

            assertThat(outcomes).filteredOn(RideRequestCommitResult.class::isInstance).hasSize(1);
            assertThat(outcomes).filteredOn(BusinessException.class::isInstance)
                    .singleElement()
                    .satisfies(outcome -> assertThat(((BusinessException) outcome).getCode())
                            .isEqualTo("UNFINISHED_RIDE_REQUEST_ALREADY_EXISTS"));
            transactionTemplate.executeWithoutResult(status -> {
                assertThat(count(
                        "select count(request) from YeuCauDiChung request "
                                + "where request.hanhKhach.id = :actorId",
                        scenario.actorId())).isEqualTo(1L);
                assertThat(fixture.remainingSeats(scenario.routeId())).isEqualTo(2);
                assertThat(fixture.remainingSeats(secondRouteId)).isEqualTo(2);
            });
        } finally {
            executor.shutdownNow();
        }
    }

    private Object commitAfterBarrier(
            RideRequestCommitCommand command,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            return sut.commit(command);
        } catch (BusinessException exception) {
            return exception;
        }
    }

    private Scenario createScenario() {
        return transactionTemplate.execute(status ->
                fixture.createStandardScenario(NOW, DEPARTURE));
    }

    private RideRequestCommitCommand command(
            Scenario scenario,
            Long routeId,
            String key,
            String fingerprintCharacter) {
        CreateRideRequestRequest request = request();
        RideRequestEvaluation evaluation = sut.evaluate(new RideRequestCriteria(
                scenario.actorId(),
                scenario.schoolId(),
                routeId,
                request.pickup().latitude(),
                request.pickup().longitude(),
                request.passengerDestination().latitude(),
                request.passengerDestination().longitude(),
                NOW));
        RideRequestPreparation preparation = evaluation.requirePreparation();
        RoutePlanRequest planRequest = planRequest(preparation, request);
        RoutePlan plan = passengerPlan(preparation, request);
        var snapshot = snapshotCalculator.calculate(
                preparation,
                request.pickup(),
                request.passengerDestination(),
                planRequest,
                plan,
                new PickupDeviation(new BigDecimal("12.00"), 30L),
                "Địa chỉ điểm thả integration",
                request.proposedSupportAmount());
        return new RideRequestCommitCommand(
                scenario.actorId(),
                routeId,
                key,
                fingerprintCharacter.repeat(64),
                NOW,
                NOW.plusSeconds(900),
                snapshot,
                request.note(),
                preparation.consistencyToken());
    }

    private static CreateRideRequestRequest request() {
        return new CreateRideRequestRequest(
                1L,
                new RouteEndpointRequest(
                        new BigDecimal("10.770100"),
                        new BigDecimal("106.690000"),
                        "Điểm đón integration"),
                new RouteEndpointRequest(
                        new BigDecimal("10.770100"),
                        new BigDecimal("106.705000"),
                        "Điểm đến integration"),
                new BigDecimal("25000.00"),
                null);
    }

    private static RoutePlanRequest planRequest(
            RideRequestPreparation preparation,
            CreateRideRequestRequest request) {
        return new RoutePlanRequest(
                List.of(
                        waypoint(RouteWaypointRole.PASSENGER_PICKUP,
                                request.pickup().latitude(), request.pickup().longitude()),
                        waypoint(RouteWaypointRole.PROPOSED_DROPOFF,
                                preparation.proposedDropoff().latitude(),
                                preparation.proposedDropoff().longitude()),
                        waypoint(RouteWaypointRole.PASSENGER_DESTINATION,
                                request.passengerDestination().latitude(),
                                request.passengerDestination().longitude())),
                preparation.vehicleType(),
                false);
    }

    private static RoutePlan passengerPlan(
            RideRequestPreparation preparation,
            CreateRideRequestRequest request) {
        LineString geometry = new GeometryFactory(new org.locationtech.jts.geom.PrecisionModel(), 4326)
                .createLineString(new Coordinate[] {
                        new Coordinate(
                                request.pickup().longitude().doubleValue(),
                                request.pickup().latitude().doubleValue()),
                        new Coordinate(
                                preparation.proposedDropoff().longitude().doubleValue(),
                                preparation.proposedDropoff().latitude().doubleValue()),
                        new Coordinate(
                                request.passengerDestination().longitude().doubleValue(),
                                request.passengerDestination().latitude().doubleValue())
                });
        geometry.setSRID(4326);
        return new RoutePlan(
                geometry,
                new BigDecimal("1510.00"),
                310L,
                List.of(
                        new RoutePlanLeg(
                                1,
                                RouteWaypointRole.PASSENGER_PICKUP,
                                RouteWaypointRole.PROPOSED_DROPOFF,
                                new BigDecimal("1500.00"),
                                300L,
                                false),
                        new RoutePlanLeg(
                                2,
                                RouteWaypointRole.PROPOSED_DROPOFF,
                                RouteWaypointRole.PASSENGER_DESTINATION,
                                new BigDecimal("10.00"),
                                10L,
                                false)),
                List.of(),
                new RouteBounds(
                        new BigDecimal("106.690000"),
                        new BigDecimal("10.770000"),
                        new BigDecimal("106.705000"),
                        new BigDecimal("10.770100")));
    }

    private static RouteWaypoint waypoint(
            RouteWaypointRole role,
            BigDecimal latitude,
            BigDecimal longitude) {
        return new RouteWaypoint(role, new GeoCoordinate(latitude, longitude));
    }

    private long count(String jpql, Long id) {
        return entityManager.createQuery(jpql, Long.class)
                .setParameter("actorId", id)
                .getSingleResult();
    }
}
