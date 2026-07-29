package com.zanh.route_sharing.entity.master;

import com.zanh.route_sharing.entity.Base;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "hang_giay_phep")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HangGiayPhep extends Base {

    @Column(nullable = false, unique = true)
    private String tenHang; // A1, B2

    private Integer phanKhoiToiDa;
    private Boolean khongThoiHan; // VD: A1 = true
}