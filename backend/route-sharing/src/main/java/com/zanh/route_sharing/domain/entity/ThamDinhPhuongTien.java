package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tham_dinh_phuong_tien")
@DiscriminatorValue("PHUONG_TIEN")
@PrimaryKeyJoinColumn(name = "lan_tham_dinh_id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ThamDinhPhuongTien extends LanThamDinh {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phuong_tien_id", nullable = false)
    private PhuongTien phuongTien;
}
