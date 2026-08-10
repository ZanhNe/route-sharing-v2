package com.zanh.route_sharing.testfixture;

import com.zanh.route_sharing.domain.entity.DongXe;
import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiXe;
import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import com.zanh.route_sharing.service.routing.model.RouteBounds;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanLeg;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class SharedRouteMother {

        public static final Instant NOW = Instant.parse("2026-08-01T08:00:00Z");

        private SharedRouteMother() {
        }

        public static NguoiDung activeUser(Long id) {
                return NguoiDung.builder()
                                .id(id)
                                .hoTen("Tài xế kiểm thử")
                                .emailTruong("driver" + id + "@university.test")
                                .matKhauDaMaHoa("encoded-password")
                                .trangThaiTaiKhoan(TrangThaiTaiKhoan.ACTIVE)
                                .build();
        }

        public static HoSoTaiXe activeDriverProfile(Long id, NguoiDung user) {
                return HoSoTaiXe.builder()
                                .id(id)
                                .nguoiDung(user)
                                .ngayDangKy(NOW.minusSeconds(172800))
                                .ngayDuocDuyet(NOW.minusSeconds(86400))
                                .trangThaiTaiXe(TrangThaiTaiXe.ACTIVE)
                                .build();
        }

        public static PhuongTien activeMotorbike(Long id, NguoiDung owner, int approvedCapacity) {
                DongXe model = DongXe.builder()
                                .id(30L)
                                .tenDongXe("Air Blade")
                                .loaiPhuongTien(LoaiPhuongTien.XE_MAY)
                                .soChoHanhKhachMacDinh(1)
                                .dangHoatDong(true)
                                .build();

                return PhuongTien.builder()
                                .id(id)
                                .bienSoXe("59A1-TEST" + id)
                                .mauSacThucTe("Đen")
                                .soChoHanhKhachDuocDuyet(approvedCapacity)
                                .trangThaiPhuongTien(TrangThaiPhuongTien.ACTIVE)
                                .nguoiDangKySuDung(owner)
                                .dongXe(model)
                                .build();
        }

        public static RoutePlan validRoutePlan() {
                GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
                LineString geometry = geometryFactory.createLineString(new Coordinate[] {
                                new Coordinate(106.660172, 10.762622),
                                new Coordinate(106.629664, 10.823099)
                });
                geometry.setSRID(4326);
                return new RoutePlan(
                                geometry,
                                new BigDecimal("12500"),
                                2100,
                                List.of(new RoutePlanLeg(
                                                1,
                                                RouteWaypointRole.DRIVER_ORIGIN,
                                                RouteWaypointRole.DRIVER_DESTINATION,
                                                new BigDecimal("12500"),
                                                2100,
                                                false)),
                                List.of(),
                                new RouteBounds(
                                                new BigDecimal("106.629664"),
                                                new BigDecimal("10.762622"),
                                                new BigDecimal("106.660172"),
                                                new BigDecimal("10.823099")));
        }

        public static com.zanh.route_sharing.domain.entity.LoTrinhChiaSe persistedOpenRoute(
                        NguoiDung actor,
                        PhuongTien vehicle,
                        CreateSharedRouteRequest request,
                        RoutePlan plan,
                        long id) {
                GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
                var origin = geometryFactory.createPoint(new Coordinate(
                                request.origin().longitude().doubleValue(),
                                request.origin().latitude().doubleValue()));
                origin.setSRID(4326);
                var destination = geometryFactory.createPoint(new Coordinate(
                                request.driverDestination().longitude().doubleValue(),
                                request.driverDestination().latitude().doubleValue()));
                destination.setSRID(4326);
                var route = com.zanh.route_sharing.domain.entity.LoTrinhChiaSe.open(
                                actor, vehicle, origin, request.origin().address(), destination,
                                request.driverDestination().address(), (LineString) plan.geometry().copy(),
                                plan.distanceMeters(), plan.durationSeconds(), request.expectedDepartureTime(),
                                request.offeredSeats(), request.suggestedSupportPerKm());
                route.setId(id);
                route.setCreatedAt(NOW);
                return route;
        }

        public static RouteEndpointRequest endpoint(String latitude, String longitude, String address) {
                return new RouteEndpointRequest(new BigDecimal(latitude), new BigDecimal(longitude), address);
        }
}
