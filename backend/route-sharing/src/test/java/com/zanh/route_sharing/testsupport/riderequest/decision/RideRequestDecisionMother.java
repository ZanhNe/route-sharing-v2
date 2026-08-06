package com.zanh.route_sharing.testsupport.riderequest.decision;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.DongXe;
import com.zanh.route_sharing.domain.entity.HangXe;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.NhaTruong;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.CoSoSuDungPhuongTien;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiPhuongTien;
import com.zanh.route_sharing.domain.riderequest.RideRequestPointSnapshot;
import com.zanh.route_sharing.domain.riderequest.RideRequestPolicySnapshot;
import com.zanh.route_sharing.domain.riderequest.RideRequestSnapshot;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public final class RideRequestDecisionMother {

        public static final Long ACTOR_ID = 9L;
        public static final Long ROUTE_ID = 22L;
        public static final Long REQUEST_ID = 501L;
        public static final Long PASSENGER_ID = 7L;
        public static final Instant DECISION_AT = Instant.parse("2026-08-06T06:30:00Z");
        public static final Instant DEPARTURE = DECISION_AT.plusSeconds(7200);
        public static final BigDecimal PROPOSED_SUPPORT = new BigDecimal("25000.00");

        private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);

        private RideRequestDecisionMother() {
        }

        public static DecisionAggregate aggregate() {
                NguoiDung driver = NguoiDung.builder()
                                .id(ACTOR_ID)
                                .version(0L)
                                .hoTen("Tài xế")
                                .emailTruong("driver@test.local")
                                .matKhauDaMaHoa("encoded")
                                .build();
                NguoiDung passenger = NguoiDung.builder()
                                .id(PASSENGER_ID)
                                .version(0L)
                                .hoTen("Hành khách")
                                .emailTruong("passenger@test.local")
                                .matKhauDaMaHoa("encoded")
                                .build();
                HangXe brand = HangXe.builder()
                                .id(61L).version(0L).maHang("TEST").tenHang("Test").dangHoatDong(true).build();
                DongXe model = DongXe.builder()
                                .id(60L).version(0L).tenDongXe("Model")
                                .loaiPhuongTien(LoaiPhuongTien.XE_MAY)
                                .soChoHanhKhachMacDinh(2).dangHoatDong(true).hangXe(brand).build();
                PhuongTien vehicle = PhuongTien.builder()
                                .id(50L).version(0L).bienSoXe("59A1-TEST")
                                .mauSacThucTe("Đen").soChoHanhKhachDuocDuyet(2)
                                .coSoSuDung(CoSoSuDungPhuongTien.CHINH_CHU)
                                .daCamKetDuocChuXeChoPhep(false)
                                .trangThaiPhuongTien(TrangThaiPhuongTien.ACTIVE)
                                .nguoiDangKySuDung(driver).dongXe(model).build();
                LoTrinhChiaSe route = LoTrinhChiaSe.open(
                                driver, vehicle,
                                point(106.69, 10.77), "A",
                                point(106.72, 10.79), "D",
                                line(106.69, 10.77, 106.72, 10.79),
                                new BigDecimal("10000.00"), 1200L, DEPARTURE, 2,
                                new BigDecimal("5000.00"));
                route.setId(ROUTE_ID);
                route.setVersion(0L);

                NhaTruong school = NhaTruong.builder()
                                .id(70L).version(0L).maTruong("TEST")
                                .tenTruong("Trường Test").diaChi("Địa chỉ").dangHoatDong(true).build();
                CauHinhNghiepVu config = CauHinhNghiepVu.builder()
                                .id(80L).version(3L).nhaTruong(school)
                                .banKinhCungDiemDenMet(new BigDecimal("200"))
                                .banKinhDiemDenGanTuyenMet(new BigDecimal("150"))
                                .tyLeTienDuongToiThieu(new BigDecimal("60"))
                                .khoangCachLechDonToiDaMet(new BigDecimal("150"))
                                .thoiGianLechDonToiDaGiay(900L)
                                .banKinhXacDinhDaDenMet(new BigDecimal("100"))
                                .thoiGianChoKhachGiay(300L).thoiGianMatTinHieuGiay(120L)
                                .doLechThoiGianKhoiHanhPhut(15).soNgayLuuViTri(30).soNgayLuuNhatKy(365)
                                .requestTtlSeconds(900L).bookingCutoffSeconds(900L)
                                .rejectionCooldownSeconds(3600L).build();

                RideRequestSnapshot snapshot = snapshot(route.getVersion(), driver.getId(), config, DEPARTURE);
                YeuCauDiChung request = YeuCauDiChung.pending(
                                passenger, route, driver, config, snapshot,
                                DECISION_AT.minusSeconds(300), DECISION_AT.plusSeconds(600), "Ghi chú");
                request.setId(REQUEST_ID);
                request.setVersion(0L);
                return new DecisionAggregate(driver, passenger, route, config, request);
        }

        public static RideRequestSnapshot snapshot(Long routeVersion, Long driverId, CauHinhNghiepVu config) {
                return snapshot(routeVersion, driverId, config, DEPARTURE);
        }

        public static RideRequestSnapshot snapshot(
                        Long routeVersion,
                        Long driverId,
                        CauHinhNghiepVu config,
                        Instant departure) {
                Point p = point(106.700, 10.775);
                Point x = point(106.710, 10.780);
                Point c = point(106.715, 10.785);
                return new RideRequestSnapshot(
                                routeVersion, driverId, departure,
                                LoaiGhepTuyen.TRUNG_DOAN_TUYEN,
                                LoaiDiemTha.DIEM_THA_TRUNG_GIAN,
                                new RideRequestPointSnapshot(p, "P"),
                                new RideRequestPointSnapshot(c, "C"),
                                new RideRequestPointSnapshot(x, "X"),
                                line(106.700, 10.775, 106.710, 10.780, 106.715, 10.785),
                                line(106.700, 10.775, 106.710, 10.780),
                                new BigDecimal("100.00"), 60L,
                                new BigDecimal("4200.00"), new BigDecimal("3900.00"),
                                new BigDecimal("300.00"), new BigDecimal("92.86"),
                                new BigDecimal("5000.00"), PROPOSED_SUPPORT, null,
                                new RideRequestPolicySnapshot(
                                                config.getId(), config.getVersion(),
                                                new BigDecimal("200"), new BigDecimal("150"),
                                                new BigDecimal("150"), 900L, new BigDecimal("60"),
                                                Duration.ofMinutes(15), Duration.ofMinutes(15), Duration.ofHours(1)));
        }

        private static Point point(double longitude, double latitude) {
                Point point = GEOMETRY.createPoint(new Coordinate(longitude, latitude));
                point.setSRID(4326);
                return point;
        }

        private static LineString line(double... coordinates) {
                Coordinate[] points = new Coordinate[coordinates.length / 2];
                for (int index = 0; index < coordinates.length; index += 2) {
                        points[index / 2] = new Coordinate(coordinates[index], coordinates[index + 1]);
                }
                LineString line = GEOMETRY.createLineString(points);
                line.setSRID(4326);
                return line;
        }

        public record DecisionAggregate(
                        NguoiDung driver,
                        NguoiDung passenger,
                        LoTrinhChiaSe route,
                        CauHinhNghiepVu configuration,
                        YeuCauDiChung request) {
        }
}
