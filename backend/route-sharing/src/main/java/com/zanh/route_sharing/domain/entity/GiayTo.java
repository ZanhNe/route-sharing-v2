package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiGiayTo;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "giay_to", indexes = {
        @Index(name = "idx_giay_to_nguoi_khai_bao", columnList = "nguoi_khai_bao_id"),
        @Index(name = "idx_giay_to_trang_thai", columnList = "trang_thai_giay_to")
})
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "loai_giay_to", discriminatorType = DiscriminatorType.STRING, length = 40)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public abstract class GiayTo extends Base {
    @Column(name = "so_giay_to", nullable = false, length = 100)
    private String soGiayTo;
    @Column(name = "ngay_cap", nullable = false)
    private LocalDate ngayCap;
    @Column(name = "ngay_het_han")
    private LocalDate ngayHetHan;
    @Column(name = "co_quan_cap", nullable = false, length = 255)
    private String coQuanCap;
    @Column(name = "mat_truoc_url", nullable = false, length = 2048)
    private String matTruocUrl;
    @Column(name = "mat_sau_url", length = 2048)
    private String matSauUrl;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_giay_to", nullable = false, length = 30)
    private TrangThaiGiayTo trangThaiGiayTo = TrangThaiGiayTo.PENDING;
    @Column(name = "ly_do_tu_choi", length = 2000)
    private String lyDoTuChoi;
    @Column(name = "ngay_duyet")
    private Instant ngayDuyet;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_khai_bao_id", nullable = false)
    private NguoiDung nguoiKhaiBao;
    @Builder.Default
    @ManyToMany(mappedBy = "danhSachGiayTo", fetch = FetchType.LAZY)
    private Set<LanThamDinh> danhSachLanThamDinh = new LinkedHashSet<>();
}
