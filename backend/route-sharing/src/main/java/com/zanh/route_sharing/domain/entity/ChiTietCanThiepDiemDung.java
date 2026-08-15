package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "chi_tiet_can_thiep_diem_dung",
        uniqueConstraints = @UniqueConstraint(name = "uk_chi_tiet_can_thiep_stop", columnNames = {"can_thiep_an_toan_chuyen_di_id", "diem_dung_hanh_trinh_id"}),
        indexes = @Index(name = "idx_chi_tiet_can_thiep_stop", columnList = "diem_dung_hanh_trinh_id"),
        check = @CheckConstraint(name = "ck_chi_tiet_can_thiep_xay_ra_luc", constraint = "xay_ra_luc IS NOT NULL"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ChiTietCanThiepDiemDung extends Base {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "can_thiep_an_toan_chuyen_di_id", nullable = false)
    private CanThiepAnToanChuyenDi canThiepAnToanChuyenDi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diem_dung_hanh_trinh_id", nullable = false)
    private DiemDungHanhTrinh diemDungHanhTrinh;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_truoc", nullable = false, length = 30)
    private TrangThaiDiemDung trangThaiTruoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_sau", nullable = false, length = 30)
    private TrangThaiDiemDung trangThaiSau;

    @Column(name = "han_cho_luc_truoc")
    private Instant hanChoLucTruoc;

    @Column(name = "han_cho_luc_sau")
    private Instant hanChoLucSau;

    @Column(name = "xay_ra_luc", nullable = false)
    private Instant xayRaLuc;

    public static ChiTietCanThiepDiemDung of(CanThiepAnToanChuyenDi intervention, DiemDungHanhTrinh stop,
                                              TrangThaiDiemDung previous, TrangThaiDiemDung resulting,
                                              Instant previousDeadline, Instant resultingDeadline, Instant occurredAt) {
        Objects.requireNonNull(intervention, "intervention không được trống");
        Objects.requireNonNull(stop, "stop không được trống");
        Objects.requireNonNull(previous, "previous status không được trống");
        Objects.requireNonNull(resulting, "resulting status không được trống");
        Objects.requireNonNull(occurredAt, "occurredAt không được trống");
        if (previous == resulting && Objects.equals(previousDeadline, resultingDeadline)) {
            throw new IllegalArgumentException("Stop intervention detail phải có material change.");
        }
        ChiTietCanThiepDiemDung x = new ChiTietCanThiepDiemDung();
        x.canThiepAnToanChuyenDi = intervention; x.diemDungHanhTrinh = stop;
        x.trangThaiTruoc = previous; x.trangThaiSau = resulting;
        x.hanChoLucTruoc = previousDeadline; x.hanChoLucSau = resultingDeadline; x.xayRaLuc = occurredAt;
        return x;
    }
}
