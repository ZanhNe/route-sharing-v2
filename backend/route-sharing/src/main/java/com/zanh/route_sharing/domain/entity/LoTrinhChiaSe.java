package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "lo_trinh_chia_se", indexes = {
        @Index(name = "idx_lo_trinh_tai_xe", columnList = "tai_xe_id"),
        @Index(name = "idx_lo_trinh_phuong_tien", columnList = "phuong_tien_id"),
        @Index(name = "idx_lo_trinh_trang_thai_khoi_hanh", columnList = "trang_thai_lo_trinh,thoi_gian_khoi_hanh_du_kien")
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
}
