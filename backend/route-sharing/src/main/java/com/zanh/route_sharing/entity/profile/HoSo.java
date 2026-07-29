package com.zanh.route_sharing.entity.profile;

import com.zanh.route_sharing.entity.Base;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.zanh.route_sharing.entity.NguoiDung;
import com.zanh.route_sharing.entity.master.DonViCongTac;
import com.zanh.route_sharing.entity.master.ToChuc;
import com.zanh.route_sharing.enums.AllEnums.TrangThaiDuyet;

@Entity
@Table(name = "ho_so")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "loai_ho_so")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class HoSo extends Base {

    @Column(nullable = false)
    private String maSoDinhDanh;
    @Column(nullable = false, unique = true)
    private String eduEmail;
    @Column(nullable = false)
    private String anhTheUrl;

    private Boolean isHoatDong = true; // Giải quyết lưu lịch sử chuyển công tác

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrangThaiDuyet trangThaiDuyet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

    // SaaS Multi-tenant Links
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_chuc_id", nullable = false)
    private ToChuc toChuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_vi_id")
    private DonViCongTac donViCongTac;
}