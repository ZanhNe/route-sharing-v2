package com.zanh.route_sharing.entity.security;

import com.zanh.route_sharing.entity.Base;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "quyen_han")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class QuyenHan extends Base {

    @Column(nullable = false, unique = true)
    private String maQuyen;

    private String moTa;
}
