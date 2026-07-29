package com.zanh.route_sharing.entity.document;

import com.zanh.route_sharing.entity.master.LoaiXe;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import com.zanh.route_sharing.entity.Base;

@Entity
@Table(name = "phuong_tien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PhuongTien extends Base {

    @Column(nullable = false, unique = true)
    private String bienSoXe;
    @Column(nullable = false)
    private String mauSacThucTe;

    @Builder.Default
    private Boolean isHoatDong = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loai_xe_id", nullable = false)
    private LoaiXe loaiXe;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ca_vet_id", nullable = false)
    private CaVet caVet;
}