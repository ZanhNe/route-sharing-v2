package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "diem_dung_hanh_trinh", uniqueConstraints = {
                @UniqueConstraint(name = "uk_diem_dung_thu_tu", columnNames = { "chuyen_di_id", "thu_tu" }),
                @UniqueConstraint(name = "uk_diem_dung_booking_loai", columnNames = { "yeu_cau_di_chung_id",
                                "loai_diem_dung" })
}, indexes = {
                @Index(name = "idx_diem_dung_chuyen_trang_thai", columnList = "chuyen_di_id,trang_thai_diem_dung")
}, check = {
                @CheckConstraint(name = "ck_diem_dung_thu_tu", constraint = "thu_tu >= 0"),
                @CheckConstraint(name = "ck_diem_dung_ban_kinh", constraint = "ban_kinh_xac_dinh_da_den_met > 0"),
                @CheckConstraint(name = "ck_diem_dung_booking", constraint = "(loai_diem_dung IN ('DRIVER_START','DRIVER_END') "
                                + "AND yeu_cau_di_chung_id IS NULL) "
                                + "OR (loai_diem_dung IN ('PICKUP','DROPOFF') "
                                + "AND yeu_cau_di_chung_id IS NOT NULL)")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class DiemDungHanhTrinh extends Base {
        @Column(name = "thu_tu", nullable = false)
        private Integer thuTu;
        @Column(name = "toa_do_ke_hoach", nullable = false, columnDefinition = "geometry(Point,4326)")
        private Point toaDoKeHoach;
        @Column(name = "toa_do_thuc_te", columnDefinition = "geometry(Point,4326)")
        private Point toaDoThucTe;
        @Column(name = "dia_chi", nullable = false, length = 500)
        private String diaChi;
        @Enumerated(EnumType.STRING)
        @Column(name = "loai_diem_dung", nullable = false, length = 30)
        private LoaiDiemDung loaiDiemDung;
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "trang_thai_diem_dung", nullable = false, length = 30)
        private TrangThaiDiemDung trangThaiDiemDung = TrangThaiDiemDung.PENDING;
        @Column(name = "ban_kinh_xac_dinh_da_den_met", nullable = false, precision = 12, scale = 2)
        private BigDecimal banKinhXacDinhDaDenMet;
        @Column(name = "den_gan_luc")
        private Instant denGanLuc;
        @Column(name = "den_luc")
        private Instant denLuc;
        @Column(name = "bat_dau_cho_luc")
        private Instant batDauChoLuc;
        @Column(name = "han_cho_luc")
        private Instant hanChoLuc;
        @Column(name = "hoan_thanh_luc")
        private Instant hoanThanhLuc;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "chuyen_di_id", nullable = false)
        private ChuyenDi chuyenDi;
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "yeu_cau_di_chung_id")
        private YeuCauDiChung yeuCauDiChung;
        public static DiemDungHanhTrinh planned(
                        ChuyenDi trip,
                        YeuCauDiChung rideRequest,
                        int order,
                        Point plannedPoint,
                        String address,
                        LoaiDiemDung type,
                        BigDecimal arrivalRadiusMeters) {
                if (trip == null) {
                        throw new IllegalArgumentException("trip không được trống");
                }
                if (order <= 0) {
                        throw new IllegalArgumentException("Thứ tự điểm dừng phải là số dương.");
                }
                if (plannedPoint == null || plannedPoint.isEmpty() || plannedPoint.getSRID() != 4326) {
                        throw new IllegalArgumentException("Điểm dừng phải là Point SRID 4326 hợp lệ.");
                }
                if (address == null || address.isBlank() || address.trim().length() > 500) {
                        throw new IllegalArgumentException("Địa chỉ điểm dừng không hợp lệ.");
                }
                if (type == null) {
                        throw new IllegalArgumentException("Loại điểm dừng không được trống.");
                }
                boolean driverBoundary = type == LoaiDiemDung.DRIVER_START || type == LoaiDiemDung.DRIVER_END;
                if (driverBoundary && rideRequest != null) {
                        throw new IllegalArgumentException("Điểm đầu/cuối tài xế không được gắn booking.");
                }
                if (!driverBoundary && rideRequest == null) {
                        throw new IllegalArgumentException("PICKUP/DROPOFF phải gắn booking.");
                }
                if (rideRequest != null && rideRequest.getChuyenDi() != trip) {
                        throw new IllegalArgumentException("Booking phải được gắn vào cùng chuyến đi trước khi tạo điểm dừng.");
                }
                if (arrivalRadiusMeters == null || arrivalRadiusMeters.signum() <= 0) {
                        throw new IllegalArgumentException("Bán kính xác định đã đến phải lớn hơn 0.");
                }
                Point pointCopy = (Point) plannedPoint.copy();
                pointCopy.setSRID(4326);
                DiemDungHanhTrinh stop = new DiemDungHanhTrinh();
                stop.chuyenDi = trip;
                stop.yeuCauDiChung = rideRequest;
                stop.thuTu = order;
                stop.toaDoKeHoach = pointCopy;
                stop.diaChi = address.trim();
                stop.loaiDiemDung = type;
                stop.trangThaiDiemDung = TrangThaiDiemDung.PENDING;
                stop.banKinhXacDinhDaDenMet = arrivalRadiusMeters;
                return stop;
        }

}
