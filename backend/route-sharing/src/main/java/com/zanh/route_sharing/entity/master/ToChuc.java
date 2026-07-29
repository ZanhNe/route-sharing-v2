package com.zanh.route_sharing.entity.master;

import com.zanh.route_sharing.entity.Base;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import com.zanh.route_sharing.enums.AllEnums.LoaiToChuc;

@Entity
@Table(name = "to_chuc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ToChuc extends Base {

    @Column(nullable = false, unique = true)
    private String tenToChuc;

    @Column(nullable = false)
    private String domainEmail; // VD: ou.edu.vn

    @Enumerated(EnumType.STRING)
    private LoaiToChuc loaiToChuc;
}