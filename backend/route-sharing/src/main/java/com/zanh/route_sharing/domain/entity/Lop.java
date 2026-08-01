package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "lop", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lop_ma", columnNames = { "nganh_id", "ma_lop" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Lop extends Base {
    @Column(name = "ma_lop", nullable = false, length = 50)
    private String maLop;
    @Column(name = "khoa_tuyen", length = 50)
    private String khoaTuyen;
    @Column(name = "nam_bat_dau")
    private Integer namBatDau;
    @Column(name = "nam_ket_thuc_du_kien")
    private Integer namKetThucDuKien;
    @Builder.Default
    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean dangHoatDong = true;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nganh_id", nullable = false)
    private Nganh nganh;
}
