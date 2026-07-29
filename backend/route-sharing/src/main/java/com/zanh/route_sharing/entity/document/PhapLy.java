package com.zanh.route_sharing.entity.document;

import com.zanh.route_sharing.enums.AllEnums.GioiTinh;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "giay_to_phap_ly")
@DiscriminatorValue("CCCD")
@PrimaryKeyJoinColumn(name = "giay_to_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PhapLy extends GiayTo {
    @Column(nullable = false)
    private String hoTen;
    @Column(nullable = false)
    private LocalDate ngaySinh;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GioiTinh gioiTinh;
    @Column(nullable = false)
    private String queQuan;
    @Column(nullable = false)
    private String thuongTru;
    @Column(nullable = false)
    private String anhChupCungCccdUrl;
}