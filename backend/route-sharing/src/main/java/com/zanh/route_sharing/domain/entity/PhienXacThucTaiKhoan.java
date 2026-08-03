package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "phien_xac_thuc_tai_khoan", indexes = {
        @Index(name = "idx_phien_xac_thuc_nguoi_dung", columnList = "nguoi_dung_id"),
        @Index(name = "idx_phien_xac_thuc_trang_thai_het_han", columnList = "trang_thai,het_han_luc")
}, check = {
        @CheckConstraint(name = "ck_phien_xac_thuc_lan_thu", constraint = "so_lan_thu >= 0 "
                + "AND so_lan_thu_toi_da > 0 "
                + "AND so_lan_thu <= so_lan_thu_toi_da"),
        @CheckConstraint(name = "ck_phien_xac_thuc_han", constraint = "het_han_luc > created_at")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class PhienXacThucTaiKhoan extends Base {
    @Enumerated(EnumType.STRING)
    @Column(name = "muc_dich", nullable = false, length = 30)
    private MucDichXacThucTaiKhoan mucDich;
    @Column(name = "email_nhan", nullable = false, length = 255)
    private String emailNhan;
    @Column(name = "ma_otp_da_bam", nullable = false, length = 255)
    private String maOtpDaBam;
    @Column(name = "gui_luc")
    private Instant guiLuc;
    @Column(name = "het_han_luc", nullable = false)
    private Instant hetHanLuc;
    @Builder.Default
    @Column(name = "so_lan_thu", nullable = false)
    private Integer soLanThu = 0;
    @Builder.Default
    @Column(name = "so_lan_thu_toi_da", nullable = false)
    private Integer soLanThuToiDa = 5;
    @Column(name = "hoan_thanh_luc")
    private Instant hoanThanhLuc;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 30)
    private TrangThaiPhienXacThucTaiKhoan trangThai = TrangThaiPhienXacThucTaiKhoan.CREATED;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;
}
