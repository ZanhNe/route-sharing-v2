package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiBienLai;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bien_lai", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bien_lai_ma", columnNames = "ma_bien_lai"),
        @UniqueConstraint(name = "uk_bien_lai_yeu_cau", columnNames = "yeu_cau_di_chung_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class BienLai extends Base {
    @Column(name = "ma_bien_lai", nullable = false, length = 100)
    private String maBienLai;
    @Column(name = "so_tien_thoa_thuan", nullable = false, precision = 15, scale = 2)
    private BigDecimal soTienThoaThuan;
    @Builder.Default
    @Column(name = "don_vi_tien_te", nullable = false, length = 10)
    private String donViTienTe = "VND";
    @Column(name = "khoang_cach_duoc_phuc_vu_met", nullable = false, precision = 14, scale = 2)
    private BigDecimal khoangCachDuocPhucVuMet;
    @Column(name = "ten_tai_xe_snapshot", nullable = false, length = 255)
    private String tenTaiXeSnapshot;
    @Column(name = "ten_hanh_khach_snapshot", nullable = false, length = 255)
    private String tenHanhKhachSnapshot;
    @Column(name = "bien_so_xe_snapshot", nullable = false, length = 30)
    private String bienSoXeSnapshot;
    @Column(name = "sinh_luc", nullable = false)
    private Instant sinhLuc;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_bien_lai", nullable = false, length = 30)
    private TrangThaiBienLai trangThaiBienLai = TrangThaiBienLai.ISSUED;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yeu_cau_di_chung_id", nullable = false, unique = true)
    private YeuCauDiChung yeuCauDiChung;
}
