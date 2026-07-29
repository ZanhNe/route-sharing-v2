package com.zanh.route_sharing.entity.master;

import com.zanh.route_sharing.entity.Base;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "chuc_vu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ChucVu extends Base {

    @Column(nullable = false)
    private String tenChucVu;
}