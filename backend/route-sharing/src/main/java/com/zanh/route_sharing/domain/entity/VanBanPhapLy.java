package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiVanBanPhapLy;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "van_ban_phap_ly", uniqueConstraints = {
        @UniqueConstraint(name = "uk_van_ban_phap_ly_phien_ban", columnNames = { "nha_truong_id", "ma_van_ban",
                "phien_ban" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class VanBanPhapLy extends Base {
    @Column(name = "ma_van_ban", nullable = false, length = 100)
    private String maVanBan;
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_van_ban", nullable = false, length = 40)
    private LoaiVanBanPhapLy loaiVanBan;
    @Column(name = "ten_van_ban", nullable = false, length = 255)
    private String tenVanBan;
    @Column(name = "phien_ban", nullable = false, length = 50)
    private String phienBan;
    @Column(name = "noi_dung_url", nullable = false, length = 2048)
    private String noiDungUrl;
    @Column(name = "hieu_luc_tu", nullable = false)
    private Instant hieuLucTu;
    @Column(name = "hieu_luc_den")
    private Instant hieuLucDen;
    @Builder.Default
    @Column(name = "bat_buoc", nullable = false)
    private Boolean batBuoc = true;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nha_truong_id", nullable = false)
    private NhaTruong nhaTruong;
}
