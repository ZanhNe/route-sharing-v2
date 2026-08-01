package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.GioiTinh;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "nguoi_dung", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nguoi_dung_email_truong", columnNames = "email_truong")
}, indexes = {
        @Index(name = "idx_nguoi_dung_trang_thai", columnList = "trang_thai_tai_khoan")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class NguoiDung extends Base {
    @Column(name = "ho_ten", nullable = false, length = 255)
    private String hoTen;
    @Column(name = "email_truong", nullable = false, length = 255)
    private String emailTruong;
    @Column(name = "so_dien_thoai", length = 30)
    private String soDienThoai;
    @Column(name = "mat_khau_da_ma_hoa", nullable = false, length = 255)
    private String matKhauDaMaHoa;
    @Column(name = "anh_dai_dien_url", length = 2048)
    private String anhDaiDienUrl;
    @Column(name = "anh_chan_dung_xac_minh_url", length = 2048)
    private String anhChanDungXacMinhUrl;
    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;
    @Enumerated(EnumType.STRING)
    @Column(name = "gioi_tinh", length = 20)
    private GioiTinh gioiTinh;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_tai_khoan", nullable = false, length = 40)
    private TrangThaiTaiKhoan trangThaiTaiKhoan = TrangThaiTaiKhoan.CHO_XAC_THUC_EMAIL;
    @Column(name = "email_da_xac_thuc_luc")
    private Instant emailDaXacThucLuc;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Version bảo mật độc lập với optimistic-lock version.
     * Mọi thay đổi trạng thái tài khoản, mật khẩu hoặc RBAC phải làm tăng giá trị
     * này.
     */
    @Setter(AccessLevel.NONE)
    @ColumnDefault("0")
    @Column(name = "security_version", nullable = false, insertable = false, updatable = false)
    private Long securityVersion;

    @PrePersist
    @PreUpdate
    private void normalizeIdentityFields() {
        if (emailTruong != null) {
            emailTruong = emailTruong.trim().toLowerCase(Locale.ROOT);
        }
        if (soDienThoai != null) {
            soDienThoai = soDienThoai.trim();
            if (soDienThoai.isEmpty()) {
                soDienThoai = null;
            }
        }
    }

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "nguoi_dung_nhom_quyen", joinColumns = @JoinColumn(name = "nguoi_dung_id"), inverseJoinColumns = @JoinColumn(name = "nhom_quyen_id"), uniqueConstraints = @UniqueConstraint(name = "uk_nguoi_dung_nhom_quyen", columnNames = {
            "nguoi_dung_id", "nhom_quyen_id" }))
    private Set<NhomQuyen> danhSachNhomQuyen = new LinkedHashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "nguoi_dung_quyen_truc_tiep", joinColumns = @JoinColumn(name = "nguoi_dung_id"), inverseJoinColumns = @JoinColumn(name = "quyen_han_id"), uniqueConstraints = @UniqueConstraint(name = "uk_nguoi_dung_quyen_truc_tiep", columnNames = {
            "nguoi_dung_id", "quyen_han_id" }))
    private Set<QuyenHan> danhSachQuyenTrucTiep = new LinkedHashSet<>();
}
