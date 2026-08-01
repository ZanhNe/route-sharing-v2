package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiXe;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "ho_so_tai_xe", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ho_so_tai_xe_nguoi_dung", columnNames = "nguoi_dung_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class HoSoTaiXe extends Base {
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_tai_xe", nullable = false, length = 40)
    private TrangThaiTaiXe trangThaiTaiXe = TrangThaiTaiXe.DRAFT;
    @Column(name = "ngay_dang_ky", nullable = false)
    private Instant ngayDangKy;
    @Column(name = "ngay_duoc_duyet")
    private Instant ngayDuocDuyet;
    @Column(name = "ngay_bi_tam_dung")
    private Instant ngayBiTamDung;
    @Column(name = "ly_do_tam_dung", length = 2000)
    private String lyDoTamDung;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_dung_id", nullable = false, unique = true)
    private NguoiDung nguoiDung;
}
