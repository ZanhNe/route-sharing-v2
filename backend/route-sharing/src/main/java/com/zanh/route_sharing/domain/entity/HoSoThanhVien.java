package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiHoSoThanhVien;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ho_so_thanh_vien", indexes = {
        @Index(name = "idx_ho_so_thanh_vien_nguoi_dung", columnList = "nguoi_dung_id"),
        @Index(name = "idx_ho_so_thanh_vien_truong", columnList = "nha_truong_id"),
        @Index(name = "idx_ho_so_thanh_vien_trang_thai", columnList = "trang_thai_ho_so")
})
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "loai_ho_so", discriminatorType = DiscriminatorType.STRING, length = 30)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public abstract class HoSoThanhVien extends Base {
    @Column(name = "ma_dinh_danh_noi_bo", nullable = false, length = 100)
    private String maDinhDanhNoiBo;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_ho_so", nullable = false, length = 40)
    private TrangThaiHoSoThanhVien trangThaiHoSo = TrangThaiHoSoThanhVien.DRAFT;
    @Column(name = "ngay_bat_dau_hieu_luc")
    private LocalDate ngayBatDauHieuLuc;
    @Column(name = "ngay_ket_thuc_hieu_luc")
    private LocalDate ngayKetThucHieuLuc;
    @Column(name = "ngay_duoc_duyet")
    private Instant ngayDuocDuyet;
    @Column(name = "ly_do_tu_choi", length = 2000)
    private String lyDoTuChoi;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nha_truong_id", nullable = false)
    private NhaTruong nhaTruong;
}
