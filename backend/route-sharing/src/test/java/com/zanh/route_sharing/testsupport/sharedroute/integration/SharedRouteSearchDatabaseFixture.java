package com.zanh.route_sharing.testsupport.sharedroute.integration;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.DongXe;
import com.zanh.route_sharing.domain.entity.HangXe;
import com.zanh.route_sharing.domain.entity.HoSoSinhVien;
import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.NhaTruong;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.domain.enums.CoSoSuDungPhuongTien;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiHoSoThanhVien;
import com.zanh.route_sharing.domain.enums.TrangThaiHocTap;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiXe;
import jakarta.persistence.EntityManager;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test Data Builder/Fake database fixture cho repository integration tests.
 *
 * <p>
 * Fixture persist entity thật qua JPA để schema, inheritance, auditing và
 * PostGIS mapping đều được sử dụng đúng như production. Test chỉ gọi public
 * API của repository; không đọc SQL private và không dùng reflection.
 * </p>
 */
public final class SharedRouteSearchDatabaseFixture {

        private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
        private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
        private static final AtomicLong SEQUENCE = new AtomicLong(10_000L);

        private final EntityManager entityManager;

        public SharedRouteSearchDatabaseFixture(EntityManager entityManager) {
                this.entityManager = entityManager;
        }

        public Scenario createStandardScenario(
                        Instant now,
                        Instant departureTime) {
                long suffix = SEQUENCE.incrementAndGet();
                LocalDate travelDate = LocalDate.ofInstant(departureTime, BUSINESS_ZONE);

                NhaTruong school = NhaTruong.builder()
                                .maTruong("IT-" + suffix)
                                .tenTruong("Trường Integration " + suffix)
                                .tenVietTat("IT" + suffix)
                                .diaChi("Địa chỉ integration " + suffix)
                                .tenMienEmailChoPhep(new LinkedHashSet<>(Set.of("integration.test")))
                                .dangHoatDong(true)
                                .build();
                entityManager.persist(school);

                CauHinhNghiepVu configuration = CauHinhNghiepVu.builder()
                                .nhaTruong(school)
                                .banKinhCungDiemDenMet(new BigDecimal("200.00"))
                                .banKinhDiemDenGanTuyenMet(new BigDecimal("150.00"))
                                .tyLeTienDuongToiThieu(new BigDecimal("60.00"))
                                .khoangCachLechDonToiDaMet(new BigDecimal("150.00"))
                                .thoiGianLechDonToiDaGiay(900L)
                                .banKinhXacDinhDaDenMet(new BigDecimal("50.00"))
                                .thoiGianChoKhachGiay(300L)
                                .thoiGianMatTinHieuGiay(120L)
                                .doLechThoiGianKhoiHanhPhut(30)
                                .soNgayLuuViTri(30)
                                .soNgayLuuNhatKy(90)
                                .batBuocTepXacNhanChuXeKhiKhongChinhChu(false)
                                .build();
                entityManager.persist(configuration);

                NguoiDung actor = createActiveUser(
                                "Hành khách Integration " + suffix,
                                "passenger-" + suffix + "@integration.test",
                                now);
                NguoiDung driver = createActiveUser(
                                "Tài xế Integration " + suffix,
                                "driver-" + suffix + "@integration.test",
                                now);

                HoSoSinhVien actorMembership = createApprovedMembership(
                                actor,
                                school,
                                "PASSENGER-" + suffix,
                                travelDate,
                                now);
                HoSoSinhVien driverMembership = createApprovedMembership(
                                driver,
                                school,
                                "DRIVER-" + suffix,
                                travelDate,
                                now);

                HoSoTaiXe driverProfile = HoSoTaiXe.builder()
                                .nguoiDung(driver)
                                .trangThaiTaiXe(TrangThaiTaiXe.ACTIVE)
                                .ngayDangKy(now.minusSeconds(86_400))
                                .ngayDuocDuyet(now.minusSeconds(43_200))
                                .build();
                entityManager.persist(driverProfile);

                HangXe brand = HangXe.builder()
                                .maHang("IT-BRAND-" + suffix)
                                .tenHang("Hãng Integration " + suffix)
                                .dangHoatDong(true)
                                .build();
                entityManager.persist(brand);

                DongXe model = DongXe.builder()
                                .hangXe(brand)
                                .tenDongXe("Dòng Integration " + suffix)
                                .loaiPhuongTien(LoaiPhuongTien.XE_MAY)
                                .soChoHanhKhachMacDinh(2)
                                .dangHoatDong(true)
                                .build();
                entityManager.persist(model);

                PhuongTien vehicle = createActiveVehicle(
                                driver,
                                model,
                                "IT" + suffix,
                                now);

                LineString routeLine = line(
                                106.6800, 10.7700,
                                106.7000, 10.7700,
                                106.7200, 10.7700);
                Point origin = point(106.6800, 10.7700);
                Point driverDestination = point(106.7200, 10.7700);

                LoTrinhChiaSe route = LoTrinhChiaSe.open(
                                driver,
                                vehicle,
                                origin,
                                "Điểm xuất phát integration " + suffix,
                                driverDestination,
                                "Điểm đích tài xế integration " + suffix,
                                routeLine,
                                new BigDecimal("4500.00"),
                                900L,
                                departureTime,
                                2,
                                new BigDecimal("3000.00"));
                entityManager.persist(route);

                flushAndClear();

                return new Scenario(
                                school.getId(),
                                configuration.getId(),
                                actor.getId(),
                                actorMembership.getId(),
                                driver.getId(),
                                driverMembership.getId(),
                                driverProfile.getId(),
                                brand.getId(),
                                model.getId(),
                                vehicle.getId(),
                                route.getId(),
                                departureTime,
                                travelDate);
        }

        public Long createAdditionalEquivalentRoute(
                        Scenario scenario,
                        Instant departureTime) {
                long suffix = SEQUENCE.incrementAndGet();
                NguoiDung driver = entityManager.find(NguoiDung.class, scenario.driverId());
                PhuongTien vehicle = entityManager.find(PhuongTien.class, scenario.vehicleId());

                LoTrinhChiaSe route = LoTrinhChiaSe.open(
                                driver,
                                vehicle,
                                point(106.6800, 10.7700),
                                "Điểm xuất phát bổ sung " + suffix,
                                point(106.7200, 10.7700),
                                "Điểm đích bổ sung " + suffix,
                                line(
                                                106.6800, 10.7700,
                                                106.7000, 10.7700,
                                                106.7200, 10.7700),
                                new BigDecimal("4500.00"),
                                900L,
                                departureTime,
                                2,
                                new BigDecimal("3000.00"));
                entityManager.persist(route);
                flushAndClear();
                return route.getId();
        }

        public void makeRouteOwnedByActor(Scenario scenario, Instant now) {
                NguoiDung actor = entityManager.find(NguoiDung.class, scenario.actorId());
                DongXe model = entityManager.find(DongXe.class, scenario.modelId());

                HoSoTaiXe actorDriverProfile = HoSoTaiXe.builder()
                                .nguoiDung(actor)
                                .trangThaiTaiXe(TrangThaiTaiXe.ACTIVE)
                                .ngayDangKy(now.minusSeconds(86_400))
                                .ngayDuocDuyet(now.minusSeconds(43_200))
                                .build();
                entityManager.persist(actorDriverProfile);

                PhuongTien actorVehicle = createActiveVehicle(
                                actor,
                                model,
                                "OWN" + SEQUENCE.incrementAndGet(),
                                now);

                LoTrinhChiaSe route = entityManager.find(
                                LoTrinhChiaSe.class,
                                scenario.routeId());
                route.setTaiXe(actor);
                route.setPhuongTien(actorVehicle);
                flushAndClear();
        }

        public void applyIneligibleMutation(
                        Scenario scenario,
                        IneligibleMutation mutation,
                        Instant now) {
                switch (mutation) {
                        case CLOSED_ROUTE -> route(scenario).setTrangThaiLoTrinh(TrangThaiLoTrinh.CANCELLED);
                        case NO_REMAINING_SEATS -> route(scenario).setSoGheConLai(0);
                        case DEPARTED_ROUTE -> route(scenario).setThoiGianKhoiHanhDuKien(now.minusSeconds(60));
                        case DRIVER_INACTIVE -> user(scenario.driverId())
                                        .setTrangThaiTaiKhoan(TrangThaiTaiKhoan.SUSPENDED);
                        case DRIVER_PROFILE_INACTIVE -> driverProfile(scenario)
                                        .setTrangThaiTaiXe(TrangThaiTaiXe.SUSPENDED);
                        case VEHICLE_INACTIVE -> vehicle(scenario)
                                        .setTrangThaiPhuongTien(TrangThaiPhuongTien.SUSPENDED);
                        case DRIVER_MEMBERSHIP_EXPIRED -> {
                                HoSoSinhVien membership = entityManager.find(
                                                HoSoSinhVien.class,
                                                scenario.driverMembershipId());
                                membership.setNgayKetThucHieuLuc(
                                                scenario.travelDate().minusDays(1));
                        }
                }
                flushAndClear();
        }

        public void expireActorBeforeRouteDate(Scenario scenario) {
                HoSoSinhVien membership = entityManager.find(
                                HoSoSinhVien.class,
                                scenario.actorMembershipId());
                membership.setNgayKetThucHieuLuc(
                                scenario.travelDate().minusDays(1));
                flushAndClear();
        }

        public int remainingSeats(Long routeId) {
                entityManager.flush();
                entityManager.clear();
                return entityManager.createQuery(
                                "select route.soGheConLai "
                                                + "from LoTrinhChiaSe route "
                                                + "where route.id = :routeId",
                                Integer.class)
                                .setParameter("routeId", routeId)
                                .getSingleResult();
        }

        public long countRideRequests() {
                entityManager.flush();
                entityManager.clear();
                return entityManager.createQuery(
                                "select count(rideRequest) "
                                                + "from YeuCauDiChung rideRequest",
                                Long.class)
                                .getSingleResult();
        }

        public void flushAndClear() {
                entityManager.flush();
                entityManager.clear();
        }

        public Point point(double longitude, double latitude) {
                return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        }

        public LineString line(double... longitudeLatitudePairs) {
                if (longitudeLatitudePairs.length < 4
                                || longitudeLatitudePairs.length % 2 != 0) {
                        throw new IllegalArgumentException(
                                        "A LineString requires at least two longitude/latitude pairs");
                }

                Coordinate[] coordinates = new Coordinate[longitudeLatitudePairs.length / 2];
                for (int index = 0; index < longitudeLatitudePairs.length; index += 2) {
                        coordinates[index / 2] = new Coordinate(
                                        longitudeLatitudePairs[index],
                                        longitudeLatitudePairs[index + 1]);
                }
                return GEOMETRY_FACTORY.createLineString(coordinates);
        }

        private NguoiDung createActiveUser(
                        String fullName,
                        String email,
                        Instant now) {
                NguoiDung user = NguoiDung.builder()
                                .hoTen(fullName)
                                .emailTruong(email)
                                .matKhauDaMaHoa("integration-test-password-hash")
                                .trangThaiTaiKhoan(TrangThaiTaiKhoan.ACTIVE)
                                .emailDaXacThucLuc(now.minusSeconds(86_400))
                                .build();
                entityManager.persist(user);
                return user;
        }

        private HoSoSinhVien createApprovedMembership(
                        NguoiDung user,
                        NhaTruong school,
                        String internalCode,
                        LocalDate travelDate,
                        Instant now) {
                HoSoSinhVien membership = HoSoSinhVien.builder()
                                .nguoiDung(user)
                                .nhaTruong(school)
                                .maDinhDanhNoiBo(internalCode)
                                .trangThaiHoSo(TrangThaiHoSoThanhVien.APPROVED)
                                .ngayBatDauHieuLuc(travelDate.minusDays(30))
                                .ngayKetThucHieuLuc(travelDate.plusDays(30))
                                .ngayDuocDuyet(now.minusSeconds(43_200))
                                .ngayNhapHoc(travelDate.minusYears(1))
                                .trangThaiHocTap(TrangThaiHocTap.DANG_HOC)
                                .build();
                entityManager.persist(membership);
                return membership;
        }

        private PhuongTien createActiveVehicle(
                        NguoiDung registeredUser,
                        DongXe model,
                        String plateSuffix,
                        Instant now) {
                String plate = "59A1-" + plateSuffix;
                if (plate.length() > 30) {
                        plate = plate.substring(0, 30);
                }

                PhuongTien vehicle = PhuongTien.builder()
                                .bienSoXe(plate)
                                .mauSacThucTe("Đen")
                                .soChoHanhKhachDuocDuyet(2)
                                .coSoSuDung(CoSoSuDungPhuongTien.CHINH_CHU)
                                .daCamKetDuocChuXeChoPhep(false)
                                .trangThaiPhuongTien(TrangThaiPhuongTien.ACTIVE)
                                .ngayDuocDuyet(now.minusSeconds(43_200))
                                .nguoiDangKySuDung(registeredUser)
                                .dongXe(model)
                                .build();
                entityManager.persist(vehicle);
                return vehicle;
        }

        private LoTrinhChiaSe route(Scenario scenario) {
                return entityManager.find(LoTrinhChiaSe.class, scenario.routeId());
        }

        private NguoiDung user(Long userId) {
                return entityManager.find(NguoiDung.class, userId);
        }

        private HoSoTaiXe driverProfile(Scenario scenario) {
                return entityManager.find(HoSoTaiXe.class, scenario.driverProfileId());
        }

        private PhuongTien vehicle(Scenario scenario) {
                return entityManager.find(PhuongTien.class, scenario.vehicleId());
        }

        public enum IneligibleMutation {
                CLOSED_ROUTE,
                NO_REMAINING_SEATS,
                DEPARTED_ROUTE,
                DRIVER_INACTIVE,
                DRIVER_PROFILE_INACTIVE,
                VEHICLE_INACTIVE,
                DRIVER_MEMBERSHIP_EXPIRED
        }

        public record Scenario(
                        Long schoolId,
                        Long configurationId,
                        Long actorId,
                        Long actorMembershipId,
                        Long driverId,
                        Long driverMembershipId,
                        Long driverProfileId,
                        Long brandId,
                        Long modelId,
                        Long vehicleId,
                        Long routeId,
                        Instant departureTime,
                        LocalDate travelDate) {
        }
}
