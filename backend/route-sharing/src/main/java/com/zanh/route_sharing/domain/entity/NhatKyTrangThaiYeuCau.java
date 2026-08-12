package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
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
@Table(name = "nhat_ky_trang_thai_yeu_cau", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nhat_ky_yeu_cau_sequence", columnNames = { "yeu_cau_di_chung_id", "sequence" })
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NhatKyTrangThaiYeuCau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yeu_cau_di_chung_id", nullable = false)
    private YeuCauDiChung yeuCauDiChung;

    @Column(name = "sequence", nullable = false)
    private Long sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_truoc", length = 40)
    private TrangThaiYeuCau trangThaiTruoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_sau", nullable = false, length = 40)
    private TrangThaiYeuCau trangThaiSau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private NguoiDung actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "reason_code", length = 100)
    private String reasonCode;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static NhatKyTrangThaiYeuCau created(
            YeuCauDiChung rideRequest,
            NguoiDung actor,
            Instant occurredAt) {
        if (rideRequest == null || rideRequest.getId() == null) {
            throw new IllegalArgumentException("Yêu cầu đi chung phải được lưu trước khi tạo nhật ký.");
        }
        NhatKyTrangThaiYeuCau event = new NhatKyTrangThaiYeuCau();
        event.yeuCauDiChung = rideRequest;
        event.sequence = 1L;
        event.trangThaiTruoc = null;
        event.trangThaiSau = TrangThaiYeuCau.PENDING;
        event.actor = Objects.requireNonNull(actor, "actor không được trống");
        event.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt không được trống");
        event.reasonCode = "CREATED";
        return event;
    }

    public static NhatKyTrangThaiYeuCau accepted(
            YeuCauDiChung rideRequest,
            NguoiDung actor,
            Instant occurredAt) {
        return transition(rideRequest, actor, occurredAt,
                TrangThaiYeuCau.PENDING, TrangThaiYeuCau.ACCEPTED, "ACCEPTED");
    }

    public static NhatKyTrangThaiYeuCau rejected(
            YeuCauDiChung rideRequest,
            NguoiDung actor,
            Instant occurredAt) {
        return transition(rideRequest, actor, occurredAt,
                TrangThaiYeuCau.PENDING, TrangThaiYeuCau.REJECTED, "REJECTED");
    }

    public static NhatKyTrangThaiYeuCau cancelledByPassenger(
            YeuCauDiChung rideRequest,
            NguoiDung actor,
            Instant occurredAt,
            TrangThaiYeuCau previous) {
        if (previous != TrangThaiYeuCau.PENDING && previous != TrangThaiYeuCau.ACCEPTED) {
            throw new IllegalArgumentException("Trạng thái trước khi hành khách hủy không hợp lệ.");
        }
        return transitionWithSequence(
                rideRequest, actor, occurredAt, previous,
                TrangThaiYeuCau.CANCELLED_BY_PASSENGER,
                "CANCELLED_BY_PASSENGER",
                previous == TrangThaiYeuCau.PENDING ? 2L : 3L);
    }

    public static NhatKyTrangThaiYeuCau routeCancelledByDriver(
            YeuCauDiChung rideRequest,
            NguoiDung actor,
            Instant occurredAt,
            TrangThaiYeuCau previous,
            long sequence) {
        if (previous != TrangThaiYeuCau.PENDING && previous != TrangThaiYeuCau.ACCEPTED) {
            throw new IllegalArgumentException("Trạng thái trước khi route cascade không hợp lệ.");
        }
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence phải là số dương.");
        }
        return transitionWithSequence(
                rideRequest, actor, occurredAt, previous,
                TrangThaiYeuCau.CANCELLED_BY_DRIVER,
                "ROUTE_CANCELLED_BY_DRIVER", sequence);
    }

    public static NhatKyTrangThaiYeuCau tripCancelledBeforeStart(
            YeuCauDiChung rideRequest,
            NguoiDung actor,
            Instant occurredAt,
            long sequence) {
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence phải là số dương.");
        }
        return transitionWithSequence(
                rideRequest, actor, occurredAt,
                TrangThaiYeuCau.ACCEPTED,
                TrangThaiYeuCau.CANCELLED_BY_DRIVER,
                "TRIP_CANCELLED_BEFORE_START",
                sequence);
    }

    public static NhatKyTrangThaiYeuCau boarded(
            YeuCauDiChung rideRequest,
            NguoiDung actor,
            Instant occurredAt,
            long sequence) {
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence phải là số dương.");
        }
        return transitionWithSequence(
                rideRequest, actor, occurredAt,
                TrangThaiYeuCau.ACCEPTED,
                TrangThaiYeuCau.ON_BOARD,
                "BOARDING_CREDENTIAL_VERIFIED",
                sequence);
    }

    public static NhatKyTrangThaiYeuCau passengerNoShow(
            YeuCauDiChung rideRequest,
            NguoiDung actor,
            Instant occurredAt,
            long sequence) {
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence phải là số dương.");
        }
        return transitionWithSequence(
                rideRequest, actor, occurredAt,
                TrangThaiYeuCau.ACCEPTED,
                TrangThaiYeuCau.NO_SHOW,
                "PASSENGER_NO_SHOW_CONFIRMED",
                sequence);
    }

    private static NhatKyTrangThaiYeuCau transitionWithSequence(
            YeuCauDiChung rideRequest,
            NguoiDung actor,
            Instant occurredAt,
            TrangThaiYeuCau previous,
            TrangThaiYeuCau next,
            String reasonCode,
            long sequence) {
        if (rideRequest == null || rideRequest.getId() == null) {
            throw new IllegalArgumentException("Yêu cầu đi chung phải được lưu trước khi tạo nhật ký.");
        }
        if (rideRequest.getTrangThaiYeuCau() != next) {
            throw new IllegalArgumentException("Trạng thái yêu cầu không khớp sự kiện cần ghi.");
        }
        NhatKyTrangThaiYeuCau event = new NhatKyTrangThaiYeuCau();
        event.yeuCauDiChung = rideRequest;
        event.sequence = sequence;
        event.trangThaiTruoc = previous;
        event.trangThaiSau = next;
        event.actor = Objects.requireNonNull(actor, "actor không được trống");
        event.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt không được trống");
        event.reasonCode = reasonCode;
        return event;
    }

    private static NhatKyTrangThaiYeuCau transition(
            YeuCauDiChung rideRequest,
            NguoiDung actor,
            Instant occurredAt,
            TrangThaiYeuCau previous,
            TrangThaiYeuCau next,
            String reasonCode) {
        if (rideRequest == null || rideRequest.getId() == null) {
            throw new IllegalArgumentException("Yêu cầu đi chung phải được lưu trước khi tạo nhật ký.");
        }
        if (rideRequest.getTrangThaiYeuCau() != next) {
            throw new IllegalArgumentException("Trạng thái yêu cầu không khớp sự kiện cần ghi.");
        }
        NhatKyTrangThaiYeuCau event = new NhatKyTrangThaiYeuCau();
        event.yeuCauDiChung = rideRequest;
        event.sequence = 2L;
        event.trangThaiTruoc = previous;
        event.trangThaiSau = next;
        event.actor = Objects.requireNonNull(actor, "actor không được trống");
        event.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt không được trống");
        event.reasonCode = reasonCode;
        return event;
    }

}
