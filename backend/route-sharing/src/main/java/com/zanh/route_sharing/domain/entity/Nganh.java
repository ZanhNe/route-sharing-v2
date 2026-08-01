package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "nganh", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nganh_ma", columnNames = { "don_vi_truong_id", "ma_nganh" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Nganh extends Base {
    @Column(name = "ma_nganh", nullable = false, length = 50)
    private String maNganh;
    @Column(name = "ten_nganh", nullable = false, length = 255)
    private String tenNganh;
    @Builder.Default
    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean dangHoatDong = true;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "don_vi_truong_id", nullable = false)
    private DonViTruong donViTruong;
}
