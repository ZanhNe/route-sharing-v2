package com.zanh.route_sharing.entity.ride;

import com.zanh.route_sharing.entity.Base;
import com.zanh.route_sharing.enums.AllEnums.*;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;

import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "diem_dung_hanh_trinh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DiemDungHanhTrinh extends Base {
    @Column(nullable = false)
    private Integer thuTu;

    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point toaDo;

    @Column(nullable = true)
    private String diaChiText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoaiDiemDung loaiDiemDung;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrangThaiDiemDung trangThai;

    private LocalDateTime thoiGianDenThucTe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chuyen_di_id", nullable = false)
    private ChuyenDi chuyenDi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yeu_cau_id") // Của booking nào? (Có thể Null nếu là điểm gốc của TX)
    private YeuCau yeuCau;
}