package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "thong_bao", indexes = {
        @Index(name = "idx_thong_bao_nguoi_nhan_trang_thai", columnList = "nguoi_nhan_id,trang_thai_thong_bao"),
        @Index(name = "idx_thong_bao_doi_tuong", columnList = "loai_doi_tuong_lien_quan,doi_tuong_lien_quan_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ThongBao extends Base {
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_thong_bao", nullable = false, length = 50)
    private LoaiThongBao loaiThongBao;
    @Column(name = "tieu_de", nullable = false, length = 255)
    private String tieuDe;
    @Column(name = "noi_dung", nullable = false, length = 3000)
    private String noiDung;
    @Enumerated(EnumType.STRING)
    @Column(name = "kenh_gui", nullable = false, length = 20)
    private KenhThongBao kenhGui;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_thong_bao", nullable = false, length = 20)
    private TrangThaiThongBao trangThaiThongBao = TrangThaiThongBao.PENDING;
    @Column(name = "gui_luc")
    private Instant guiLuc;
    @Column(name = "doc_luc")
    private Instant docLuc;
    @Column(name = "loai_doi_tuong_lien_quan", length = 100)
    private String loaiDoiTuongLienQuan;
    @Column(name = "doi_tuong_lien_quan_id")
    private Long doiTuongLienQuanId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_nhan_id", nullable = false)
    private NguoiDung nguoiNhan;
}
