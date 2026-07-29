package com.zanh.route_sharing.entity;

import com.zanh.route_sharing.enums.AllEnums.*;
import com.zanh.route_sharing.entity.profile.HoSo;
import com.zanh.route_sharing.entity.security.*;
import com.zanh.route_sharing.entity.document.GiayTo;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "nguoi_dung")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NguoiDung extends Base {

    private String hoTen;

    @Column(nullable = false, unique = true)
    private String soDienThoai;

    @Column(nullable = false)
    private String matKhau;

    private String pin; // Băm bằng BCrypt

    @Column(length = 2048)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private GioiTinh gioiTinh;

    // AI Vector
    @Column(columnDefinition = "vector(128)")
    @JdbcTypeCode(SqlTypes.VECTOR) // Báo cho Hibernate biết đây là kiểu VECTOR của CSDL
    @Array(length = 128)
    private float[] faceVector;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TrangThaiTaiKhoan trangThai = TrangThaiTaiKhoan.INACTIVE;

    @Builder.Default
    private Double diemUyTin = 5.0;
    @Builder.Default
    private Integer soChuyenHoanThanh = 0;
    @Builder.Default
    private Integer soLanHuyChuyen = 0;

    private LocalDateTime lastLoginAt;

    // Quan hệ kết nối
    @OneToMany(mappedBy = "nguoiDung")
    private List<HoSo> danhSachHoSo;

    @OneToMany(mappedBy = "nguoiDung")
    private List<GiayTo> danhSachGiayTo;

    // 1 Người dùng có thể thuộc nhiều Nhóm quyền (VD: Vừa là Admin trường, Vừa là
    // Đội an ninh)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "nguoi_dung_nhom_quyen", joinColumns = @JoinColumn(name = "nguoi_dung_id"), inverseJoinColumns = @JoinColumn(name = "nhom_quyen_id"))
    private Set<NhomQuyen> danhSachNhomQuyen;

    // Cấp quyền trực tiếp cho User (Ngoại lệ - Không cần thông qua Nhóm)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "nguoi_dung_quyen_rieng", joinColumns = @JoinColumn(name = "nguoi_dung_id"), inverseJoinColumns = @JoinColumn(name = "quyen_han_id"))
    private Set<QuyenHan> danhSachQuyenRieng;
}