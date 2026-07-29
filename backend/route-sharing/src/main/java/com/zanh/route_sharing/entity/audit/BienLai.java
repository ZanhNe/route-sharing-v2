package com.zanh.route_sharing.entity.audit;

import com.zanh.route_sharing.entity.ride.*;
import com.zanh.route_sharing.entity.Base;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "bien_lai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BienLai extends Base {
    @Column(nullable = false)
    private Double soTien;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yeu_cau_id", nullable = false, unique = true)
    private YeuCau yeuCau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chuyen_di_id", nullable = false)
    private ChuyenDi chuyenDi;
}
