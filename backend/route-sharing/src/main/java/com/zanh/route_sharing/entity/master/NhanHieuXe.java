package com.zanh.route_sharing.entity.master;

import com.zanh.route_sharing.entity.Base;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "nhan_hieu_xe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NhanHieuXe extends Base {

    @Column(nullable = false, unique = true)
    private String tenNhanHieu; // Honda
}