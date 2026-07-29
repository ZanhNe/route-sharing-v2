package com.zanh.route_sharing.entity.document;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "giay_to_ca_vet")
@DiscriminatorValue("CAVET")
@PrimaryKeyJoinColumn(name = "giay_to_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CaVet extends GiayTo {
    @Column(nullable = false)
    private String chuXe;
    @Column(nullable = false)
    private String soLoaiInTrenGiay;
    @Column(nullable = false)
    private Integer dungTich;
    @Column(nullable = false)
    private String soKhung;
    @Column(nullable = false)
    private String soMay;

    @OneToOne(mappedBy = "caVet", fetch = FetchType.LAZY)
    private PhuongTien phuongTien;
}