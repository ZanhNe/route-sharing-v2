package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Table(name = "su_co_chuyen_di", indexes = {
        @Index(name = "idx_su_co_chuyen_di_chuyen", columnList = "chuyen_di_id"),
        @Index(name = "idx_su_co_chuyen_di_trang_thai", columnList = "trang_thai_xu_ly,muc_do")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class SuCoChuyenDi extends Base {
    @Enumerated(EnumType.STRING)
    @Column(name = "nguon_phat_hien", nullable = false, length = 20)
    private NguonPhatHienSuCo nguonPhatHien;
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_su_co", nullable = false, length = 50)
    private LoaiSuCo loaiSuCo;
    @Enumerated(EnumType.STRING)
    @Column(name = "muc_do", nullable = false, length = 20)
    private MucDoSuCo mucDo;
    @Column(name = "xay_ra_luc", nullable = false)
    private Instant xayRaLuc;
    @Column(name = "toa_do_xay_ra", columnDefinition = "geometry(Point,4326)")
    private Point toaDoXayRa;
    @Column(name = "noi_dung", nullable = false, length = 5000)
    private String noiDung;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_xu_ly", nullable = false, length = 30)
    private TrangThaiXuLySuCo trangThaiXuLy = TrangThaiXuLySuCo.OPEN;
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
    @JoinColumn(name = "nguoi_bao_cao_id")
    private NguoiDung nguoiBaoCao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_bi_bao_cao_id")
    private NguoiDung nguoiBiBaoCao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_tiep_nhan_id")
    private NguoiDung nguoiTiepNhan;
}
