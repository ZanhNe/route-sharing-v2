package com.zanh.route_sharing.entity.ride;

import com.zanh.route_sharing.entity.Base;
import com.zanh.route_sharing.enums.AllEnums.TrangThaiChuyenDi;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.LineString;
import java.time.LocalDateTime;
import java.util.List;

import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "chuyen_di")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ChuyenDi extends Base {
    @Column(nullable = false)
    private LocalDateTime thoiGianBatDau;

    @Column(nullable = false)
    private Integer soKhachThucTe;

    @Column(columnDefinition = "geometry(LineString, 4326)")
    private LineString nhatKyToaDoThucTe;

    @Column(columnDefinition = "geometry(LineString, 4326)")
    private LineString nhatKyDuongDuKien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrangThaiChuyenDi trangThai;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lo_trinh_id", nullable = false)
    private LoTrinhChiaSe loTrinhChiaSe;

    @OneToMany(mappedBy = "chuyenDi", cascade = CascadeType.ALL)
    private List<DiemDungHanhTrinh> danhSachDiemDung;
}