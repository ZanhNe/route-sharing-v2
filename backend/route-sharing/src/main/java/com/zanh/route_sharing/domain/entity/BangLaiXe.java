package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "bang_lai_xe")
@DiscriminatorValue("BANG_LAI_XE")
@PrimaryKeyJoinColumn(name = "giay_to_id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class BangLaiXe extends GiayTo {
    @Column(name = "ho_ten", nullable = false, length = 255)
    private String hoTen;
    @Column(name = "ngay_sinh", nullable = false)
    private LocalDate ngaySinh;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hang_giay_phep_id", nullable = false)
    private HangGiayPhep hangGiayPhep;
}
