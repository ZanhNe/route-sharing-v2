package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "chuc_vu", uniqueConstraints = {
        @UniqueConstraint(name = "uk_chuc_vu_ma", columnNames = "ma_chuc_vu")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ChucVu extends Base {
    @Column(name = "ma_chuc_vu", nullable = false, length = 50)
    private String maChucVu;
    @Column(name = "ten_chuc_vu", nullable = false, length = 255)
    private String tenChucVu;
    @Column(name = "mo_ta", length = 1000)
    private String moTa;
    @Builder.Default
    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean dangHoatDong = true;
}
