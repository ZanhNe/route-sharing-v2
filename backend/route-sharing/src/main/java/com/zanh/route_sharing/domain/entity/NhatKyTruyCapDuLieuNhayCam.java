package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "nhat_ky_truy_cap_du_lieu_nhay_cam", indexes = {
        @Index(name = "idx_nhat_ky_truy_cap_nguoi_dung", columnList = "nguoi_truy_cap_id,truy_cap_luc"),
        @Index(name = "idx_nhat_ky_truy_cap_tai_nguyen", columnList = "loai_tai_nguyen,tai_nguyen_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class NhatKyTruyCapDuLieuNhayCam extends Base {
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_tai_nguyen", nullable = false, length = 40)
    private LoaiTaiNguyenNhayCam loaiTaiNguyen;
    @Column(name = "tai_nguyen_id", nullable = false)
    private Long taiNguyenId;
    @Enumerated(EnumType.STRING)
    @Column(name = "hanh_dong", nullable = false, length = 20)
    private HanhDongTruyCap hanhDong;
    @Column(name = "muc_dich", length = 1000)
    private String mucDich;
    @Column(name = "truy_cap_luc", nullable = false)
    private Instant truyCapLuc;
    @Column(name = "dia_chi_ip", nullable = false, length = 64)
    private String diaChiIp;
    @Column(name = "thong_tin_trinh_duyet", nullable = false, length = 1000)
    private String thongTinTrinhDuyet;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_truy_cap_id", nullable = false)
    private NguoiDung nguoiTruyCap;
}
