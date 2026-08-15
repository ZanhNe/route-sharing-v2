package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "nhat_ky_trang_thai_chuyen_di", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nhat_ky_chuyen_di_sequence", columnNames = { "chuyen_di_id", "sequence" })
}, indexes = @Index(name = "idx_nhat_ky_chuyen_di_can_thiep", columnList = "can_thiep_an_toan_chuyen_di_id"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NhatKyTrangThaiChuyenDi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chuyen_di_id", nullable = false)
    private ChuyenDi chuyenDi;

    @Column(name = "sequence", nullable = false)
    private Long sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_truoc", nullable = false, length = 40)
    private TrangThaiVanHanhChuyenDi trangThaiTruoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_sau", nullable = false, length = 40)
    private TrangThaiVanHanhChuyenDi trangThaiSau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private NguoiDung actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "reason_code", nullable = false, length = 100)
    private String reasonCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "can_thiep_an_toan_chuyen_di_id")
    private CanThiepAnToanChuyenDi canThiepAnToanChuyenDi;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static NhatKyTrangThaiChuyenDi safetyTransition(
            ChuyenDi trip, NguoiDung actor, Instant occurredAt, long sequence,
            TrangThaiVanHanhChuyenDi previous, TrangThaiVanHanhChuyenDi next,
            String reasonCode, CanThiepAnToanChuyenDi intervention) {
        if (trip == null || trip.getId() == null || actor == null || occurredAt == null || sequence <= 0
                || previous == null || next == null || previous == next || intervention == null) {
            throw new IllegalArgumentException("Safety Trip history data không hợp lệ.");
        }
        if (trip.getTrangThaiVanHanh() != next) throw new IllegalArgumentException("Trip current state không khớp history result.");
        NhatKyTrangThaiChuyenDi event = new NhatKyTrangThaiChuyenDi();
        event.chuyenDi = trip; event.sequence = sequence; event.trangThaiTruoc = previous; event.trangThaiSau = next;
        event.actor = actor; event.occurredAt = occurredAt; event.reasonCode = reasonCode; event.canThiepAnToanChuyenDi = intervention;
        return event;
    }

    public static NhatKyTrangThaiChuyenDi driverStarted(
            ChuyenDi trip,
            NguoiDung driver,
            Instant occurredAt,
            long sequence) {
        if (trip == null || trip.getId() == null) {
            throw new IllegalArgumentException("Chuyến đi phải được lưu trước khi tạo nhật ký.");
        }
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                || trip.getBatDauLuc() == null) {
            throw new IllegalArgumentException("Trạng thái chuyến không khớp sự kiện bắt đầu.");
        }
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence phải là số dương.");
        }
        if (driver == null || trip.getLoTrinhChiaSe() == null
                || trip.getLoTrinhChiaSe().getTaiXe() == null
                || !Objects.equals(driver.getId(), trip.getLoTrinhChiaSe().getTaiXe().getId())) {
            throw new IllegalArgumentException("Actor phải là tài xế sở hữu chuyến đi.");
        }
        NhatKyTrangThaiChuyenDi event = new NhatKyTrangThaiChuyenDi();
        event.chuyenDi = trip;
        event.sequence = sequence;
        event.trangThaiTruoc = TrangThaiVanHanhChuyenDi.PREPARING;
        event.trangThaiSau = TrangThaiVanHanhChuyenDi.IN_PROGRESS;
        event.actor = driver;
        event.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt không được trống");
        if (!event.occurredAt.equals(trip.getBatDauLuc())) {
            throw new IllegalArgumentException("occurredAt phải khớp thời điểm bắt đầu chuyến.");
        }
        event.reasonCode = "DRIVER_STARTED_TRIP";
        return event;
    }

    public static NhatKyTrangThaiChuyenDi driverCancelledBeforeStart(
            ChuyenDi trip,
            NguoiDung driver,
            Instant occurredAt,
            long sequence) {
        if (trip == null || trip.getId() == null) {
            throw new IllegalArgumentException("Chuyến đi phải được lưu trước khi tạo nhật ký.");
        }
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START
                || trip.getBatDauLuc() != null) {
            throw new IllegalArgumentException("Trạng thái chuyến không khớp sự kiện hủy trước Start.");
        }
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence phải là số dương.");
        }
        if (driver == null || trip.getLoTrinhChiaSe() == null
                || trip.getLoTrinhChiaSe().getTaiXe() == null
                || !Objects.equals(driver.getId(), trip.getLoTrinhChiaSe().getTaiXe().getId())) {
            throw new IllegalArgumentException("Actor phải là tài xế sở hữu chuyến đi.");
        }
        NhatKyTrangThaiChuyenDi event = new NhatKyTrangThaiChuyenDi();
        event.chuyenDi = trip;
        event.sequence = sequence;
        event.trangThaiTruoc = TrangThaiVanHanhChuyenDi.PREPARING;
        event.trangThaiSau = TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START;
        event.actor = driver;
        event.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt không được trống");
        event.reasonCode = "DRIVER_CANCELLED_TRIP_BEFORE_START";
        return event;
    }
}
