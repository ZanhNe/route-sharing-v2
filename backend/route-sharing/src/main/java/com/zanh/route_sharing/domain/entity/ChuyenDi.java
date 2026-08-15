package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chuyen_di", uniqueConstraints = {
                @UniqueConstraint(name = "uk_chuyen_di_lo_trinh", columnNames = "lo_trinh_chia_se_id")
}, indexes = {
                @Index(name = "idx_chuyen_di_trang_thai", columnList = "trang_thai_van_hanh,trang_thai_giam_sat")
}, check = @CheckConstraint(name = "ck_chuyen_di_so_khach", constraint = "so_khach_ke_hoach >= 0 "
                + "AND so_khach_thuc_te >= 0 "
                + "AND so_khach_thuc_te <= so_khach_ke_hoach"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ChuyenDi extends Base {
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "trang_thai_van_hanh", nullable = false, length = 40)
        private TrangThaiVanHanhChuyenDi trangThaiVanHanh = TrangThaiVanHanhChuyenDi.PREPARING;
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "trang_thai_giam_sat", nullable = false, length = 30)
        private TrangThaiGiamSatChuyenDi trangThaiGiamSat = TrangThaiGiamSatChuyenDi.NORMAL;
        @Column(name = "so_khach_ke_hoach", nullable = false)
        private Integer soKhachKeHoach;
        @Builder.Default
        @Column(name = "so_khach_thuc_te", nullable = false)
        private Integer soKhachThucTe = 0;
        @Column(name = "tuyen_duong_van_hanh", nullable = false, columnDefinition = "geometry(LineString,4326)")
        private LineString tuyenDuongVanHanh;
        @Column(name = "tuyen_duong_thuc_te_tong_hop", columnDefinition = "geometry(LineString,4326)")
        private LineString tuyenDuongThucTeTongHop;
        @Column(name = "bat_dau_luc")
        private Instant batDauLuc;
        @Column(name = "ket_thuc_luc")
        private Instant ketThucLuc;
        @Column(name = "vi_tri_cuoi_cung", columnDefinition = "geometry(Point,4326)")
        private Point viTriCuoiCung;
        @Column(name = "nhan_tin_hieu_cuoi_luc")
        private Instant nhanTinHieuCuoiLuc;
        @Column(name = "dong_bang_luc")
        private Instant dongBangLuc;
        @Column(name = "ly_do_dong_bang", length = 2000)
        private String lyDoDongBang;
        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "lo_trinh_chia_se_id", nullable = false, unique = true)
        private LoTrinhChiaSe loTrinhChiaSe;
        @Builder.Default
        @OneToMany(mappedBy = "chuyenDi", cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderBy("thuTu ASC")
        private List<DiemDungHanhTrinh> danhSachDiemDung = new ArrayList<>();

        public static ChuyenDi preparing(
                        LoTrinhChiaSe route,
                        int plannedPassengerCount,
                        LineString operationalRoute) {
                if (route == null || route.getId() == null) {
                        throw new IllegalArgumentException("Lộ trình phải được lưu trước khi hình thành chuyến đi.");
                }
                if (plannedPassengerCount <= 0) {
                        throw new IllegalArgumentException(
                                        "Chuyến đi chia sẻ phải có ít nhất một hành khách kế hoạch.");
                }
                if (operationalRoute == null || operationalRoute.isEmpty()
                                || operationalRoute.getNumPoints() < 2
                                || operationalRoute.getLength() <= 0.0d
                                || operationalRoute.getSRID() != Wgs84Coordinates.SRID) {
                        throw new IllegalArgumentException("Tuyến vận hành phải là LineString SRID 4326 hợp lệ.");
                }
                LineString routeCopy = (LineString) operationalRoute.copy();
                routeCopy.setSRID(Wgs84Coordinates.SRID);
                ChuyenDi trip = new ChuyenDi();
                trip.loTrinhChiaSe = route;
                trip.trangThaiVanHanh = TrangThaiVanHanhChuyenDi.PREPARING;
                trip.trangThaiGiamSat = TrangThaiGiamSatChuyenDi.NORMAL;
                trip.soKhachKeHoach = plannedPassengerCount;
                trip.soKhachThucTe = 0;
                trip.tuyenDuongVanHanh = routeCopy;
                return trip;
        }

        public void start(Instant startedAt) {
                if (startedAt == null) {
                        throw new IllegalArgumentException("startedAt không được trống.");
                }
                if (this.batDauLuc != null) {
                        throw new IllegalStateException("Chuyến đi đã được bắt đầu trước đó.");
                }
                if (this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.PREPARING) {
                        throw new IllegalStateException("Chỉ chuyến PREPARING mới có thể bắt đầu.");
                }
                if (this.loTrinhChiaSe == null
                                || this.loTrinhChiaSe.getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                                || this.loTrinhChiaSe.getChuyenDi() != this) {
                        throw new IllegalStateException("Chuyến PREPARING phải thuộc lộ trình LOCKED tương ứng.");
                }
                if (this.soKhachThucTe == null || this.soKhachThucTe != 0) {
                        throw new IllegalStateException("Số khách thực tế trước Start phải bằng 0.");
                }
                this.trangThaiVanHanh = TrangThaiVanHanhChuyenDi.IN_PROGRESS;
                this.batDauLuc = startedAt;
        }

        public void boardOnePassenger() {
                if (this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.IN_PROGRESS || this.batDauLuc == null) {
                        throw new IllegalStateException("Chỉ chuyến IN_PROGRESS đã Start mới ghi nhận Passenger lên xe.");
                }
                if (this.loTrinhChiaSe == null
                                || this.loTrinhChiaSe.getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                                || this.loTrinhChiaSe.getChuyenDi() != this) {
                        throw new IllegalStateException("Chuyến IN_PROGRESS phải thuộc lộ trình LOCKED tương ứng.");
                }
                if (this.soKhachThucTe == null || this.soKhachKeHoach == null
                                || this.soKhachThucTe < 0
                                || this.soKhachThucTe >= this.soKhachKeHoach) {
                        throw new IllegalStateException("Số khách thực tế không thể tăng thêm.");
                }
                this.soKhachThucTe += 1;
        }


        public void recordCurrentLocation(Point location, Instant receivedAt) {
                if (location == null || location.isEmpty() || location.getSRID() != Wgs84Coordinates.SRID) {
                        throw new IllegalArgumentException("Vị trí hiện tại phải là Point WGS84 SRID 4326 hợp lệ.");
                }
                if (receivedAt == null) {
                        throw new IllegalArgumentException("receivedAt không được trống.");
                }
                if (this.batDauLuc == null
                                || this.ketThucLuc != null
                                || (this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                                                && this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN)) {
                        throw new IllegalStateException("Chuyến đi hiện không thuộc tracking lifecycle.");
                }
                Point copy = (Point) location.copy();
                copy.setSRID(Wgs84Coordinates.SRID);
                this.viTriCuoiCung = copy;
                this.nhanTinHieuCuoiLuc = receivedAt;
        }

        public void transitionMonitoringState(TrangThaiGiamSatChuyenDi desiredState) {
                if (desiredState == null) {
                        throw new IllegalArgumentException("desiredState không được trống.");
                }
                if (this.batDauLuc == null
                                || this.ketThucLuc != null
                                || (this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                                                && this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN)) {
                        throw new IllegalStateException("Chuyến đi hiện không thuộc monitoring lifecycle.");
                }
                if (this.trangThaiGiamSat == desiredState) {
                        throw new IllegalStateException("Monitoring state phải thực sự thay đổi.");
                }
                this.trangThaiGiamSat = desiredState;
        }

        public void batDauGiuAnToan(Instant startedAt, String participantSafeReason) {
                if (startedAt == null) throw new IllegalArgumentException("startedAt không được trống.");
                String reason = participantSafeReason == null ? null : participantSafeReason.trim();
                if (reason == null || reason.isEmpty() || reason.length() > 2000) {
                        throw new IllegalArgumentException("participantSafeReason phải có nội dung <= 2000 ký tự.");
                }
                if (this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.IN_PROGRESS || this.batDauLuc == null || this.ketThucLuc != null) {
                        throw new IllegalStateException("Chỉ Trip IN_PROGRESS đã Start mới có thể bắt đầu Safety hold.");
                }
                if (this.dongBangLuc != null || this.lyDoDongBang != null) {
                        throw new IllegalStateException("Trip đã có current Safety hold projection.");
                }
                this.trangThaiVanHanh = TrangThaiVanHanhChuyenDi.SECURITY_FROZEN;
                this.dongBangLuc = startedAt;
                this.lyDoDongBang = reason;
        }

        public void ketThucGiuAnToanVaTiepTuc() {
                if (this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN || this.ketThucLuc != null
                                || this.dongBangLuc == null || this.lyDoDongBang == null || this.lyDoDongBang.isBlank()) {
                        throw new IllegalStateException("Trip không có active Safety hold hợp lệ để tiếp tục.");
                }
                this.trangThaiVanHanh = TrangThaiVanHanhChuyenDi.IN_PROGRESS;
                this.dongBangLuc = null;
                this.lyDoDongBang = null;
        }

        public void giamMotKhachDangTrenXe() {
                if (this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN || this.soKhachThucTe == null || this.soKhachThucTe <= 0) {
                        throw new IllegalStateException("Trip frozen không có Passenger ON_BOARD để giảm.");
                }
                this.soKhachThucTe -= 1;
        }

        public void huyKhanCap(Instant endedAt) {
                if (endedAt == null) throw new IllegalArgumentException("endedAt không được trống.");
                if (this.batDauLuc == null || endedAt.isBefore(this.batDauLuc) || this.ketThucLuc != null
                                || (this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                                && this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN)) {
                        throw new IllegalStateException("Trip không ở lifecycle cho phép emergency abort.");
                }
                this.trangThaiVanHanh = TrangThaiVanHanhChuyenDi.EMERGENCY_ABORTED;
                this.ketThucLuc = endedAt;
                this.soKhachThucTe = 0;
                this.dongBangLuc = null;
                this.lyDoDongBang = null;
        }

        public void cancelBeforeStart() {
                if (this.batDauLuc != null) {
                        throw new IllegalStateException("Chuyến đi đã được bắt đầu và không thể hủy trước Start.");
                }
                if (this.trangThaiVanHanh != TrangThaiVanHanhChuyenDi.PREPARING) {
                        throw new IllegalStateException("Chỉ chuyến PREPARING mới có thể hủy trước Start.");
                }
                if (this.loTrinhChiaSe == null
                                || this.loTrinhChiaSe.getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                                || this.loTrinhChiaSe.getChuyenDi() != this) {
                        throw new IllegalStateException("Chuyến PREPARING phải thuộc lộ trình LOCKED tương ứng.");
                }
                if (this.soKhachThucTe == null || this.soKhachThucTe != 0) {
                        throw new IllegalStateException("Số khách thực tế trước khi hủy phải bằng 0.");
                }
                this.trangThaiVanHanh = TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START;
        }

}
