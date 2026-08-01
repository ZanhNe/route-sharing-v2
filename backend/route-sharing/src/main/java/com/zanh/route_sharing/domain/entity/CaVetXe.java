package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "ca_vet_xe", indexes = {
        @Index(name = "idx_ca_vet_xe_phuong_tien", columnList = "phuong_tien_id")
})
@DiscriminatorValue("CA_VET_XE")
@PrimaryKeyJoinColumn(name = "giay_to_id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class CaVetXe extends GiayTo {
    @Column(name = "chu_xe", nullable = false, length = 255)
    private String chuXe;
    @Column(name = "bien_so_tren_giay", nullable = false, length = 30)
    private String bienSoTrenGiay;
    @Column(name = "nhan_hieu_tren_giay", nullable = false, length = 100)
    private String nhanHieuTrenGiay;
    @Column(name = "so_loai_tren_giay", nullable = false, length = 100)
    private String soLoaiTrenGiay;
    @Column(name = "dung_tich")
    private Integer dungTich;
    @Column(name = "so_khung", nullable = false, length = 100)
    private String soKhung;
    @Column(name = "so_may", nullable = false, length = 100)
    private String soMay;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phuong_tien_id", nullable = false)
    private PhuongTien phuongTien;
}
