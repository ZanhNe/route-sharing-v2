package com.zanh.route_sharing.repository.sharedroute;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.GioiTinh;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.riderequest.RideRequestPointSnapshot;
import com.zanh.route_sharing.domain.riderequest.RideRequestPolicySnapshot;
import com.zanh.route_sharing.domain.riderequest.RideRequestSnapshot;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.RouteRideRequestQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestDetailLookup;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture.Scenario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostgisRouteRideRequestQueryRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-06T03:00:00Z");
    private static final Instant DEPARTURE = NOW.plusSeconds(7200);
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private RouteRideRequestQueryRepository sut;
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
    void givenOwnedRouteWithPendingRequests_whenListing_thenOnlyPendingRowsUseStableOrderAndPagination() {
        Setup setup = createSetup(NOW.plusSeconds(20));
        Long secondPassengerId = transactionTemplate.execute(status -> createAdditionalPassenger());
        Long earlierRequestId = transactionTemplate.execute(status -> persistPendingRequest(
                setup.scenario(),
                secondPassengerId,
                NOW.plusSeconds(10),
                new BigDecimal("15000.00")));

        var page = sut.findPendingPage(
                        setup.scenario().driverId(),
                        setup.scenario().routeId(),
                        0,
                        1)
                .orElseThrow();

        assertThat(page.totalElements()).isEqualTo(2L);
        assertThat(page.rows()).hasSize(1);
        assertThat(page.rows().get(0).rideRequestId()).isEqualTo(earlierRequestId);
        assertThat(page.rows().get(0).status().name()).isEqualTo("PENDING");
        assertThat(page.rows().get(0).passengerId()).isEqualTo(secondPassengerId);

        var secondPage = sut.findPendingPage(
                        setup.scenario().driverId(),
                        setup.scenario().routeId(),
                        1,
                        1)
                .orElseThrow();
        assertThat(secondPage.rows()).singleElement()
                .extracting(row -> row.rideRequestId())
                .isEqualTo(setup.rideRequestId());
    }

    @Test
    void givenMissingOrNonOwnedRoute_whenListing_thenRouteIsHidden() {
        Setup setup = createSetup(NOW);

        assertThat(sut.findPendingPage(
                setup.scenario().actorId(),
                setup.scenario().routeId(),
                0,
                10)).isEmpty();
        assertThat(sut.findPendingPage(
                setup.scenario().driverId(),
                Long.MAX_VALUE,
                0,
                10)).isEmpty();
    }

    @Test
    void givenOwnedPendingRequest_whenGettingDetail_thenPassengerAndStoredSnapshotAreReturned() {
        Setup setup = createSetup(NOW);

        PendingRideRequestDetailLookup lookup = sut.findPendingDetail(
                setup.scenario().driverId(),
                setup.scenario().routeId(),
                setup.rideRequestId());

        assertThat(lookup.status()).isEqualTo(PendingRideRequestDetailLookup.Status.FOUND);
        assertThat(lookup.route().routeId()).isEqualTo(setup.scenario().routeId());
        assertThat(lookup.request().passengerId()).isEqualTo(setup.scenario().actorId());
        assertThat(lookup.request().passengerGender()).isEqualTo(GioiTinh.NAM);
        assertThat(lookup.request().passengerDateOfBirth()).isEqualTo(LocalDate.of(2003, 3, 15));
        assertThat(lookup.request().passengerDesiredRouteGeoJson()).contains("LineString");
        assertThat(lookup.request().servedSegmentGeoJson()).contains("LineString");
        assertThat(lookup.request().pickupLatitude()).isEqualByComparingTo("10.770100");
        assertThat(lookup.request().pickupLongitude()).isEqualByComparingTo("106.690000");
        assertThat(lookup.request().proposedSupportAmount()).isEqualByComparingTo("25000.00");
    }

    @Test
    void givenRequestNoLongerPending_whenListingOrGettingDetail_thenItIsOutsideThePendingQueue() {
        Setup setup = createSetup(NOW);
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery("""
                    UPDATE yeu_cau_di_chung
                    SET trang_thai_yeu_cau = 'ACCEPTED'
                    WHERE id = :requestId
                    """)
                    .setParameter("requestId", setup.rideRequestId())
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();
        });

        var page = sut.findPendingPage(
                        setup.scenario().driverId(),
                        setup.scenario().routeId(),
                        0,
                        10)
                .orElseThrow();
        var detail = sut.findPendingDetail(
                setup.scenario().driverId(),
                setup.scenario().routeId(),
                setup.rideRequestId());

        assertThat(page.totalElements()).isZero();
        assertThat(page.rows()).isEmpty();
        assertThat(detail.status())
                .isEqualTo(PendingRideRequestDetailLookup.Status.REQUEST_NOT_FOUND_OR_NOT_PENDING);
    }

    @Test
    void givenOwnedNonOpenRoute_whenListing_thenPendingQueueRemainsReadable() {
        Setup setup = createSetup(NOW);
        transactionTemplate.executeWithoutResult(status -> {
            LoTrinhChiaSe route = entityManager.find(
                    LoTrinhChiaSe.class,
                    setup.scenario().routeId());
            route.cancelByDriver(NOW, "Test fixture: route đã đóng");
            entityManager.flush();
            entityManager.clear();
        });

        var page = sut.findPendingPage(
                        setup.scenario().driverId(),
                        setup.scenario().routeId(),
                        0,
                        10)
                .orElseThrow();

        assertThat(page.route().routeStatus()).isEqualTo(TrangThaiLoTrinh.CANCELLED);
        assertThat(page.rows()).hasSize(1);
    }

    @Test
    void givenReadOperations_whenListingAndGettingDetail_thenRequestSeatAuditNotificationAndTripRemainUnchanged() {
        Setup setup = createSetup(NOW);
        DatabaseState before = databaseState(setup);

        sut.findPendingPage(
                setup.scenario().driverId(),
                setup.scenario().routeId(),
                0,
                10).orElseThrow();
        sut.findPendingDetail(
                setup.scenario().driverId(),
                setup.scenario().routeId(),
                setup.rideRequestId());

        DatabaseState after = databaseState(setup);
        assertThat(after).isEqualTo(before);
    }

    private Setup createSetup(Instant sentAt) {
        return transactionTemplate.execute(status -> {
            Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
            NguoiDung passenger = entityManager.find(NguoiDung.class, scenario.actorId());
            passenger.setAnhDaiDienUrl("https://example.test/passenger.png");
            passenger.setGioiTinh(GioiTinh.NAM);
            passenger.setNgaySinh(LocalDate.of(2003, 3, 15));
            Long requestId = persistPendingRequest(
                    scenario,
                    passenger.getId(),
                    sentAt,
                    new BigDecimal("25000.00"));
            return new Setup(scenario, requestId);
        });
    }

    private Long persistPendingRequest(
            Scenario scenario,
            Long passengerId,
            Instant sentAt,
            BigDecimal supportAmount) {
        NguoiDung passenger = entityManager.find(NguoiDung.class, passengerId);
        LoTrinhChiaSe route = entityManager.find(LoTrinhChiaSe.class, scenario.routeId());
        NguoiDung driver = entityManager.find(NguoiDung.class, scenario.driverId());
        CauHinhNghiepVu configuration = entityManager.find(
                CauHinhNghiepVu.class,
                scenario.configurationId());

        Point pickup = point(106.690000, 10.770100);
        Point dropoff = point(106.705000, 10.770000);
        Point destination = point(106.710000, 10.770100);
        LineString passengerRoute = line(
                106.690000, 10.770100,
                106.705000, 10.770000,
                106.710000, 10.770100);
        LineString servedSegment = line(
                106.690000, 10.770100,
                106.705000, 10.770000);

        RideRequestPolicySnapshot policy = new RideRequestPolicySnapshot(
                configuration.getId(),
                configuration.getVersion(),
                configuration.getBanKinhCungDiemDenMet(),
                configuration.getBanKinhDiemDenGanTuyenMet(),
                configuration.getKhoangCachLechDonToiDaMet(),
                configuration.getThoiGianLechDonToiDaGiay(),
                configuration.getTyLeTienDuongToiThieu(),
                Duration.ofSeconds(configuration.getBookingCutoffSeconds()),
                Duration.ofSeconds(configuration.getRejectionCooldownSeconds()));
        RideRequestSnapshot snapshot = new RideRequestSnapshot(
                route.getVersion(),
                driver.getId(),
                route.getThoiGianKhoiHanhDuKien(),
                LoaiGhepTuyen.TRUNG_DOAN_TUYEN,
                LoaiDiemTha.DIEM_THA_TRUNG_GIAN,
                new RideRequestPointSnapshot(pickup, "Điểm đón integration"),
                new RideRequestPointSnapshot(destination, "Điểm đến integration"),
                new RideRequestPointSnapshot(dropoff, "Điểm thả integration"),
                passengerRoute,
                servedSegment,
                new BigDecimal("50.00"),
                60L,
                new BigDecimal("4000.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("75.00"),
                new BigDecimal("3000.00"),
                supportAmount,
                null,
                policy);

        YeuCauDiChung request = YeuCauDiChung.pending(
                passenger,
                route,
                driver,
                configuration,
                snapshot,
                sentAt,
                "Ghi chú integration");
        entityManager.persist(request);
        entityManager.flush();
        entityManager.clear();
        return request.getId();
    }

    private Long createAdditionalPassenger() {
        long suffix = System.nanoTime();
        NguoiDung passenger = NguoiDung.builder()
                .hoTen("Hành khách bổ sung " + suffix)
                .emailTruong("extra-" + suffix + "@integration.test")
                .matKhauDaMaHoa("integration-test-password-hash")
                .trangThaiTaiKhoan(TrangThaiTaiKhoan.ACTIVE)
                .emailDaXacThucLuc(NOW.minusSeconds(3600))
                .gioiTinh(GioiTinh.NU)
                .ngaySinh(LocalDate.of(2004, 4, 16))
                .build();
        entityManager.persist(passenger);
        entityManager.flush();
        return passenger.getId();
    }

    private DatabaseState databaseState(Setup setup) {
        return transactionTemplate.execute(status -> {
            long requestCount = count("select count(r) from YeuCauDiChung r");
            long auditCount = count("select count(a) from NhatKyTrangThaiYeuCau a");
            long notificationCount = count("select count(n) from ThongBao n");
            long tripCount = count("select count(t) from ChuyenDi t");
            String requestStatus = entityManager.createQuery(
                            "select r.trangThaiYeuCau from YeuCauDiChung r where r.id = :id",
                            com.zanh.route_sharing.domain.enums.TrangThaiYeuCau.class)
                    .setParameter("id", setup.rideRequestId())
                    .getSingleResult()
                    .name();
            int seats = fixture.remainingSeats(setup.scenario().routeId());
            return new DatabaseState(
                    requestCount,
                    auditCount,
                    notificationCount,
                    tripCount,
                    requestStatus,
                    seats);
        });
    }

    private long count(String jpql) {
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    private static Point point(double longitude, double latitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    private static LineString line(double... longitudeLatitudePairs) {
        Coordinate[] coordinates = new Coordinate[longitudeLatitudePairs.length / 2];
        for (int index = 0; index < longitudeLatitudePairs.length; index += 2) {
            coordinates[index / 2] = new Coordinate(
                    longitudeLatitudePairs[index],
                    longitudeLatitudePairs[index + 1]);
        }
        LineString lineString = GEOMETRY_FACTORY.createLineString(coordinates);
        lineString.setSRID(4326);
        return lineString;
    }

    private record Setup(
            Scenario scenario,
            Long rideRequestId) {
    }

    private record DatabaseState(
            long requestCount,
            long auditCount,
            long notificationCount,
            long tripCount,
            String requestStatus,
            int remainingSeats) {
    }
}
