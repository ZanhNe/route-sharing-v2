package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "dong_xe", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dong_xe", columnNames = { "hang_xe_id", "ten_dong_xe" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class DongXe extends Base {
    @Column(name = "ten_dong_xe", nullable = false, length = 100)
    private String tenDongXe;
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_phuong_tien", nullable = false, length = 20)
    private LoaiPhuongTien loaiPhuongTien;
    @Column(name = "so_cho_hanh_khach_mac_dinh", nullable = false)
    private Integer soChoHanhKhachMacDinh;
    @Builder.Default
    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean dangHoatDong = true;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hang_xe_id", nullable = false)
    private HangXe hangXe;
}
