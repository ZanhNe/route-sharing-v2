package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "quyet_dinh_ky_luat", indexes = {
        @Index(name = "idx_quyet_dinh_nguoi_bi_xu_ly", columnList = "nguoi_bi_xu_ly_id"),
        @Index(name = "idx_quyet_dinh_trang_thai", columnList = "trang_thai_quyet_dinh")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class QuyetDinhKyLuat extends Base {
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_ky_luat", nullable = false, length = 50)
    private LoaiKyLuat loaiKyLuat;
    @Column(name = "ly_do", nullable = false, length = 5000)
    private String lyDo;
    @Column(name = "hieu_luc_tu", nullable = false)
    private Instant hieuLucTu;
    @Column(name = "hieu_luc_den")
    private Instant hieuLucDen;
    @Column(name = "ra_quyet_dinh_luc", nullable = false)
    private Instant raQuyetDinhLuc;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_quyet_dinh", nullable = false, length = 30)
    private TrangThaiQuyetDinh trangThaiQuyetDinh = TrangThaiQuyetDinh.DRAFT;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_bi_xu_ly_id", nullable = false)
    private NguoiDung nguoiBiXuLy;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_ra_quyet_dinh_id", nullable = false)
    private NguoiDung nguoiRaQuyetDinh;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khieu_nai_id")
    private KhieuNai khieuNai;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "su_co_chuyen_di_id")
    private SuCoChuyenDi suCoChuyenDi;
}
