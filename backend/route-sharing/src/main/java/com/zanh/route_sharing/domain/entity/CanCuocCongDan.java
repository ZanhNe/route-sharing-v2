package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.GioiTinh;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "can_cuoc_cong_dan")
@DiscriminatorValue("CAN_CUOC_CONG_DAN")
@PrimaryKeyJoinColumn(name = "giay_to_id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class CanCuocCongDan extends GiayTo {
    @Column(name = "ho_ten", nullable = false, length = 255)
    private String hoTen;
    @Column(name = "ngay_sinh", nullable = false)
    private LocalDate ngaySinh;
    @Enumerated(EnumType.STRING)
    @Column(name = "gioi_tinh", nullable = false, length = 20)
    private GioiTinh gioiTinh;
    @Column(name = "que_quan", nullable = false, length = 500)
    private String queQuan;
    @Column(name = "thuong_tru", nullable = false, length = 500)
    private String thuongTru;
}
