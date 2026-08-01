package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiDonViTruong;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "don_vi_truong", uniqueConstraints = {
                @UniqueConstraint(name = "uk_don_vi_truong_ma", columnNames = { "nha_truong_id", "ma_don_vi" })
}, indexes = {
                @Index(name = "idx_don_vi_truong_cha", columnList = "don_vi_cha_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class DonViTruong extends Base {
        @Column(name = "ma_don_vi", nullable = false, length = 50)
        private String maDonVi;
        @Column(name = "ten_don_vi", nullable = false, length = 255)
        private String tenDonVi;
        @Enumerated(EnumType.STRING)
        @Column(name = "loai_don_vi", nullable = false, length = 30)
        private LoaiDonViTruong loaiDonVi;
        @Builder.Default
        @Column(name = "dang_hoat_dong", nullable = false)
        private Boolean dangHoatDong = true;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "nha_truong_id", nullable = false)
        private NhaTruong nhaTruong;
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "don_vi_cha_id")
        private DonViTruong donViCha;
}
