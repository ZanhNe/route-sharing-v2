package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiGiamSatChuyenDi;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "nhat_ky_giam_sat_tin_hieu", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nhat_ky_giam_sat_trip_sequence", columnNames = { "chuyen_di_id", "sequence" })
}, check = {
        @CheckConstraint(name = "ck_nhat_ky_giam_sat_transition", constraint = "trang_thai_truoc <> trang_thai_sau"),
        @CheckConstraint(name = "ck_nhat_ky_giam_sat_threshold", constraint = "delay_threshold_seconds > 0 AND lost_threshold_seconds > delay_threshold_seconds"),
        @CheckConstraint(name = "ck_nhat_ky_giam_sat_source", constraint = "source = 'SYSTEM_TEMPORAL_MONITORING'")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NhatKyGiamSatTinHieu {
    public static final String SOURCE_SYSTEM_TEMPORAL_MONITORING = "SYSTEM_TEMPORAL_MONITORING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chuyen_di_id", nullable = false)
    private ChuyenDi chuyenDi;

    @Column(name = "sequence", nullable = false)
    private Long sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_truoc", nullable = false, length = 30)
    private TrangThaiGiamSatChuyenDi trangThaiTruoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_sau", nullable = false, length = 30)
    private TrangThaiGiamSatChuyenDi trangThaiSau;

    @Column(name = "transition_at", nullable = false)
    private Instant transitionAt;

    @Column(name = "signal_reference_at", nullable = false)
    private Instant signalReferenceAt;

    @Column(name = "delay_threshold_seconds", nullable = false)
    private Long delayThresholdSeconds;

    @Column(name = "lost_threshold_seconds", nullable = false)
    private Long lostThresholdSeconds;

    @Column(name = "reason_code", nullable = false, length = 100)
    private String reasonCode;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static NhatKyGiamSatTinHieu transitioned(
            ChuyenDi trip,
            long sequence,
            TrangThaiGiamSatChuyenDi previousState,
            TrangThaiGiamSatChuyenDi resultingState,
            Instant transitionAt,
            Instant signalReferenceAt,
            long delayThresholdSeconds,
            long lostThresholdSeconds,
            String reasonCode) {
        if (trip == null || trip.getId() == null) {
            throw new IllegalArgumentException("Chuyến đi phải được lưu trước khi tạo nhật ký giám sát.");
        }
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence phải là số dương.");
        }
        Objects.requireNonNull(previousState, "previousState không được trống");
        Objects.requireNonNull(resultingState, "resultingState không được trống");
        if (previousState == resultingState || trip.getTrangThaiGiamSat() != resultingState) {
            throw new IllegalArgumentException("Monitoring transition không khớp trạng thái chuyến hiện tại.");
        }
        Objects.requireNonNull(transitionAt, "transitionAt không được trống");
        Objects.requireNonNull(signalReferenceAt, "signalReferenceAt không được trống");
        if (signalReferenceAt.isAfter(transitionAt)) {
            throw new IllegalArgumentException("signalReferenceAt không được sau transitionAt.");
        }
        if (delayThresholdSeconds <= 0 || lostThresholdSeconds <= delayThresholdSeconds) {
            throw new IllegalArgumentException("Monitoring thresholds không hợp lệ.");
        }
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("reasonCode không được trống.");
        }

        NhatKyGiamSatTinHieu history = new NhatKyGiamSatTinHieu();
        history.chuyenDi = trip;
        history.sequence = sequence;
        history.trangThaiTruoc = previousState;
        history.trangThaiSau = resultingState;
        history.transitionAt = transitionAt;
        history.signalReferenceAt = signalReferenceAt;
        history.delayThresholdSeconds = delayThresholdSeconds;
        history.lostThresholdSeconds = lostThresholdSeconds;
        history.reasonCode = reasonCode;
        history.source = SOURCE_SYSTEM_TEMPORAL_MONITORING;
        return history;
    }
}
