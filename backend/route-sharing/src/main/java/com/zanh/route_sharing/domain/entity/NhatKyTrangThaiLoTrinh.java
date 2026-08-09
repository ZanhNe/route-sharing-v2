package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "nhat_ky_trang_thai_lo_trinh", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nhat_ky_lo_trinh_sequence", columnNames = {
                "lo_trinh_chia_se_id", "sequence"
        })
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NhatKyTrangThaiLoTrinh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lo_trinh_chia_se_id", nullable = false)
    private LoTrinhChiaSe loTrinhChiaSe;

    @Column(name = "sequence", nullable = false)
    private Long sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_truoc", nullable = false, length = 30)
    private TrangThaiLoTrinh trangThaiTruoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_sau", nullable = false, length = 30)
    private TrangThaiLoTrinh trangThaiSau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private NguoiDung actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "reason_code", nullable = false, length = 100)
    private String reasonCode;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static NhatKyTrangThaiLoTrinh tripFormed(
            LoTrinhChiaSe route,
            NguoiDung driver,
            Instant occurredAt,
            long sequence) {
        if (route == null || route.getId() == null) {
            throw new IllegalArgumentException("Lộ trình phải được lưu trước khi tạo nhật ký.");
        }
        if (route.getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED || route.getChuyenDi() == null) {
            throw new IllegalArgumentException("Trạng thái lộ trình không khớp sự kiện hình thành chuyến đi.");
        }
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence phải là số dương.");
        }
        if (driver == null || route.getTaiXe() == null
                || !Objects.equals(driver.getId(), route.getTaiXe().getId())) {
            throw new IllegalArgumentException("Actor phải là tài xế sở hữu lộ trình.");
        }
        NhatKyTrangThaiLoTrinh event = new NhatKyTrangThaiLoTrinh();
        event.loTrinhChiaSe = route;
        event.sequence = sequence;
        event.trangThaiTruoc = TrangThaiLoTrinh.OPEN;
        event.trangThaiSau = TrangThaiLoTrinh.LOCKED;
        event.actor = driver;
        event.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt không được trống");
        event.reasonCode = "DRIVER_LOCKED_ROUTE_FOR_TRIP";
        return event;
    }

    public static NhatKyTrangThaiLoTrinh driverCancelled(
            LoTrinhChiaSe route,
            NguoiDung driver,
            Instant occurredAt,
            long sequence) {
        if (route == null || route.getId() == null) {
            throw new IllegalArgumentException("Lộ trình phải được lưu trước khi tạo nhật ký.");
        }
        if (route.getTrangThaiLoTrinh() != TrangThaiLoTrinh.CANCELLED) {
            throw new IllegalArgumentException("Trạng thái lộ trình không khớp sự kiện hủy.");
        }
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence phải là số dương.");
        }
        if (driver == null || route.getTaiXe() == null
                || !Objects.equals(driver.getId(), route.getTaiXe().getId())) {
            throw new IllegalArgumentException("Actor phải là tài xế sở hữu lộ trình.");
        }
        NhatKyTrangThaiLoTrinh event = new NhatKyTrangThaiLoTrinh();
        event.loTrinhChiaSe = route;
        event.sequence = sequence;
        event.trangThaiTruoc = TrangThaiLoTrinh.OPEN;
        event.trangThaiSau = TrangThaiLoTrinh.CANCELLED;
        event.actor = driver;
        event.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt không được trống");
        event.reasonCode = "DRIVER_CANCELLED_ROUTE";
        return event;
    }
}
