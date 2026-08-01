package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "cau_hinh_nghiep_vu", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cau_hinh_nghiep_vu_truong", columnNames = "nha_truong_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class CauHinhNghiepVu extends Base {
    @Column(name = "ban_kinh_cung_diem_den_met", nullable = false, precision = 12, scale = 2)
    private BigDecimal banKinhCungDiemDenMet;
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
    @Column(name = "thoi_gian_mat_tin_hieu_giay", nullable = false)
    private Long thoiGianMatTinHieuGiay;
    @Column(name = "do_lech_thoi_gian_khoi_hanh_phut", nullable = false)
    private Integer doLechThoiGianKhoiHanhPhut;
    @Column(name = "so_ngay_luu_vi_tri", nullable = false)
    private Integer soNgayLuuViTri;
    @Column(name = "so_ngay_luu_nhat_ky", nullable = false)
    private Integer soNgayLuuNhatKy;
    @Builder.Default
    @Column(name = "bat_buoc_tep_xac_nhan_chu_xe", nullable = false)
    private Boolean batBuocTepXacNhanChuXeKhiKhongChinhChu = false;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nha_truong_id", nullable = false, unique = true)
    private NhaTruong nhaTruong;
}
