package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Locale;

@Entity
@Table(name = "quyen_han", uniqueConstraints = {
        @UniqueConstraint(name = "uk_quyen_han_ma", columnNames = "ma_quyen")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class QuyenHan extends Base {
    @Column(name = "ma_quyen", nullable = false, length = 150)
    private String maQuyen;
    @Column(name = "ten_quyen", nullable = false, length = 255)
    private String tenQuyen;
    @Column(name = "mo_ta", length = 1000)
    private String moTa;
    @Column(name = "nhom_chuc_nang", length = 100)
    private String nhomChucNang;
    @Builder.Default
    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean dangHoatDong = true;

    @PrePersist
    @PreUpdate
    private void normalizeCode() {
        if (maQuyen != null) {
            maQuyen = maQuyen.trim().toUpperCase(Locale.ROOT);
        }
    }
}
