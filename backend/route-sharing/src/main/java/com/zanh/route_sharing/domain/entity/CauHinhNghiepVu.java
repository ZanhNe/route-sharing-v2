package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "cau_hinh_nghiep_vu", uniqueConstraints = {
                @UniqueConstraint(name = "uk_cau_hinh_nghiep_vu_truong", columnNames = "nha_truong_id")
}, check = {
                @CheckConstraint(name = "ck_cau_hinh_ty_le_tien_duong", constraint = "ty_le_tien_duong_toi_thieu BETWEEN 0 AND 100"),
                @CheckConstraint(name = "ck_cau_hinh_gia_tri_duong", constraint = "ban_kinh_cung_diem_den_met > 0 "
                                + "AND ban_kinh_diem_den_gan_tuyen_met > 0 "
                                + "AND khoang_cach_lech_don_toi_da_met >= 0 "
                                + "AND thoi_gian_lech_don_toi_da_giay >= 0 "
                                + "AND ban_kinh_xac_dinh_da_den_met > 0 "
                                + "AND thoi_gian_cho_khach_giay >= 0 "
                                + "AND thoi_gian_mat_tin_hieu_giay > 0 "
                                + "AND so_ngay_luu_vi_tri > 0 "
                                + "AND so_ngay_luu_nhat_ky > 0 "
                                + "AND chu_ky_gui_vi_tri_giay > 0 "
                                + "AND thoi_gian_tre_tin_hieu_giay > chu_ky_gui_vi_tri_giay "
                                + "AND thoi_gian_mat_tin_hieu_giay > thoi_gian_tre_tin_hieu_giay"),
                @CheckConstraint(name = "ck_cau_hinh_yeu_cau_di_chung", constraint = "booking_cutoff_seconds >= 0 "
                                + "AND rejection_cooldown_seconds >= 0"),
                @CheckConstraint(name = "ck_cau_hinh_khieu_nai", constraint = "thoi_han_nop_khieu_nai_gio > 0"),
                @CheckConstraint(name = "ck_cau_hinh_xu_ly_khieu_nai", constraint = "thoi_han_phan_hoi_khieu_nai_gio > 0")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class CauHinhNghiepVu extends Base {
        @Column(name = "ban_kinh_cung_diem_den_met", nullable = false, precision = 12, scale = 2)
        private BigDecimal banKinhCungDiemDenMet;
        @Column(name = "ban_kinh_diem_den_gan_tuyen_met", nullable = false, precision = 12, scale = 2)
        private BigDecimal banKinhDiemDenGanTuyenMet;
        @Column(name = "ty_le_tien_duong_toi_thieu", nullable = false, precision = 5, scale = 2)
        private BigDecimal tyLeTienDuongToiThieu;
        @Column(name = "khoang_cach_lech_don_toi_da_met", nullable = false, precision = 12, scale = 2)
        private BigDecimal khoangCachLechDonToiDaMet;
        @Column(name = "thoi_gian_lech_don_toi_da_giay", nullable = false)
        private Long thoiGianLechDonToiDaGiay;
        @Column(name = "ban_kinh_xac_dinh_da_den_met", nullable = false, precision = 12, scale = 2)
        private BigDecimal banKinhXacDinhDaDenMet;
        @Column(name = "thoi_gian_cho_khach_giay", nullable = false)
        private Long thoiGianChoKhachGiay;
        @Column(name = "thoi_gian_tre_tin_hieu_giay", nullable = false)
        private Long thoiGianTreTinHieuGiay;
        @Column(name = "thoi_gian_mat_tin_hieu_giay", nullable = false)
        private Long thoiGianMatTinHieuGiay;
        @Column(name = "do_lech_thoi_gian_khoi_hanh_phut", nullable = false)
        private Integer doLechThoiGianKhoiHanhPhut;
        @Column(name = "so_ngay_luu_vi_tri", nullable = false)
        private Integer soNgayLuuViTri;
        @Column(name = "chu_ky_gui_vi_tri_giay", nullable = false)
        private Long chuKyGuiViTriGiay;
        @Column(name = "so_ngay_luu_nhat_ky", nullable = false)
        private Integer soNgayLuuNhatKy;
        @Column(name = "booking_cutoff_seconds", nullable = false)
        private Long bookingCutoffSeconds;
        @Column(name = "rejection_cooldown_seconds", nullable = false)
        private Long rejectionCooldownSeconds;
        @Column(name = "thoi_han_nop_khieu_nai_gio", nullable = false)
        private Long thoiHanNopKhieuNaiGio;
        @Column(name = "thoi_han_phan_hoi_khieu_nai_gio", nullable = false)
        private Long thoiHanPhanHoiKhieuNaiGio;
        @Builder.Default
        @Column(name = "bat_buoc_tep_xac_nhan_chu_xe", nullable = false)
        private Boolean batBuocTepXacNhanChuXeKhiKhongChinhChu = false;
        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "nha_truong_id", nullable = false, unique = true)
        private NhaTruong nhaTruong;
}
