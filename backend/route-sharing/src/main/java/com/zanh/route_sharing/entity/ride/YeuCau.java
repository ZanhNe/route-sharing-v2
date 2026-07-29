package com.zanh.route_sharing.entity.ride;

import com.zanh.route_sharing.entity.NguoiDung;
import com.zanh.route_sharing.enums.AllEnums.TrangThaiYeuCau;
import com.zanh.route_sharing.entity.Base;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "yeu_cau")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class YeuCau extends Base {

    @Column(nullable = false)
    private Double mucHoTro; // Tiền
    @Column(nullable = false)
    private Double khoangCachDiChuyen;

    // Hỗ trợ PostGIS điểm Đón/Trả
    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point toaDoDon;
    @Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point toaDoTha;

    @Column(nullable = true, columnDefinition = "geometry(Point, 4326)")
    private Point toaDoDonAo;
    @Column(nullable = true, columnDefinition = "geometry(Point, 4326)")
    private Point toaDoThaAo;

    @Column(nullable = true)
    private String otpChuyenDi; // OTP để lên xe

    @Column(nullable = true)
    private String ghiChu; // Ghi chú của hành khách

    @Builder.Default
    private Boolean chiDiCungToChuc = true; // Thuật toán tùy chọn Matching

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrangThaiYeuCau trangThai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hanh_khach_id", nullable = false)
    private NguoiDung hanhKhach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lo_trinh_id") // Null khi đang tìm xe
    private LoTrinhChiaSe loTrinhChiaSe;

    @OneToMany(mappedBy = "yeuCau", fetch = FetchType.LAZY)
    private List<DiemDungHanhTrinh> danhSachDiemDungThucTe;

}