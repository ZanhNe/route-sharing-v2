package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiHocTap;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "ho_so_sinh_vien")
@DiscriminatorValue("SINH_VIEN")
@PrimaryKeyJoinColumn(name = "ho_so_thanh_vien_id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class HoSoSinhVien extends HoSoThanhVien {
    @Column(name = "ma_so_sinh_vien", length = 100)
    private String maSoSinhVien;
    @Column(name = "ngay_nhap_hoc")
    private LocalDate ngayNhapHoc;
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_hoc_tap", length = 30)
    private TrangThaiHocTap trangThaiHocTap;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lop_id")
    private Lop lop;
}
