package com.zanh.route_sharing.entity.profile;

import com.zanh.route_sharing.entity.master.ChucVu;
import com.zanh.route_sharing.enums.AllEnums.LoaiHopDong;
import com.zanh.route_sharing.enums.AllEnums.TrinhDoHocVan;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "ho_so_nhan_su")
@DiscriminatorValue("NHAN_SU")
@PrimaryKeyJoinColumn(name = "ho_so_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HoSoNhanSu extends HoSo {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chuc_vu_id", nullable = false)
    private ChucVu chucVu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrinhDoHocVan trinhDoHocVan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoaiHopDong loaiHopDong;
}