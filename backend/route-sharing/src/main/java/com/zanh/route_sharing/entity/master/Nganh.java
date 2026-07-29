package com.zanh.route_sharing.entity.master;

import com.zanh.route_sharing.entity.Base;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "nganh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Nganh extends Base {

    @Column(nullable = false)
    private String tenNganh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_vi_id", nullable = false)
    private DonViCongTac donViCongTac;
}