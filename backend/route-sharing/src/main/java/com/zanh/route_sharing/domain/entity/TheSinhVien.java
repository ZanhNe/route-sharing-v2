package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "the_sinh_vien")
@DiscriminatorValue("THE_SINH_VIEN")
@PrimaryKeyJoinColumn(name = "giay_to_id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class TheSinhVien extends GiayTo {
    @Column(name = "ma_so_sinh_vien_tren_the", nullable = false, length = 100)
    private String maSoSinhVienTrenThe;
    @Column(name = "ho_ten_tren_the", nullable = false, length = 255)
    private String hoTenTrenThe;
    @Column(name = "ten_truong_tren_the", nullable = false, length = 255)
    private String tenTruongTrenThe;
    @Column(name = "khoa_hoc", length = 100)
    private String khoaHoc;
}
