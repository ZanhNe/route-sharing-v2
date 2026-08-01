package com.zanh.route_sharing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "dong_y_phap_ly", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dong_y_phap_ly", columnNames = { "nguoi_dung_id", "van_ban_phap_ly_id" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class DongYPhapLy extends Base {
    @Column(name = "dong_y_luc", nullable = false)
    private Instant dongYLuc;
    @Column(name = "dia_chi_ip", nullable = false, length = 64)
    private String diaChiIp;
    @Column(name = "thong_tin_trinh_duyet", nullable = false, length = 1000)
    private String thongTinTrinhDuyet;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "van_ban_phap_ly_id", nullable = false)
    private VanBanPhapLy vanBanPhapLy;
}
