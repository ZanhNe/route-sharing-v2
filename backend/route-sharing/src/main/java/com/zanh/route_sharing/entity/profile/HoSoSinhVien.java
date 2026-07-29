package com.zanh.route_sharing.entity.profile;

import java.time.LocalDateTime;

import com.zanh.route_sharing.entity.master.Lop;
import com.zanh.route_sharing.enums.AllEnums.TrangThaiHocTap;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "ho_so_sinh_vien")
@DiscriminatorValue("SINH_VIEN")
@PrimaryKeyJoinColumn(name = "ho_so_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HoSoSinhVien extends HoSo {
    @Column(nullable = false)
    private LocalDateTime ngayNhapHoc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrangThaiHocTap trangThaiHocTap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lop_id", nullable = false)
    private Lop lop;
}
