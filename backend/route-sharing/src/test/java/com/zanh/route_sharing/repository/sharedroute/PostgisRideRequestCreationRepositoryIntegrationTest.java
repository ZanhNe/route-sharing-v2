package com.zanh.route_sharing.repository.sharedroute;

import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.RideRequestCreationRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPersistedView;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCriteria;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluation;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluationStatus;
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
import com.zanh.route_sharing.testsupport.riderequest.decision.RideRequestDecisionMother;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture.Scenario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        void givenEligibleCommand_whenCommitting_thenAggregateAuditNotificationAndSeatInvariantArePersisted() {
                Scenario scenario = createScenario();
                RideRequestCommitCommand command = command(scenario, scenario.routeId());

                RideRequestPersistedView created = sut.commit(command);

                assertThat(created.status()).isEqualTo(TrangThaiYeuCau.PENDING);
                assertThat(created.agreedSupportAmount()).isNull();
                transactionTemplate.executeWithoutResult(status -> {
                        assertThat(count(
                                        "select count(request) from YeuCauDiChung request "
                                                        + "where request.hanhKhach.id = :actorId",
                                        scenario.actorId())).isEqualTo(1L);
                        assertThat(count(
                                        "select count(event) from NhatKyTrangThaiYeuCau event "
                                                        + "where event.yeuCauDiChung.id = :actorId",
                                        created.rideRequestId())).isEqualTo(1L);
                        assertThat(count(
                                        "select count(notification) from ThongBao notification "
                                                        + "where notification.doiTuongLienQuanId = :actorId "
                                                        + "and notification.loaiDoiTuongLienQuan = 'YEU_CAU_DI_CHUNG'",
                                        created.rideRequestId())).isEqualTo(1L);
                        assertThat(fixture.remainingSeats(scenario.routeId())).isEqualTo(2);
                });
        }

        @Test
        void givenTwoConcurrentCommandsForDifferentRoutes_whenCommitting_thenOnlyOneBlockingRequestIsCreated()
                        throws Exception {
                Scenario scenario = createScenario();
                Long secondRouteId = transactionTemplate.execute(
                                status -> fixture.createAdditionalEquivalentRoute(scenario, DEPARTURE.plusSeconds(60)));
                RideRequestCommitCommand first = command(scenario, scenario.routeId());
                RideRequestCommitCommand second = command(scenario, secondRouteId);

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

                        assertThat(outcomes).filteredOn(RideRequestPersistedView.class::isInstance).hasSize(1);
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

        @Test
        void givenCurrentEligibleRoute_whenEvaluating_thenPreparationContainsCurrentPolicyAndConsistencyToken() {
                Scenario scenario = createScenario();

                RideRequestEvaluation result = sut.evaluate(criteria(scenario, scenario.routeId(), request()));

                assertThat(result.status()).isEqualTo(RideRequestEvaluationStatus.ELIGIBLE);
                RideRequestPreparation preparation = result.requirePreparation();
                assertThat(preparation.routeId()).isEqualTo(scenario.routeId());
                assertThat(preparation.driverId()).isEqualTo(scenario.driverId());
                assertThat(preparation.remainingSeats()).isEqualTo(2);
                assertThat(preparation.policy().configurationId()).isEqualTo(scenario.configurationId());
                assertThat(preparation.policy().bookingCutoff().toSeconds()).isEqualTo(900L);
                assertThat(preparation.consistencyToken().routeId()).isEqualTo(scenario.routeId());
                assertThat(rideRequestCount(scenario.actorId())).isZero();
                assertThat(remainingSeats(scenario.routeId())).isEqualTo(2);
        }

        @ParameterizedTest
        @EnumSource(SharedRouteSearchDatabaseFixture.IneligibleMutation.class)
        void givenRouteDriverOrVehicleBecomesIneligible_whenEvaluating_thenStableEvaluationStatusIsReturned(
                        SharedRouteSearchDatabaseFixture.IneligibleMutation mutation) {
                Scenario scenario = createScenario();
                transactionTemplate.executeWithoutResult(
                                status -> fixture.applyIneligibleMutation(scenario, mutation, NOW));

                RideRequestEvaluation result = sut.evaluate(criteria(scenario, scenario.routeId(), request()));

                RideRequestEvaluationStatus expected = switch (mutation) {
                        case CLOSED_ROUTE, NO_REMAINING_SEATS, DEPARTED_ROUTE ->
                                RideRequestEvaluationStatus.ROUTE_UNAVAILABLE;
                        case DRIVER_INACTIVE, DRIVER_PROFILE_INACTIVE, VEHICLE_INACTIVE ->
                                RideRequestEvaluationStatus.DRIVER_OR_VEHICLE_INELIGIBLE;
                        case DRIVER_MEMBERSHIP_EXPIRED ->
                                RideRequestEvaluationStatus.NOT_FOUND_OR_INACCESSIBLE;
                };
                assertThat(result.status()).isEqualTo(expected);
                assertThat(rideRequestCount(scenario.actorId())).isZero();
        }

        @Test
        void givenActorOwnsTheRoute_whenEvaluating_thenSelfRouteIsRejected() {
                Scenario scenario = createScenario();
                transactionTemplate.executeWithoutResult(status -> fixture.makeRouteOwnedByActor(scenario, NOW));

                RideRequestEvaluation result = sut.evaluate(criteria(scenario, scenario.routeId(), request()));

                assertThat(result.status()).isEqualTo(RideRequestEvaluationStatus.SELF_ROUTE);
                assertThat(rideRequestCount(scenario.actorId())).isZero();
        }

        @Test
        void givenActorMembershipExpired_whenEvaluating_thenRouteIsHiddenAsNotFoundOrInaccessible() {
                Scenario scenario = createScenario();
                transactionTemplate.executeWithoutResult(status -> fixture.expireActorBeforeRouteDate(scenario));

                RideRequestEvaluation result = sut.evaluate(criteria(scenario, scenario.routeId(), request()));

                assertThat(result.status())
                                .isEqualTo(RideRequestEvaluationStatus.NOT_FOUND_OR_INACCESSIBLE);
        }

        @Test
        void givenDestinationNoLongerMatchesRoute_whenEvaluating_thenNoLongerMatchesIsReturned() {
                Scenario scenario = createScenario();
                CreateRideRequestRequest farDestination = new CreateRideRequestRequest(
                                scenario.schoolId(),
                                request().pickup(),
                                new RouteEndpointRequest(
                                                new BigDecimal("11.500000"),
                                                new BigDecimal("108.500000"),
                                                "Điểm đến không còn gần tuyến"),
                                new BigDecimal("25000.00"),
                                null);

                RideRequestEvaluation result = sut.evaluate(
                                criteria(scenario, scenario.routeId(), farDestination));

                assertThat(result.status()).isEqualTo(RideRequestEvaluationStatus.NO_LONGER_MATCHES);
        }

        @Test
        void givenRejectedRequestOnSameRouteWithinCooldown_whenEvaluating_thenCooldownBlocksOnlyThatRoute() {
                Scenario scenario = createScenario();
                Instant rejectedAt = NOW.minusSeconds(60);
                Instant expectedCooldownUntil = transactionTemplate.execute(
                                status -> persistRejectedRequest(scenario, scenario.routeId(), rejectedAt, 3600L));

                RideRequestEvaluation result = sut.evaluate(criteria(scenario, scenario.routeId(), request()));

                assertThat(result.status()).isEqualTo(RideRequestEvaluationStatus.REJECTION_COOLDOWN_ACTIVE);
                assertThat(result.cooldownUntil()).isEqualTo(expectedCooldownUntil);
                assertThat(rideRequestCount(scenario.actorId())).isEqualTo(1L);
        }

        @Test
        void givenRejectedRequestOnFirstRouteWithinCooldown_whenEvaluatingSecondRouteOfSameDriver_thenSecondRouteRemainsEligible() {
                Scenario scenario = createScenario();
                Long secondRouteId = transactionTemplate.execute(
                                status -> fixture.createAdditionalEquivalentRoute(scenario, DEPARTURE.plusSeconds(60)));
                transactionTemplate.executeWithoutResult(status -> persistRejectedRequest(scenario, scenario.routeId(),
                                NOW.minusSeconds(60), 3600L));

                RideRequestEvaluation result = sut.evaluate(criteria(scenario, secondRouteId, request()));

                assertThat(result.status()).isEqualTo(RideRequestEvaluationStatus.ELIGIBLE);
                assertThat(result.requirePreparation().routeId()).isEqualTo(secondRouteId);
        }

        @Test
        void givenRejectedRequestCooldownElapsed_whenEvaluatingSameRoute_thenRetryIsEligible() {
                Scenario scenario = createScenario();
                transactionTemplate.executeWithoutResult(status -> persistRejectedRequest(scenario, scenario.routeId(),
                                NOW.minusSeconds(3601), 3600L));

                RideRequestEvaluation result = sut.evaluate(criteria(scenario, scenario.routeId(), request()));

                assertThat(result.status()).isEqualTo(RideRequestEvaluationStatus.ELIGIBLE);
        }

        @Test
        void givenConfigurationChangesAfterReject_whenEvaluatingSameRoute_thenStoredCooldownUntilRemainsAuthoritative() {
                Scenario scenario = createScenario();
                Instant rejectedAt = NOW.minusSeconds(60);
                Instant expectedCooldownUntil = transactionTemplate.execute(
                                status -> persistRejectedRequest(scenario, scenario.routeId(), rejectedAt, 1800L));
                transactionTemplate.executeWithoutResult(status -> {
                        CauHinhNghiepVu configuration = entityManager.find(
                                        CauHinhNghiepVu.class, scenario.configurationId());
                        configuration.setRejectionCooldownSeconds(7200L);
                });

                RideRequestEvaluation result = sut.evaluate(criteria(scenario, scenario.routeId(), request()));

                assertThat(result.status()).isEqualTo(RideRequestEvaluationStatus.REJECTION_COOLDOWN_ACTIVE);
                assertThat(result.cooldownUntil()).isEqualTo(expectedCooldownUntil);
                assertThat(expectedCooldownUntil).isEqualTo(rejectedAt.plusSeconds(1800L));
        }

        @Test
        void givenSameRouteCooldownAppearsAfterPreparation_whenCommitting_thenCommitRecheckBlocksCreation() {
                Scenario scenario = createScenario();
                RideRequestCommitCommand preparedCommand = command(scenario, scenario.routeId());
                Instant expectedCooldownUntil = transactionTemplate.execute(status -> persistRejectedRequest(scenario,
                                scenario.routeId(), NOW.minusSeconds(30), 3600L));

                assertThatThrownBy(() -> sut.commit(preparedCommand))
                                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                                        assertThat(exception.getCode())
                                                        .isEqualTo("RIDE_REQUEST_REJECTION_COOLDOWN_ACTIVE");
                                        assertThat(exception.getErrors())
                                                        .containsEntry("cooldownUntil",
                                                                        expectedCooldownUntil.toString());
                                });
                assertThat(rideRequestCount(scenario.actorId())).isEqualTo(1L);
                assertThat(remainingSeats(scenario.routeId())).isEqualTo(2);
        }

        @Test
        void givenExistingBlockingRequest_whenEvaluatingOrCommittingAgain_thenSameBlockingRequestIsReported() {
                Scenario scenario = createScenario();
                RideRequestCommitCommand first = command(scenario, scenario.routeId());
                Long existingId = sut.commit(first).rideRequestId();

                RideRequestEvaluation evaluation = sut.evaluate(
                                criteria(scenario, scenario.routeId(), request()));
                assertThat(evaluation.status())
                                .isEqualTo(RideRequestEvaluationStatus.UNFINISHED_REQUEST_EXISTS);
                assertThat(evaluation.existingRideRequestId()).isEqualTo(existingId);
                assertThat(evaluation.existingStatus()).isEqualTo(TrangThaiYeuCau.PENDING);

                assertThatThrownBy(() -> sut.commit(first))
                                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                                        assertThat(exception.getCode())
                                                        .isEqualTo("UNFINISHED_RIDE_REQUEST_ALREADY_EXISTS");
                                        assertThat(exception.getErrors())
                                                        .containsEntry("existingRideRequestId", existingId.toString())
                                                        .containsEntry("status", "PENDING");
                                });
                assertThat(rideRequestCount(scenario.actorId())).isEqualTo(1L);
        }

        @Test
        void givenCommandDoesNotMatchConsistencyToken_whenCommitting_thenStaleConflictIsReturnedWithoutPersistence() {
                Scenario scenario = createScenario();
                RideRequestCommitCommand valid = command(scenario, scenario.routeId());
                RideRequestCommitCommand stale = new RideRequestCommitCommand(
                                valid.actorUserId(),
                                valid.routeId() + 999L,
                                valid.sentAt(),
                                valid.snapshot(),
                                valid.note(),
                                valid.consistencyToken());

                assertThatThrownBy(() -> sut.commit(stale))
                                .isInstanceOfSatisfying(BusinessException.class,
                                                exception -> assertThat(exception.getCode())
                                                                .isEqualTo("RIDE_REQUEST_STALE"));
                assertThat(rideRequestCount(scenario.actorId())).isZero();
                assertThat(remainingSeats(scenario.routeId())).isEqualTo(2);
        }

        @Test
        void givenCommandAtBookingCutoff_whenCommitting_thenCutoffConflictIsReturned() {
                Scenario scenario = createScenario();
                RideRequestCommitCommand valid = command(scenario, scenario.routeId());
                RideRequestCommitCommand atCutoff = new RideRequestCommitCommand(
                                valid.actorUserId(),
                                valid.routeId(),
                                valid.snapshot().expectedDepartureTime().minusSeconds(900L),
                                valid.snapshot(),
                                valid.note(),
                                valid.consistencyToken());

                assertThatThrownBy(() -> sut.commit(atCutoff))
                                .isInstanceOfSatisfying(BusinessException.class,
                                                exception -> assertThat(exception.getCode())
                                                                .isEqualTo("SHARED_ROUTE_BOOKING_CUTOFF_REACHED"));
                assertThat(rideRequestCount(scenario.actorId())).isZero();
        }

        @Test
        void givenTwoConcurrentDuplicateCommands_whenCommitting_thenOneCreatesAndOneIsRejectedAsBlocking()
                        throws Exception {
                Scenario scenario = createScenario();
                RideRequestCommitCommand command = command(scenario, scenario.routeId());
                ExecutorService executor = Executors.newFixedThreadPool(2);
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                try {
                        Future<Object> first = executor.submit(() -> commitAfterBarrier(command, ready, start));
                        Future<Object> second = executor.submit(() -> commitAfterBarrier(command, ready, start));
                        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                        start.countDown();

                        List<Object> outcomes = List.of(
                                        first.get(20, TimeUnit.SECONDS),
                                        second.get(20, TimeUnit.SECONDS));

                        assertThat(outcomes).filteredOn(RideRequestPersistedView.class::isInstance).hasSize(1);
                        assertThat(outcomes).filteredOn(BusinessException.class::isInstance)
                                        .singleElement()
                                        .satisfies(outcome -> assertThat(((BusinessException) outcome).getCode())
                                                        .isEqualTo("UNFINISHED_RIDE_REQUEST_ALREADY_EXISTS"));
                        assertThat(rideRequestCount(scenario.actorId())).isEqualTo(1L);
                        assertThat(remainingSeats(scenario.routeId())).isEqualTo(2);
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
                return transactionTemplate.execute(status -> fixture.createStandardScenario(NOW, DEPARTURE));
        }

        private static RideRequestCriteria criteria(
                        Scenario scenario,
                        Long routeId,
                        CreateRideRequestRequest request) {
                return new RideRequestCriteria(
                                scenario.actorId(),
                                scenario.schoolId(),
                                routeId,
                                request.pickup().latitude(),
                                request.pickup().longitude(),
                                request.passengerDestination().latitude(),
                                request.passengerDestination().longitude(),
                                NOW);
        }

        private RideRequestCommitCommand command(
                        Scenario scenario,
                        Long routeId) {
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
                                NOW,
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
                                                                request.pickup().latitude(),
                                                                request.pickup().longitude()),
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
                                                                request.passengerDestination().longitude()
                                                                                .doubleValue(),
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

        private Instant persistRejectedRequest(
                        Scenario scenario,
                        Long routeId,
                        Instant rejectedAt,
                        long cooldownSeconds) {
                LoTrinhChiaSe route = entityManager.find(LoTrinhChiaSe.class, routeId);
                NguoiDung passenger = entityManager.find(NguoiDung.class, scenario.actorId());
                NguoiDung driver = entityManager.find(NguoiDung.class, scenario.driverId());
                CauHinhNghiepVu configuration = entityManager.find(
                                CauHinhNghiepVu.class, scenario.configurationId());
                YeuCauDiChung request = YeuCauDiChung.pending(
                                passenger,
                                route,
                                driver,
                                configuration,
                                RideRequestDecisionMother.snapshot(
                                                route.getVersion(),
                                                driver.getId(),
                                                configuration,
                                                route.getThoiGianKhoiHanhDuKien()),
                                rejectedAt.minusSeconds(30),
                                "Cooldown integration fixture");
                entityManager.persist(request);
                request.reject(rejectedAt, configuration, cooldownSeconds);
                entityManager.flush();
                return request.getCooldownUntil();
        }

        private long rideRequestCount(Long actorUserId) {
                return transactionTemplate.execute(status -> entityManager.createQuery(
                                "select count(request) from YeuCauDiChung request "
                                                + "where request.hanhKhach.id = :actorUserId",
                                Long.class)
                                .setParameter("actorUserId", actorUserId)
                                .getSingleResult());
        }

        private int remainingSeats(Long routeId) {
                return transactionTemplate.execute(status -> fixture.remainingSeats(routeId));
        }

        private long count(String jpql, Long id) {
                return entityManager.createQuery(jpql, Long.class)
                                .setParameter("actorId", id)
                                .getSingleResult();
        }
}
