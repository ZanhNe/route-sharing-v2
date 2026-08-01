package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiCongTac;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "ho_so_nhan_su")
@DiscriminatorValue("NHAN_SU")
@PrimaryKeyJoinColumn(name = "ho_so_thanh_vien_id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class HoSoNhanSu extends HoSoThanhVien {
    @Column(name = "ngay_bat_dau_cong_tac", nullable = false)
    private LocalDate ngayBatDauCongTac;
    @Column(name = "ngay_ket_thuc_cong_tac")
    private LocalDate ngayKetThucCongTac;
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_cong_tac", nullable = false, length = 30)
    private TrangThaiCongTac trangThaiCongTac;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_vi_truong_id")
    private DonViTruong donViTruong;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chuc_vu_id")
    private ChucVu chucVu;
}
