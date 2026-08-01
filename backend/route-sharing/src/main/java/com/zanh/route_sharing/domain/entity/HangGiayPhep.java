package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "hang_giay_phep", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hang_giay_phep_ma", columnNames = "ma_hang")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class HangGiayPhep extends Base {
    @Column(name = "ma_hang", nullable = false, length = 30)
    private String maHang;
    @Column(name = "ten_hang", nullable = false, length = 100)
    private String tenHang;
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_phuong_tien", nullable = false, length = 20)
    private LoaiPhuongTien loaiPhuongTien;
    @Column(name = "phan_khoi_toi_da")
    private Integer phanKhoiToiDa;
    @Builder.Default
    @Column(name = "khong_thoi_han", nullable = false)
    private Boolean khongThoiHan = false;
    @Builder.Default
    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean dangHoatDong = true;
}
