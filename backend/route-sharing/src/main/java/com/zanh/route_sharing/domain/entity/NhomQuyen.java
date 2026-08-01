package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "nhom_quyen", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nhom_quyen_ma", columnNames = "ma_nhom")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class NhomQuyen extends Base {
    @Column(name = "ma_nhom", nullable = false, length = 100)
    private String maNhom;
    @Column(name = "ten_nhom", nullable = false, length = 255)
    private String tenNhom;
    @Column(name = "mo_ta", length = 1000)
    private String moTa;
    @Builder.Default
    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean dangHoatDong = true;
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "nhom_quyen_quyen_han", joinColumns = @JoinColumn(name = "nhom_quyen_id"), inverseJoinColumns = @JoinColumn(name = "quyen_han_id"), uniqueConstraints = @UniqueConstraint(name = "uk_nhom_quyen_quyen_han", columnNames = {
            "nhom_quyen_id", "quyen_han_id" }))
    private Set<QuyenHan> danhSachQuyenHan = new LinkedHashSet<>();

    @PrePersist
    @PreUpdate
    private void normalizeCode() {
        if (maNhom != null) {
            maNhom = maNhom.trim().toUpperCase(Locale.ROOT);
        }
    }
}
