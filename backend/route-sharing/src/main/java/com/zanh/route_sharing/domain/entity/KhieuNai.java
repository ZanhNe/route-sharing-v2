package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiKhieuNai;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "khieu_nai", indexes = {
        @Index(name = "idx_khieu_nai_chuyen", columnList = "chuyen_di_id"),
        @Index(name = "idx_khieu_nai_trang_thai", columnList = "trang_thai_khieu_nai")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class KhieuNai extends Base {
    @Column(name = "tieu_de", nullable = false, length = 255)
    private String tieuDe;
    @Column(name = "noi_dung", nullable = false, length = 5000)
    private String noiDung;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_khieu_nai", nullable = false, length = 40)
    private TrangThaiKhieuNai trangThaiKhieuNai = TrangThaiKhieuNai.SUBMITTED;
    @Column(name = "nop_luc", nullable = false)
    private Instant nopLuc;
    @Column(name = "tiep_nhan_luc")
    private Instant tiepNhanLuc;
    @Column(name = "ket_luan", length = 5000)
    private String ketLuan;
    @Column(name = "giai_quyet_luc")
    private Instant giaiQuyetLuc;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chuyen_di_id", nullable = false)
    private ChuyenDi chuyenDi;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yeu_cau_di_chung_id")
    private YeuCauDiChung yeuCauDiChung;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "su_co_chuyen_di_id")
    private SuCoChuyenDi suCoChuyenDi;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_khieu_nai_id", nullable = false)
    private NguoiDung nguoiKhieuNai;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_bi_khieu_nai_id")
    private NguoiDung nguoiBiKhieuNai;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_tiep_nhan_id")
    private NguoiDung nguoiTiepNhan;
}
