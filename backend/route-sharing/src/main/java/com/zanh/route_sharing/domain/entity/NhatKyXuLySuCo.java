package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiThaoTacXuLySuCo;
import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "nhat_ky_xu_ly_su_co", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nhat_ky_xu_ly_su_co_sequence", columnNames = { "su_co_chuyen_di_id", "sequence" })
}, check = {
        @CheckConstraint(name = "ck_nhat_ky_xu_ly_su_co_sequence", constraint = "sequence > 0")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NhatKyXuLySuCo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "su_co_chuyen_di_id", nullable = false)
    private SuCoChuyenDi suCoChuyenDi;

    @Column(name = "sequence", nullable = false)
    private Long sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "thao_tac", nullable = false, length = 40)
    private LoaiThaoTacXuLySuCo thaoTac;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_truoc", nullable = false, length = 30)
    private TrangThaiXuLySuCo trangThaiTruoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_sau", nullable = false, length = 30)
    private TrangThaiXuLySuCo trangThaiSau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_phu_trach_truoc_id")
    private NguoiDung nguoiPhuTrachTruoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_phu_trach_sau_id")
    private NguoiDung nguoiPhuTrachSau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private NguoiDung actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "safe_conclusion_snapshot", length = 5000)
    private String safeConclusionSnapshot;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static NhatKyXuLySuCo of(
            SuCoChuyenDi incident,
            long sequence,
            LoaiThaoTacXuLySuCo action,
            TrangThaiXuLySuCo previousStatus,
            TrangThaiXuLySuCo resultingStatus,
            NguoiDung previousHandler,
            NguoiDung resultingHandler,
            NguoiDung actor,
            Instant occurredAt,
            String reason,
            String safeConclusionSnapshot) {
        if (incident == null || incident.getId() == null || sequence <= 0 || action == null
                || previousStatus == null || resultingStatus == null || actor == null || actor.getId() == null
                || occurredAt == null) {
            throw new IllegalArgumentException("Incident handling history không hợp lệ.");
        }
        String normalizedReason = normalize(reason, 1000, "reason");
        String normalizedConclusion = normalize(safeConclusionSnapshot, 5000, "safeConclusionSnapshot");
        NhatKyXuLySuCo history = new NhatKyXuLySuCo();
        history.suCoChuyenDi = incident;
        history.sequence = sequence;
        history.thaoTac = action;
        history.trangThaiTruoc = previousStatus;
        history.trangThaiSau = resultingStatus;
        history.nguoiPhuTrachTruoc = previousHandler;
        history.nguoiPhuTrachSau = resultingHandler;
        history.actor = actor;
        history.occurredAt = occurredAt;
        history.reason = normalizedReason;
        history.safeConclusionSnapshot = normalizedConclusion;
        return history;
    }

    private static String normalize(String value, int max, String field) {
        if (value == null)
            return null;
        String normalized = value.trim();
        if (normalized.isEmpty())
            return null;
        if (normalized.length() > max)
            throw new IllegalArgumentException(field + " vượt quá " + max + " ký tự.");
        return normalized;
    }
}
