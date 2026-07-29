package com.zanh.route_sharing.entity.master;

import com.zanh.route_sharing.entity.Base;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "loai_xe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LoaiXe extends Base {

    @Column(nullable = false)
    private String tenLoai; // Vision

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_hieu_id", nullable = false)
    private NhanHieuXe nhanHieu;
}