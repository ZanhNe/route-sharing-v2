package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "hang_xe", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hang_xe_ma", columnNames = "ma_hang")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class HangXe extends Base {
    @Column(name = "ma_hang", nullable = false, length = 50)
    private String maHang;
    @Column(name = "ten_hang", nullable = false, length = 100)
    private String tenHang;
    @Builder.Default
    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean dangHoatDong = true;
}
