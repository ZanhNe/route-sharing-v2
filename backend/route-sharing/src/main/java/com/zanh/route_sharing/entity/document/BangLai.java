package com.zanh.route_sharing.entity.document;

import java.time.*;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import com.zanh.route_sharing.entity.master.HangGiayPhep;

@Entity
@Table(name = "giay_to_bang_lai")
@DiscriminatorValue("GPLX")
@PrimaryKeyJoinColumn(name = "giay_to_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BangLai extends GiayTo {
    @Column(nullable = false)
    private String hoTen;
    @Column(nullable = false)
    private LocalDate ngaySinh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hang_giay_phep_id", nullable = false)
    private HangGiayPhep hangGiayPhep;
}