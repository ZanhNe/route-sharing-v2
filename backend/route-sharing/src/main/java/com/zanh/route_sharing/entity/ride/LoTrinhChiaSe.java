package com.zanh.route_sharing.entity.ride;

import com.zanh.route_sharing.entity.Base;
import com.zanh.route_sharing.entity.NguoiDung;
import com.zanh.route_sharing.entity.document.PhuongTien;
import com.zanh.route_sharing.enums.AllEnums.TrangThaiChiaSe;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

import org.locationtech.jts.geom.LineString;

@Entity
@Table(name = "lo_trinh_chia_se")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LoTrinhChiaSe extends Base {
    @Column(nullable = false)
    private Integer soGheTrong;
    @Column(nullable = false)
    private Double giaTienMoiKm;
    @Column(nullable = false)
    private LocalDateTime thoiGianKhoiHanhDuKien;

    // Hỗ trợ PostGIS vẽ Line (Tuyến đường)
    @Column(nullable = false, columnDefinition = "geometry(LineString, 4326)")
    private LineString tuyenDuongGoc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrangThaiChiaSe trangThai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tai_xe_id", nullable = false)
    private NguoiDung taiXe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phuong_tien_id", nullable = false)
    private PhuongTien phuongTien;

    @OneToMany(mappedBy = "loTrinhChiaSe", fetch = FetchType.LAZY)
    private List<YeuCau> danhSachYeuCau;

}