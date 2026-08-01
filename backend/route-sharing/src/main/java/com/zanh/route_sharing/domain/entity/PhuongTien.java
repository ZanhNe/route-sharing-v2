package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "phuong_tien", uniqueConstraints = {
                @UniqueConstraint(name = "uk_phuong_tien_bien_so", columnNames = "bien_so_xe")
}, indexes = {
                @Index(name = "idx_phuong_tien_nguoi_dang_ky", columnList = "nguoi_dang_ky_su_dung_id"),
                @Index(name = "idx_phuong_tien_trang_thai", columnList = "trang_thai_phuong_tien")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class PhuongTien extends Base {
        @Column(name = "bien_so_xe", nullable = false, length = 30)
        private String bienSoXe;
        @Column(name = "mau_sac_thuc_te", nullable = false, length = 100)
        private String mauSacThucTe;
        @Column(name = "so_cho_hanh_khach_duoc_duyet", nullable = false)
        private Integer soChoHanhKhachDuocDuyet;
        @Enumerated(EnumType.STRING)
        @Column(name = "co_so_su_dung", nullable = false, length = 40)
        private CoSoSuDungPhuongTien coSoSuDung;
        @Builder.Default
        @Column(name = "da_cam_ket_duoc_chu_xe_cho_phep", nullable = false)
        private Boolean daCamKetDuocChuXeChoPhep = false;
        @Column(name = "cam_ket_luc")
        private Instant camKetLuc;
        @Column(name = "tep_xac_nhan_chu_xe_url", length = 2048)
        private String tepXacNhanChuXeUrl;
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "trang_thai_phuong_tien", nullable = false, length = 40)
        private TrangThaiPhuongTien trangThaiPhuongTien = TrangThaiPhuongTien.DRAFT;
        @Column(name = "ngay_duoc_duyet")
        private Instant ngayDuocDuyet;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "nguoi_dang_ky_su_dung_id", nullable = false)
        private NguoiDung nguoiDangKySuDung;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "dong_xe_id", nullable = false)
        private DongXe dongXe;
}
