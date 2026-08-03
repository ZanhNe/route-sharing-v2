package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "yeu_cau_di_chung", indexes = {
        @Index(name = "idx_yeu_cau_lo_trinh_trang_thai", columnList = "lo_trinh_chia_se_id,trang_thai_yeu_cau"),
        @Index(name = "idx_yeu_cau_chuyen_di", columnList = "chuyen_di_id"),
        @Index(name = "idx_yeu_cau_hanh_khach", columnList = "hanh_khach_id")
}, check = {
        @CheckConstraint(name = "ck_yeu_cau_ty_le", constraint = "ty_le_tien_duong BETWEEN 0 AND 100"),
        @CheckConstraint(name = "ck_yeu_cau_khoang_cach", constraint = "khoang_cach_lech_de_don_met >= 0 "
                + "AND thoi_gian_lech_de_don_giay >= 0 "
                + "AND tong_khoang_cach_mong_muon_met > 0 "
                + "AND khoang_cach_duoc_phuc_vu_met >= 0 "
                + "AND khoang_cach_con_lai_met >= 0"),
        @CheckConstraint(name = "ck_yeu_cau_matching", constraint = "(loai_ghep_tuyen = 'CUNG_DIEM_DEN' "
                + "AND loai_diem_tha = 'DIEM_DICH_CUOI_CUNG' "
                + "AND khoang_cach_con_lai_met = 0) "
                + "OR (loai_ghep_tuyen = 'TRUNG_DOAN_TUYEN' "
                + "AND loai_diem_tha = 'DIEM_THA_TRUNG_GIAN' "
                + "AND khoang_cach_con_lai_met > 0)"),
        @CheckConstraint(name = "ck_yeu_cau_muc_ho_tro", constraint = "muc_ho_tro_de_xuat >= 0 "
                + "AND (muc_ho_tro_da_thoa_thuan IS NULL OR muc_ho_tro_da_thoa_thuan >= 0)")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class YeuCauDiChung extends Base {
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_ghep_tuyen", nullable = false, length = 30)
    private LoaiGhepTuyen loaiGhepTuyen;
    @Column(name = "diem_don_thuc_te", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point diemDonThucTe;
    @Column(name = "dia_chi_don_thuc_te", nullable = false, length = 500)
    private String diaChiDonThucTe;
    @Column(name = "diem_dich_cuoi_cung_mong_muon", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point diemDichCuoiCungMongMuon;
    @Column(name = "dia_chi_dich_cuoi_cung", nullable = false, length = 500)
    private String diaChiDichCuoiCung;
    @Column(name = "diem_tha_thoa_thuan", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point diemThaThoaThuan;
    @Column(name = "dia_chi_diem_tha", nullable = false, length = 500)
    private String diaChiDiemTha;
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_diem_tha", nullable = false, length = 40)
    private LoaiDiemTha loaiDiemTha;
    @Column(name = "tuyen_duong_mong_muon_hanh_khach", nullable = false, columnDefinition = "geometry(LineString,4326)")
    private LineString tuyenDuongMongMuonHanhKhach;
    @Column(name = "doan_tuyen_duoc_phuc_vu", nullable = false, columnDefinition = "geometry(LineString,4326)")
    private LineString doanTuyenDuocPhucVu;
    @Column(name = "ty_le_tien_duong", nullable = false, precision = 5, scale = 2)
    private BigDecimal tyLeTienDuong;
    @Column(name = "khoang_cach_lech_de_don_met", nullable = false, precision = 14, scale = 2)
    private BigDecimal khoangCachLechDeDonMet;
    @Column(name = "thoi_gian_lech_de_don_giay", nullable = false)
    private Long thoiGianLechDeDonGiay;
    @Column(name = "tong_khoang_cach_mong_muon_met", nullable = false, precision = 14, scale = 2)
    private BigDecimal tongKhoangCachMongMuonMet;
    @Column(name = "khoang_cach_duoc_phuc_vu_met", nullable = false, precision = 14, scale = 2)
    private BigDecimal khoangCachDuocPhucVuMet;
    @Column(name = "khoang_cach_con_lai_met", nullable = false, precision = 14, scale = 2)
    private BigDecimal khoangCachConLaiMet;
    @Column(name = "muc_ho_tro_de_xuat", nullable = false, precision = 15, scale = 2)
    private BigDecimal mucHoTroDeXuat;
    @Column(name = "muc_ho_tro_da_thoa_thuan", precision = 15, scale = 2)
    private BigDecimal mucHoTroDaThoaThuan;
    @Column(name = "ghi_chu", length = 1000)
    private String ghiChu;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_yeu_cau", nullable = false, length = 40)
    private TrangThaiYeuCau trangThaiYeuCau = TrangThaiYeuCau.PENDING;
    @Column(name = "gui_luc", nullable = false)
    private Instant guiLuc;
    @Column(name = "chap_nhan_luc")
    private Instant chapNhanLuc;
    @Column(name = "huy_luc")
    private Instant huyLuc;
    @Column(name = "ly_do_huy", length = 2000)
    private String lyDoHuy;
    @Column(name = "tai_xe_xac_nhan_don_luc")
    private Instant taiXeXacNhanDonLuc;
    @Column(name = "hanh_khach_xac_nhan_don_luc")
    private Instant hanhKhachXacNhanDonLuc;
    @Column(name = "len_xe_luc")
    private Instant lenXeLuc;
    @Column(name = "tai_xe_xac_nhan_tra_luc")
    private Instant taiXeXacNhanTraLuc;
    @Column(name = "hanh_khach_xac_nhan_tra_luc")
    private Instant hanhKhachXacNhanTraLuc;
    @Column(name = "xuong_xe_luc")
    private Instant xuongXeLuc;
    @Column(name = "ly_do_xac_nhan_that_bai", length = 2000)
    private String lyDoXacNhanThatBai;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hanh_khach_id", nullable = false)
    private NguoiDung hanhKhach;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lo_trinh_chia_se_id", nullable = false)
    private LoTrinhChiaSe loTrinhChiaSe;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chuyen_di_id")
    private ChuyenDi chuyenDi;
}
