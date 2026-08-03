package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "danh_gia", uniqueConstraints = {
                @UniqueConstraint(name = "uk_danh_gia_nguoi_yeu_cau", columnNames = { "yeu_cau_di_chung_id",
                                "nguoi_danh_gia_id" })
}, indexes = {
                @Index(name = "idx_danh_gia_nguoi_duoc_danh_gia", columnList = "nguoi_duoc_danh_gia_id,danh_gia_luc")
}, check = {
                @CheckConstraint(name = "ck_danh_gia_so_sao", constraint = "so_sao BETWEEN 1 AND 5"),
                @CheckConstraint(name = "ck_danh_gia_hai_nguoi", constraint = "nguoi_danh_gia_id <> nguoi_duoc_danh_gia_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class DanhGia extends Base {
        @Column(name = "so_sao", nullable = false)
        private Integer soSao;
        @Column(name = "nhan_xet", length = 3000)
        private String nhanXet;
        @Column(name = "danh_gia_luc", nullable = false)
        private Instant danhGiaLuc;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "yeu_cau_di_chung_id", nullable = false)
        private YeuCauDiChung yeuCauDiChung;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "nguoi_danh_gia_id", nullable = false)
        private NguoiDung nguoiDanhGia;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "nguoi_duoc_danh_gia_id", nullable = false)
        private NguoiDung nguoiDuocDanhGia;
}
