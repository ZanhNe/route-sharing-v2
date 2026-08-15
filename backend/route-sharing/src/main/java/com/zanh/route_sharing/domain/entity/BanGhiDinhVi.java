package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.NguonViTri;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ban_ghi_dinh_vi", uniqueConstraints = {
                @UniqueConstraint(name = "uk_ban_ghi_dinh_vi_thu_tu", columnNames = { "chuyen_di_id",
                                "thu_tu_ban_ghi" })
}, indexes = {
                @Index(name = "idx_ban_ghi_dinh_vi_chuyen_thoi_gian", columnList = "chuyen_di_id,thoi_gian_server_nhan"),
                @Index(name = "idx_ban_ghi_dinh_vi_chuyen_trinh_duyet", columnList = "chuyen_di_id,thoi_gian_trinh_duyet")
}, check = {
                @CheckConstraint(name = "ck_ban_ghi_thu_tu", constraint = "thu_tu_ban_ghi >= 0"),
                @CheckConstraint(name = "ck_ban_ghi_do_chinh_xac", constraint = "do_chinh_xac_met IS NULL OR do_chinh_xac_met >= 0"),
                @CheckConstraint(name = "ck_ban_ghi_toc_do", constraint = "toc_do_met_moi_giay IS NULL OR toc_do_met_moi_giay >= 0"),
                @CheckConstraint(name = "ck_ban_ghi_huong", constraint = "huong_di_chuyen IS NULL OR huong_di_chuyen BETWEEN 0 AND 360")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class BanGhiDinhVi extends Base {
        @Column(name = "toa_do", nullable = false, columnDefinition = "geometry(Point,4326)")
        private Point toaDo;
        @Column(name = "thoi_gian_trinh_duyet", nullable = false)
        private Instant thoiGianTrinhDuyet;
        @Column(name = "thoi_gian_server_nhan", nullable = false)
        private Instant thoiGianServerNhan;
        @Column(name = "do_chinh_xac_met", precision = 12, scale = 2)
        private BigDecimal doChinhXacMet;
        @Column(name = "toc_do_met_moi_giay", precision = 12, scale = 3)
        private BigDecimal tocDoMetMoiGiay;
        @Column(name = "huong_di_chuyen", precision = 6, scale = 2)
        private BigDecimal huongDiChuyen;
        @Column(name = "thu_tu_ban_ghi", nullable = false)
        private Long thuTuBanGhi;
        @Enumerated(EnumType.STRING)
        @Column(name = "nguon_vi_tri", nullable = false, length = 30)
        private NguonViTri nguonViTri;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "chuyen_di_id", nullable = false)
        private ChuyenDi chuyenDi;
}
