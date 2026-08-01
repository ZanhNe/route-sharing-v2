package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiThamDinh;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "lan_tham_dinh", indexes = {
                @Index(name = "idx_lan_tham_dinh_trang_thai", columnList = "trang_thai"),
                @Index(name = "idx_lan_tham_dinh_nguoi_duyet", columnList = "nguoi_duyet_id")
})
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "loai_tham_dinh", discriminatorType = DiscriminatorType.STRING, length = 40)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public abstract class LanThamDinh extends Base {
        @Column(name = "lan_nop", nullable = false)
        private Integer lanNop;
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "trang_thai", nullable = false, length = 40)
        private TrangThaiThamDinh trangThai = TrangThaiThamDinh.DRAFT;
        @Column(name = "nop_luc")
        private Instant nopLuc;
        @Column(name = "bat_dau_xet_luc")
        private Instant batDauXetLuc;
        @Column(name = "ket_thuc_xet_luc")
        private Instant ketThucXetLuc;
        @Column(name = "noi_dung_yeu_cau_bo_sung", length = 3000)
        private String noiDungYeuCauBoSung;
        @Column(name = "ly_do_tu_choi", length = 3000)
        private String lyDoTuChoi;
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "nguoi_duyet_id")
        private NguoiDung nguoiDuyet;
        @Builder.Default
        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(name = "lan_tham_dinh_giay_to", joinColumns = @JoinColumn(name = "lan_tham_dinh_id"), inverseJoinColumns = @JoinColumn(name = "giay_to_id"), uniqueConstraints = @UniqueConstraint(name = "uk_lan_tham_dinh_giay_to", columnNames = {
                        "lan_tham_dinh_id", "giay_to_id" }))
        private Set<GiayTo> danhSachGiayTo = new LinkedHashSet<>();
}
