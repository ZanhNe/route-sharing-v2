package com.zanh.route_sharing.testfixture;

import com.zanh.route_sharing.domain.entity.DongXe;
import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiXe;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import com.zanh.route_sharing.integration.goong.RouteCalculation;
import com.zanh.route_sharing.integration.goong.RouteCoordinate;

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

        public static HoSoTaiXe activeDriverProfile(
                        Long id,
                        NguoiDung user) {
                return HoSoTaiXe.builder()
                                .id(id)
                                .nguoiDung(user)
                                .ngayDangKy(NOW.minusSeconds(172800))
                                .ngayDuocDuyet(NOW.minusSeconds(86400))
                                .trangThaiTaiXe(TrangThaiTaiXe.ACTIVE)
                                .build();
        }

        public static PhuongTien activeMotorbike(
                        Long id,
                        NguoiDung owner,
                        int approvedCapacity) {
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

        public static RouteCalculation validCalculation() {
                return new RouteCalculation(
                                List.of(
                                                coordinate("10.762622", "106.660172"),
                                                coordinate("10.823099", "106.629664")),
                                new BigDecimal("12500"),
                                2100);
        }

        public static RouteEndpointRequest endpoint(
                        String latitude,
                        String longitude,
                        String address) {
                return new RouteEndpointRequest(
                                new BigDecimal(latitude),
                                new BigDecimal(longitude),
                                address);
        }

        public static RouteCoordinate coordinate(
                        String latitude,
                        String longitude) {
                return new RouteCoordinate(
                                new BigDecimal(latitude),
                                new BigDecimal(longitude));
        }
}
