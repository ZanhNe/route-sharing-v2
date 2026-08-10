package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "lo_trinh_chia_se", indexes = {
                @Index(name = "idx_lo_trinh_tai_xe", columnList = "tai_xe_id"),
                @Index(name = "idx_lo_trinh_phuong_tien", columnList = "phuong_tien_id"),
                @Index(name = "idx_lo_trinh_trang_thai_khoi_hanh", columnList = "trang_thai_lo_trinh,thoi_gian_khoi_hanh_du_kien")
}, check = {
                @CheckConstraint(name = "ck_lo_trinh_so_ghe", constraint = "so_ghe_cung_cap > 0 "
                                + "AND so_ghe_con_lai >= 0 "
                                + "AND so_ghe_con_lai <= so_ghe_cung_cap"),
                @CheckConstraint(name = "ck_lo_trinh_khoang_cach", constraint = "khoang_cach_du_kien_met >= 0 AND thoi_luong_du_kien_giay >= 0"),
                @CheckConstraint(name = "ck_lo_trinh_muc_ho_tro", constraint = "muc_ho_tro_goi_y_moi_km IS NULL OR muc_ho_tro_goi_y_moi_km >= 0"),
                @CheckConstraint(name = "ck_lo_trinh_huy", constraint = "trang_thai_lo_trinh <> 'CANCELLED' OR "
                                + "(huy_luc IS NOT NULL AND ly_do_huy IS NOT NULL "
                                + "AND char_length(btrim(ly_do_huy)) BETWEEN 1 AND 2000)")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class LoTrinhChiaSe extends Base {
        @Column(name = "diem_xuat_phat", nullable = false, columnDefinition = "geometry(Point,4326)")
        private Point diemXuatPhat;
        @Column(name = "dia_chi_xuat_phat", nullable = false, length = 500)
        private String diaChiXuatPhat;
        @Column(name = "diem_dich_tai_xe", nullable = false, columnDefinition = "geometry(Point,4326)")
        private Point diemDichTaiXe;
        @Column(name = "dia_chi_dich_tai_xe", nullable = false, length = 500)
        private String diaChiDichTaiXe;
        @Column(name = "tuyen_duong_goc", nullable = false, columnDefinition = "geometry(LineString,4326)")
        private LineString tuyenDuongGoc;
        @Column(name = "khoang_cach_du_kien_met", nullable = false, precision = 14, scale = 2)
        private BigDecimal khoangCachDuKienMet;
        @Column(name = "thoi_luong_du_kien_giay", nullable = false)
        private Long thoiLuongDuKienGiay;
        @Column(name = "thoi_gian_khoi_hanh_du_kien", nullable = false)
        private Instant thoiGianKhoiHanhDuKien;
        @Column(name = "so_ghe_cung_cap", nullable = false)
        private Integer soGheCungCap;
        @Column(name = "so_ghe_con_lai", nullable = false)
        private Integer soGheConLai;
        @Column(name = "muc_ho_tro_goi_y_moi_km", precision = 15, scale = 2)
        private BigDecimal mucHoTroGoiYMoiKm;
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "trang_thai_lo_trinh", nullable = false, length = 30)
        private TrangThaiLoTrinh trangThaiLoTrinh = TrangThaiLoTrinh.OPEN;
        @Column(name = "chot_danh_sach_luc")
        private Instant chotDanhSachLuc;
        @Column(name = "huy_luc")
        private Instant huyLuc;
        @Column(name = "ly_do_huy", length = 2000)
        private String lyDoHuy;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "tai_xe_id", nullable = false)
        private NguoiDung taiXe;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "phuong_tien_id", nullable = false)
        private PhuongTien phuongTien;
        @OneToOne(mappedBy = "loTrinhChiaSe", fetch = FetchType.LAZY)
        private ChuyenDi chuyenDi;

        public static LoTrinhChiaSe open(
                        NguoiDung driver,
                        PhuongTien vehicle,
                        Point origin,
                        String originAddress,
                        Point destination,
                        String destinationAddress,
                        LineString route,
                        BigDecimal distanceMeters,
                        long durationSeconds,
                        Instant expectedDepartureTime,
                        int offeredSeats,
                        BigDecimal suggestedSupportPerKm) {
                LoTrinhChiaSe entity = new LoTrinhChiaSe();
                entity.taiXe = Objects.requireNonNull(driver, "driver must not be null");
                entity.phuongTien = Objects.requireNonNull(vehicle, "vehicle must not be null");
                entity.diemXuatPhat = requirePoint(origin, "Điểm xuất phát");
                entity.diaChiXuatPhat = requireAddress(originAddress, "Địa chỉ xuất phát");
                entity.diemDichTaiXe = requirePoint(destination, "Điểm đích");
                entity.diaChiDichTaiXe = requireAddress(destinationAddress, "Địa chỉ đích");
                entity.tuyenDuongGoc = requireLineString(route);
                entity.khoangCachDuKienMet = requirePositive(distanceMeters, "Khoảng cách dự kiến");
                entity.thoiLuongDuKienGiay = requirePositive(durationSeconds, "Thời lượng dự kiến");
                entity.thoiGianKhoiHanhDuKien = Objects.requireNonNull(expectedDepartureTime,
                                "expectedDepartureTime must not be null");
                entity.soGheCungCap = requireSeatCount(vehicle, offeredSeats);
                entity.soGheConLai = offeredSeats;
                entity.mucHoTroGoiYMoiKm = requireNonNegativeOrNull(suggestedSupportPerKm);
                entity.trangThaiLoTrinh = TrangThaiLoTrinh.OPEN;
                return entity;
        }

        public void allocateOneSeat() {
                if (this.trangThaiLoTrinh != TrangThaiLoTrinh.OPEN) {
                        throw new IllegalStateException("Chỉ lộ trình OPEN mới được cấp ghế.");
                }
                if (this.soGheConLai == null || this.soGheConLai <= 0) {
                        throw new IllegalStateException("Lộ trình không còn ghế trống.");
                }
                this.soGheConLai = this.soGheConLai - 1;
        }

        public void releaseOneSeat() {
                if (this.trangThaiLoTrinh != TrangThaiLoTrinh.OPEN) {
                        throw new IllegalStateException("Chỉ lộ trình OPEN mới được hoàn ghế.");
                }
                if (this.soGheConLai == null || this.soGheCungCap == null
                                || this.soGheConLai >= this.soGheCungCap) {
                        throw new IllegalStateException("Không thể hoàn ghế vượt quá số ghế cung cấp.");
                }
                this.soGheConLai = this.soGheConLai + 1;
        }

        public void lockForTripFormation(ChuyenDi trip, Instant lockedAt) {
                Objects.requireNonNull(trip, "trip không được trống");
                Objects.requireNonNull(lockedAt, "lockedAt không được trống");
                if (this.trangThaiLoTrinh != TrangThaiLoTrinh.OPEN) {
                        throw new IllegalStateException("Chỉ lộ trình OPEN mới có thể khóa danh sách.");
                }
                if (this.chuyenDi != null) {
                        throw new IllegalStateException("Lộ trình đã hình thành chuyến đi thực tế.");
                }
                if (trip.getLoTrinhChiaSe() != this) {
                        throw new IllegalArgumentException("Chuyến đi phải được hình thành từ chính lộ trình này.");
                }
                this.trangThaiLoTrinh = TrangThaiLoTrinh.LOCKED;
                this.chotDanhSachLuc = lockedAt;
                this.chuyenDi = trip;
        }

        public void cancelByDriver(Instant cancelledAt, String reason) {
                Objects.requireNonNull(cancelledAt, "cancelledAt không được trống");
                if (this.trangThaiLoTrinh != TrangThaiLoTrinh.OPEN) {
                        throw new IllegalStateException("Chỉ lộ trình OPEN mới có thể được hủy.");
                }
                if (this.chuyenDi != null) {
                        throw new IllegalStateException("Lộ trình đã hình thành chuyến đi thực tế.");
                }
                if (reason == null || reason.isBlank()) {
                        throw new IllegalArgumentException("Lý do hủy không được để trống.");
                }
                String normalizedReason = reason.trim();
                if (normalizedReason.length() > 2000) {
                        throw new IllegalArgumentException("Lý do hủy không được vượt quá 2000 ký tự.");
                }
                this.trangThaiLoTrinh = TrangThaiLoTrinh.CANCELLED;
                this.huyLuc = cancelledAt;
                this.lyDoHuy = normalizedReason;
        }

        private static Point requirePoint(Point point, String fieldName) {
                if (point == null || point.isEmpty() || point.getSRID() != Wgs84Coordinates.SRID) {
                        throw new IllegalArgumentException(fieldName + " phải là Point WGS84 SRID 4326 hợp lệ.");
                }
                return point;
        }

        private static LineString requireLineString(LineString lineString) {
                if (lineString == null || lineString.isEmpty()
                                || lineString.getNumPoints() < 2 || lineString.getLength() == 0.0
                                || lineString.getSRID() != Wgs84Coordinates.SRID) {
                        throw new IllegalArgumentException("Tuyến đường phải là LineString WGS84 SRID 4326 hợp lệ.");
                }
                return lineString;
        }

        private static int requireSeatCount(PhuongTien vehicle, int offeredSeats) {
                requirePositive(offeredSeats, "Số ghế cung cấp");
                Integer approvedCapacity = vehicle.getSoChoHanhKhachDuocDuyet();
                if (approvedCapacity == null || offeredSeats > approvedCapacity) {
                        throw new IllegalArgumentException("Số ghế cung cấp vượt quá sức chứa đã được duyệt.");
                }
                return offeredSeats;
        }

        private static String requireAddress(String value, String fieldName) {
                if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException(fieldName + " không được để trống.");
                }
                String normalized = value.trim();
                if (normalized.length() > 500) {
                        throw new IllegalArgumentException(fieldName + " không được vượt quá 500 ký tự.");
                }
                return normalized;
        }

        private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
                if (value == null || value.signum() <= 0) {
                        throw new IllegalArgumentException(fieldName + " phải lớn hơn 0.");
                }
                return value;
        }

        private static long requirePositive(long value, String fieldName) {
                if (value <= 0) {
                        throw new IllegalArgumentException(fieldName + " phải lớn hơn 0.");
                }
                return value;
        }

        private static int requirePositive(int value, String fieldName) {
                if (value <= 0) {
                        throw new IllegalArgumentException(fieldName + " phải lớn hơn 0.");
                }
                return value;
        }

        private static BigDecimal requireNonNegativeOrNull(BigDecimal value) {
                if (value != null && value.signum() < 0) {
                        throw new IllegalArgumentException("Mức hỗ trợ gợi ý không được âm.");
                }
                return value;
        }
}
