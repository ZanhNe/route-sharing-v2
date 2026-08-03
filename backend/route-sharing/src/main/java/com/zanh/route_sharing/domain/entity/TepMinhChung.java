package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiTepMinhChung;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "tep_minh_chung", indexes = {
        @Index(name = "idx_tep_minh_chung_khieu_nai", columnList = "khieu_nai_id"),
        @Index(name = "idx_tep_minh_chung_su_co", columnList = "su_co_chuyen_di_id")
}, check = @CheckConstraint(name = "ck_tep_minh_chung_xor", constraint = "(khieu_nai_id IS NOT NULL AND su_co_chuyen_di_id IS NULL) "
        + "OR (khieu_nai_id IS NULL AND su_co_chuyen_di_id IS NOT NULL)"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class TepMinhChung extends Base {
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_tep", nullable = false, length = 20)
    private LoaiTepMinhChung loaiTep;
    @Column(name = "file_url", nullable = false, length = 2048)
    private String fileUrl;
    @Column(name = "file_hash", length = 255)
    private String fileHash;
    @Column(name = "mo_ta", length = 2000)
    private String moTa;
    @Column(name = "tai_len_luc", nullable = false)
    private Instant taiLenLuc;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_tai_len_id", nullable = false)
    private NguoiDung nguoiTaiLen;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khieu_nai_id")
    private KhieuNai khieuNai;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "su_co_chuyen_di_id")
    private SuCoChuyenDi suCoChuyenDi;
}
