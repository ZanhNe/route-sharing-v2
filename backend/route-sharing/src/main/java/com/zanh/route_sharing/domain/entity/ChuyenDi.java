package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chuyen_di", uniqueConstraints = {
                @UniqueConstraint(name = "uk_chuyen_di_lo_trinh", columnNames = "lo_trinh_chia_se_id")
}, indexes = {
                @Index(name = "idx_chuyen_di_trang_thai", columnList = "trang_thai_van_hanh,trang_thai_giam_sat")
}, check = @CheckConstraint(name = "ck_chuyen_di_so_khach", constraint = "so_khach_ke_hoach >= 0 "
                + "AND so_khach_thuc_te >= 0 "
                + "AND so_khach_thuc_te <= so_khach_ke_hoach"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ChuyenDi extends Base {
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "trang_thai_van_hanh", nullable = false, length = 40)
        private TrangThaiVanHanhChuyenDi trangThaiVanHanh = TrangThaiVanHanhChuyenDi.PREPARING;
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "trang_thai_giam_sat", nullable = false, length = 30)
        private TrangThaiGiamSatChuyenDi trangThaiGiamSat = TrangThaiGiamSatChuyenDi.NORMAL;
        @Column(name = "so_khach_ke_hoach", nullable = false)
        private Integer soKhachKeHoach;
        @Builder.Default
        @Column(name = "so_khach_thuc_te", nullable = false)
        private Integer soKhachThucTe = 0;
        @Column(name = "tuyen_duong_van_hanh", nullable = false, columnDefinition = "geometry(LineString,4326)")
        private LineString tuyenDuongVanHanh;
        @Column(name = "tuyen_duong_thuc_te_tong_hop", columnDefinition = "geometry(LineString,4326)")
        private LineString tuyenDuongThucTeTongHop;
        @Column(name = "bat_dau_luc")
        private Instant batDauLuc;
        @Column(name = "ket_thuc_luc")
        private Instant ketThucLuc;
        @Column(name = "vi_tri_cuoi_cung", columnDefinition = "geometry(Point,4326)")
        private Point viTriCuoiCung;
        @Column(name = "nhan_tin_hieu_cuoi_luc")
        private Instant nhanTinHieuCuoiLuc;
        @Column(name = "dong_bang_luc")
        private Instant dongBangLuc;
        @Column(name = "ly_do_dong_bang", length = 2000)
        private String lyDoDongBang;
        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "lo_trinh_chia_se_id", nullable = false, unique = true)
        private LoTrinhChiaSe loTrinhChiaSe;
        @Builder.Default
        @OneToMany(mappedBy = "chuyenDi", cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderBy("thuTu ASC")
        private List<DiemDungHanhTrinh> danhSachDiemDung = new ArrayList<>();
}
