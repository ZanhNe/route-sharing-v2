package com.zanh.route_sharing.entity.audit;

import com.zanh.route_sharing.entity.Base;
import com.zanh.route_sharing.entity.ride.*;
import com.zanh.route_sharing.enums.AllEnums.*;
import com.zanh.route_sharing.entity.NguoiDung;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "bao_cao_an_ninh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaoCaoAnNinh extends Base {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoaiSuCo loaiSuCo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MucDoSuCo mucDo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String lyDo; // Mô tả chi tiết

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_bao_cao_id") // Bỏ nullable = false
    private NguoiDung nguoiBaoCao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_bi_bao_cao_id", nullable = false)
    private NguoiDung nguoiBiBaoCao;

    // N-1 với Chuyến đi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chuyen_di_id", nullable = false)
    private ChuyenDi chuyenDi;
}