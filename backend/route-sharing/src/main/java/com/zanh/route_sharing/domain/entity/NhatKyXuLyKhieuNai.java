package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiThaoTacXuLyKhieuNai;
import com.zanh.route_sharing.domain.enums.TrangThaiKhieuNai;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "nhat_ky_xu_ly_khieu_nai", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nhat_ky_xu_ly_khieu_nai_sequence", columnNames = { "khieu_nai_id", "sequence" })
}, indexes = {
        @Index(name = "idx_nhat_ky_xu_ly_khieu_nai_complaint", columnList = "khieu_nai_id,sequence")
}, check = {
        @CheckConstraint(name = "ck_nhat_ky_xu_ly_khieu_nai_sequence", constraint = "sequence > 0")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class NhatKyXuLyKhieuNai extends Base {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "khieu_nai_id", nullable = false)
    private KhieuNai khieuNai;

    @Column(name = "sequence", nullable = false)
    private Long sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "thao_tac", nullable = false, length = 40)
    private LoaiThaoTacXuLyKhieuNai thaoTac;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_truoc", nullable = false, length = 40)
    private TrangThaiKhieuNai trangThaiTruoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_sau", nullable = false, length = 40)
    private TrangThaiKhieuNai trangThaiSau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_truoc_id")
    private NguoiDung reviewerTruoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_sau_id")
    private NguoiDung reviewerSau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private NguoiDung actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_participant_id")
    private NguoiDung targetParticipant;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "evidence_id_watermark")
    private Long evidenceIdWatermark;

    @Column(name = "reason", length = 1000)
    private String reason;

    public static NhatKyXuLyKhieuNai of(KhieuNai complaint, long sequence,
            LoaiThaoTacXuLyKhieuNai action, TrangThaiKhieuNai previousState,
            TrangThaiKhieuNai resultingState, NguoiDung previousReviewer, NguoiDung resultingReviewer,
            NguoiDung actor, NguoiDung targetParticipant, Instant occurredAt, Instant deadlineAt,
            Long evidenceIdWatermark, String reason) {
        if (complaint == null || complaint.getId() == null || sequence <= 0 || action == null
                || previousState == null || resultingState == null || actor == null || actor.getId() == null
                || occurredAt == null) {
            throw new IllegalArgumentException("Complaint handling history không hợp lệ.");
        }
        String normalizedReason = normalize(reason);
        NhatKyXuLyKhieuNai history = new NhatKyXuLyKhieuNai();
        history.khieuNai = complaint;
        history.sequence = sequence;
        history.thaoTac = action;
        history.trangThaiTruoc = previousState;
        history.trangThaiSau = resultingState;
        history.reviewerTruoc = previousReviewer;
        history.reviewerSau = resultingReviewer;
        history.actor = actor;
        history.targetParticipant = targetParticipant;
        history.occurredAt = occurredAt;
        history.deadlineAt = deadlineAt;
        history.evidenceIdWatermark = evidenceIdWatermark;
        history.reason = normalizedReason;
        return history;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank())
            return null;
        String normalized = value.trim();
        if (normalized.length() > 1000)
            throw new IllegalArgumentException("reason vượt quá 1000 ký tự.");
        return normalized;
    }
}
