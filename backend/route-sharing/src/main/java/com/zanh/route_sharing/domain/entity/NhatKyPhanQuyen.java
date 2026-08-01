package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiThaoTacPhanQuyen;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "nhat_ky_phan_quyen", indexes = {
        @Index(name = "idx_nhat_ky_phan_quyen_thuc_hien", columnList = "nguoi_thuc_hien_id,thuc_hien_luc"),
        @Index(name = "idx_nhat_ky_phan_quyen_bi_tac_dong", columnList = "nguoi_bi_tac_dong_id,thuc_hien_luc")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class NhatKyPhanQuyen extends Base {
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_thao_tac", nullable = false, length = 50)
    private LoaiThaoTacPhanQuyen loaiThaoTac;
    @Column(name = "thuc_hien_luc", nullable = false)
    private Instant thucHienLuc;
    @Column(name = "ly_do", length = 2000)
    private String lyDo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_thuc_hien_id", nullable = false)
    private NguoiDung nguoiThucHien;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_bi_tac_dong_id")
    private NguoiDung nguoiBiTacDong;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhom_quyen_id")
    private NhomQuyen nhomQuyen;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quyen_han_id")
    private QuyenHan quyenHan;
}
