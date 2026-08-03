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
}
