package com.zanh.route_sharing.entity.security;

import com.zanh.route_sharing.entity.Base;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.Set;

@Entity
@Table(name = "nhom_quyen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NhomQuyen extends Base {
    @Column(nullable = false, unique = true)
    private String tenNhom;

    private String moTa;

    // 1 Nhóm quyền có NHIỀU Quyền hạn
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "nhom_quyen_chi_tiet", joinColumns = @JoinColumn(name = "nhom_quyen_id"), inverseJoinColumns = @JoinColumn(name = "quyen_han_id"))
    private Set<QuyenHan> danhSachQuyen;
}