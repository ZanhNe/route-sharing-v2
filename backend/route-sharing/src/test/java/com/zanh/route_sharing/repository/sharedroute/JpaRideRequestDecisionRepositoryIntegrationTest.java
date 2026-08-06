package com.zanh.route_sharing.repository.sharedroute;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.HoSoSinhVien;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.NhaTruong;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.LoaiThongBao;
import com.zanh.route_sharing.domain.enums.TrangThaiHoSoThanhVien;
import com.zanh.route_sharing.domain.enums.TrangThaiHocTap;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiXe;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.service.RideRequestDecisionService;
import com.zanh.route_sharing.testsupport.riderequest.decision.RideRequestDecisionMother;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture.Scenario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

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
class JpaRideRequestDecisionRepositoryIntegrationTest {

    @Autowired
    private RideRequestDecisionService service;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private SharedRouteSearchDatabaseFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new SharedRouteSearchDatabaseFixture(entityManager);
    }

    @Test
    void givenPendingRequest_whenAccepting_thenRequestSeatAuditAndPassengerNotificationCommitTogether() {
        TestAggregate aggregate = createAggregate(2, false);

        var result = service.accept(
                aggregate.scenario().driverId(),
                aggregate.scenario().routeId(),
                aggregate.requestId());

        assertThat(result.status()).isEqualTo(TrangThaiYeuCau.ACCEPTED);
        assertThat(result.remainingSeats()).isEqualTo(1);
        transactionTemplate.executeWithoutResult(status -> {
            YeuCauDiChung request = entityManager.find(YeuCauDiChung.class, aggregate.requestId());
            assertThat(request.getTrangThaiYeuCau()).isEqualTo(TrangThaiYeuCau.ACCEPTED);
            assertThat(request.getMucHoTroDaThoaThuan())
                    .isEqualByComparingTo(request.getMucHoTroHanhKhachDeNghi());
            assertThat(remainingSeats(aggregate.scenario().routeId())).isEqualTo(1);
            assertThat(stateEventCount(aggregate.requestId())).isEqualTo(2L);
            assertThat(notificationCount(aggregate.requestId(), LoaiThongBao.BOOKING_ACCEPTED)).isEqualTo(1L);
            assertThat(tripCount(aggregate.scenario().routeId())).isZero();
        });
    }

    @Test
    void givenPendingRequest_whenRejecting_thenSeatStaysAndCurrentCooldownIsPersisted() {
        TestAggregate aggregate = createAggregate(2, false);

        var result = service.reject(
                aggregate.scenario().driverId(),
                aggregate.scenario().routeId(),
                aggregate.requestId());

        assertThat(result.status()).isEqualTo(TrangThaiYeuCau.REJECTED);
        assertThat(result.remainingSeats()).isEqualTo(2);
        transactionTemplate.executeWithoutResult(status -> {
            YeuCauDiChung request = entityManager.find(YeuCauDiChung.class, aggregate.requestId());
            assertThat(request.getRejectionCooldownAppliedSeconds()).isEqualTo(3600L);
            assertThat(request.getCooldownUntil()).isEqualTo(request.getTuChoiLuc().plusSeconds(3600));
            assertThat(request.getCauHinhVersionLucTuChoi()).isNotNull();
            assertThat(remainingSeats(aggregate.scenario().routeId())).isEqualTo(2);
            assertThat(stateEventCount(aggregate.requestId())).isEqualTo(2L);
            assertThat(notificationCount(aggregate.requestId(), LoaiThongBao.BOOKING_REJECTED)).isEqualTo(1L);
            assertThat(tripCount(aggregate.scenario().routeId())).isZero();
        });
    }

    @Test
    void givenAlreadyAcceptedRequest_whenAcceptingAgain_thenConflictHasNoDuplicateSideEffect() {
        TestAggregate aggregate = createAggregate(2, false);
        service.accept(aggregate.scenario().driverId(), aggregate.scenario().routeId(), aggregate.requestId());

        assertThatThrownBy(() -> service.accept(
                aggregate.scenario().driverId(),
                aggregate.scenario().routeId(),
                aggregate.requestId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_RIDE_REQUEST_STATE"));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(remainingSeats(aggregate.scenario().routeId())).isEqualTo(1);
            assertThat(stateEventCount(aggregate.requestId())).isEqualTo(2L);
            assertThat(notificationCount(aggregate.requestId(), LoaiThongBao.BOOKING_ACCEPTED)).isEqualTo(1L);
        });
    }

    @Test
    void givenNonOwner_whenDeciding_thenRouteIsHiddenAndNothingChanges() {
        TestAggregate aggregate = createAggregate(2, false);

        assertThatThrownBy(() -> service.reject(
                aggregate.scenario().actorId(),
                aggregate.scenario().routeId(),
                aggregate.requestId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("SHARED_ROUTE_NOT_FOUND"));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(statusOf(aggregate.requestId())).isEqualTo(TrangThaiYeuCau.PENDING);
            assertThat(remainingSeats(aggregate.scenario().routeId())).isEqualTo(2);
            assertThat(stateEventCount(aggregate.requestId())).isEqualTo(1L);
        });
    }

    @Test
    void givenExpiredRequest_whenDeciding_thenNoMutationOccurs() {
        TestAggregate aggregate = createAggregate(2, true);

        assertThatThrownBy(() -> service.accept(
                aggregate.scenario().driverId(),
                aggregate.scenario().routeId(),
                aggregate.requestId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("RIDE_REQUEST_EXPIRED"));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(statusOf(aggregate.requestId())).isEqualTo(TrangThaiYeuCau.PENDING);
            assertThat(remainingSeats(aggregate.scenario().routeId())).isEqualTo(2);
        });
    }

    @Test
    void givenCurrentDriverProfileIneligible_whenAccepting_thenNoMutationOccurs() {
        TestAggregate aggregate = createAggregate(2, false);
        transactionTemplate.executeWithoutResult(status -> entityManager.createQuery(
                "update HoSoTaiXe profile set profile.trangThaiTaiXe = :state "
                        + "where profile.id = :profileId")
                .setParameter("state", TrangThaiTaiXe.SUSPENDED)
                .setParameter("profileId", aggregate.scenario().driverProfileId())
                .executeUpdate());

        assertThatThrownBy(() -> service.accept(
                aggregate.scenario().driverId(),
                aggregate.scenario().routeId(),
                aggregate.requestId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("DRIVER_OR_VEHICLE_INELIGIBLE"));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(statusOf(aggregate.requestId())).isEqualTo(TrangThaiYeuCau.PENDING);
            assertThat(remainingSeats(aggregate.scenario().routeId())).isEqualTo(2);
        });
    }

    @Test
    void givenTwoPendingRequestsAndLastSeat_whenAcceptingConcurrently_thenExactlyOneWins() throws Exception {
        TestAggregate aggregate = createAggregate(1, false);
        Long secondRequestId = createAdditionalPendingRequest(aggregate.scenario());
        List<Object> outcomes = runConcurrent(
                () -> service.accept(
                        aggregate.scenario().driverId(),
                        aggregate.scenario().routeId(),
                        aggregate.requestId()),
                () -> service.accept(
                        aggregate.scenario().driverId(),
                        aggregate.scenario().routeId(),
                        secondRequestId));

        assertThat(outcomes).filteredOn(result -> !(result instanceof BusinessException)).hasSize(1);
        assertThat(outcomes).filteredOn(BusinessException.class::isInstance)
                .singleElement()
                .satisfies(result -> assertThat(((BusinessException) result).getCode())
                        .isEqualTo("SHARED_ROUTE_NO_REMAINING_SEATS"));
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(remainingSeats(aggregate.scenario().routeId())).isZero();
            long accepted = List.of(statusOf(aggregate.requestId()), statusOf(secondRequestId)).stream()
                    .filter(state -> state == TrangThaiYeuCau.ACCEPTED)
                    .count();
            assertThat(accepted).isEqualTo(1L);
        });
    }

    @Test
    void givenSamePendingRequest_whenAcceptAndRejectRace_thenExactlyOneTerminalTransitionWins() throws Exception {
        TestAggregate aggregate = createAggregate(1, false);

        List<Object> outcomes = runConcurrent(
                () -> service.accept(
                        aggregate.scenario().driverId(),
                        aggregate.scenario().routeId(),
                        aggregate.requestId()),
                () -> service.reject(
                        aggregate.scenario().driverId(),
                        aggregate.scenario().routeId(),
                        aggregate.requestId()));

        assertThat(outcomes).filteredOn(result -> !(result instanceof BusinessException)).hasSize(1);
        assertThat(outcomes).filteredOn(BusinessException.class::isInstance)
                .singleElement()
                .satisfies(result -> assertThat(((BusinessException) result).getCode())
                        .isEqualTo("INVALID_RIDE_REQUEST_STATE"));
        transactionTemplate.executeWithoutResult(status -> {
            TrangThaiYeuCau finalState = statusOf(aggregate.requestId());
            assertThat(finalState).isIn(TrangThaiYeuCau.ACCEPTED, TrangThaiYeuCau.REJECTED);
            assertThat(stateEventCount(aggregate.requestId())).isEqualTo(2L);
            long decisionNotifications = notificationCount(
                    aggregate.requestId(), LoaiThongBao.BOOKING_ACCEPTED)
                    + notificationCount(aggregate.requestId(), LoaiThongBao.BOOKING_REJECTED);
            assertThat(decisionNotifications).isEqualTo(1L);
            assertThat(remainingSeats(aggregate.scenario().routeId()))
                    .isEqualTo(finalState == TrangThaiYeuCau.ACCEPTED ? 0 : 1);
        });
    }

    private TestAggregate createAggregate(int seats, boolean expired) {
        Instant now = Instant.now();
        Instant departure = now.plusSeconds(7200);
        Scenario scenario = transactionTemplate.execute(status -> fixture.createStandardScenario(now, departure));
        Long requestId = transactionTemplate.execute(status -> {
            LoTrinhChiaSe route = entityManager.find(LoTrinhChiaSe.class, scenario.routeId());
            route.setSoGheCungCap(Math.max(seats, 1));
            route.setSoGheConLai(seats);
            Instant sentAt = expired ? now.minusSeconds(120) : now.minusSeconds(30);
            Instant expiresAt = expired ? now.minusSeconds(1) : now.plusSeconds(1200);
            NguoiDung passenger = entityManager.find(NguoiDung.class, scenario.actorId());
            return persistPending(scenario, route, passenger, sentAt, expiresAt);
        });
        return new TestAggregate(scenario, requestId);
    }

    private Long createAdditionalPendingRequest(Scenario scenario) {
        return transactionTemplate.execute(status -> {
            LoTrinhChiaSe route = entityManager.find(LoTrinhChiaSe.class, scenario.routeId());
            Instant now = Instant.now();
            NguoiDung secondPassenger = createAdditionalPassenger(scenario, now);
            return persistPending(
                    scenario,
                    route,
                    secondPassenger,
                    now.minusSeconds(20),
                    now.plusSeconds(1200));
        });
    }

    private NguoiDung createAdditionalPassenger(Scenario scenario, Instant now) {
        String suffix = String.valueOf(scenario.routeId());
        NguoiDung passenger = NguoiDung.builder()
                .hoTen("Hành khách tranh ghế " + suffix)
                .emailTruong("passenger-race-" + suffix + "@integration.test")
                .matKhauDaMaHoa("integration-test-password-hash")
                .trangThaiTaiKhoan(TrangThaiTaiKhoan.ACTIVE)
                .emailDaXacThucLuc(now.minusSeconds(86_400))
                .build();
        entityManager.persist(passenger);

        NhaTruong school = entityManager.find(NhaTruong.class, scenario.schoolId());
        HoSoSinhVien membership = HoSoSinhVien.builder()
                .nguoiDung(passenger)
                .nhaTruong(school)
                .maDinhDanhNoiBo("RACE-PASSENGER-" + suffix)
                .trangThaiHoSo(TrangThaiHoSoThanhVien.APPROVED)
                .ngayBatDauHieuLuc(scenario.travelDate().minusDays(30))
                .ngayKetThucHieuLuc(scenario.travelDate().plusDays(30))
                .ngayDuocDuyet(now.minusSeconds(43_200))
                .ngayNhapHoc(scenario.travelDate().minusYears(1))
                .trangThaiHocTap(TrangThaiHocTap.DANG_HOC)
                .build();
        entityManager.persist(membership);
        entityManager.flush();
        return passenger;
    }

    private Long persistPending(
            Scenario scenario,
            LoTrinhChiaSe route,
            NguoiDung passenger,
            Instant sentAt,
            Instant expiresAt) {
        NguoiDung driver = entityManager.find(NguoiDung.class, scenario.driverId());
        CauHinhNghiepVu configuration = entityManager.find(
                CauHinhNghiepVu.class, scenario.configurationId());
        YeuCauDiChung request = YeuCauDiChung.pending(
                passenger,
                route,
                driver,
                configuration,
                RideRequestDecisionMother.snapshot(
                        route.getVersion(), driver.getId(), configuration, route.getThoiGianKhoiHanhDuKien()),
                sentAt,
                expiresAt,
                "Integration decision");
        entityManager.persist(request);
        entityManager.flush();
        entityManager.persist(NhatKyTrangThaiYeuCau.created(request, passenger, sentAt));
        entityManager.persist(ThongBao.bookingRequest(request, driver));
        entityManager.flush();
        return request.getId();
    }

    private List<Object> runConcurrent(CheckedSupplier first, CheckedSupplier second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Object> a = executor.submit(() -> afterBarrier(first, ready, start));
            Future<Object> b = executor.submit(() -> afterBarrier(second, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private static Object afterBarrier(
            CheckedSupplier supplier,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            return supplier.get();
        } catch (BusinessException exception) {
            return exception;
        }
    }

    private int remainingSeats(Long routeId) {
        return entityManager.createQuery(
                "select route.soGheConLai from LoTrinhChiaSe route where route.id = :id",
                Integer.class)
                .setParameter("id", routeId)
                .getSingleResult();
    }

    private TrangThaiYeuCau statusOf(Long requestId) {
        return entityManager.createQuery(
                "select request.trangThaiYeuCau from YeuCauDiChung request where request.id = :id",
                TrangThaiYeuCau.class)
                .setParameter("id", requestId)
                .getSingleResult();
    }

    private long stateEventCount(Long requestId) {
        return entityManager.createQuery(
                "select count(event) from NhatKyTrangThaiYeuCau event "
                        + "where event.yeuCauDiChung.id = :id",
                Long.class)
                .setParameter("id", requestId)
                .getSingleResult();
    }

    private long notificationCount(Long requestId, LoaiThongBao type) {
        return entityManager.createQuery(
                "select count(notification) from ThongBao notification "
                        + "where notification.doiTuongLienQuanId = :id "
                        + "and notification.loaiThongBao = :type",
                Long.class)
                .setParameter("id", requestId)
                .setParameter("type", type)
                .getSingleResult();
    }

    private long tripCount(Long routeId) {
        return entityManager.createQuery(
                "select count(trip) from ChuyenDi trip where trip.loTrinhChiaSe.id = :id",
                Long.class)
                .setParameter("id", routeId)
                .getSingleResult();
    }

    private record TestAggregate(Scenario scenario, Long requestId) {
    }

    @FunctionalInterface
    private interface CheckedSupplier {
        Object get() throws Exception;
    }
}
