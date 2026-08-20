package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiKhieuNai;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "khieu_nai", uniqueConstraints = {
        @UniqueConstraint(name = "uk_khieu_nai_nguoi_booking", columnNames = { "nguoi_khieu_nai_id",
                "yeu_cau_di_chung_id" })
}, indexes = {
        @Index(name = "idx_khieu_nai_chuyen", columnList = "chuyen_di_id"),
        @Index(name = "idx_khieu_nai_trang_thai", columnList = "trang_thai_khieu_nai"),
        @Index(name = "idx_khieu_nai_trang_thai_reviewer", columnList = "trang_thai_khieu_nai,nguoi_tiep_nhan_id"),
        @Index(name = "idx_khieu_nai_reviewer_trang_thai", columnList = "nguoi_tiep_nhan_id,trang_thai_khieu_nai")
}, check = {
        @CheckConstraint(name = "ck_khieu_nai_noi_dung", constraint = "char_length(btrim(tieu_de)) BETWEEN 5 AND 255 AND char_length(btrim(noi_dung)) BETWEEN 20 AND 5000"),
        @CheckConstraint(name = "ck_khieu_nai_deadline", constraint = "han_nop_khieu_nai_ap_dung_luc >= nop_luc"),
        @CheckConstraint(name = "ck_khieu_nai_pairwise", constraint = "nguoi_khieu_nai_id <> nguoi_bi_khieu_nai_id"),
        @CheckConstraint(name = "ck_khieu_nai_submitted_shape", constraint = "trang_thai_khieu_nai <> 'SUBMITTED' OR (nguoi_tiep_nhan_id IS NULL AND tiep_nhan_luc IS NULL AND thoi_han_phan_hoi_ap_dung_gio IS NULL AND han_phan_hoi_ban_dau_luc IS NULL AND nguoi_duoc_yeu_cau_bo_sung_id IS NULL AND yeu_cau_bo_sung_luc IS NULL AND ly_do_yeu_cau_bo_sung IS NULL AND han_bo_sung_luc IS NULL AND moc_id_minh_chung_luc_yeu_cau IS NULL AND ket_luan IS NULL AND giai_quyet_luc IS NULL)"),
        @CheckConstraint(name = "ck_khieu_nai_review_shape", constraint = "trang_thai_khieu_nai NOT IN ('UNDER_REVIEW','NEED_MORE_EVIDENCE','ACCEPTED','REJECTED') OR (nguoi_tiep_nhan_id IS NOT NULL AND tiep_nhan_luc IS NOT NULL AND thoi_han_phan_hoi_ap_dung_gio > 0 AND han_phan_hoi_ban_dau_luc > tiep_nhan_luc)"),
        @CheckConstraint(name = "ck_khieu_nai_request_shape", constraint = "trang_thai_khieu_nai <> 'NEED_MORE_EVIDENCE' OR (nguoi_duoc_yeu_cau_bo_sung_id IS NOT NULL AND yeu_cau_bo_sung_luc IS NOT NULL AND char_length(btrim(ly_do_yeu_cau_bo_sung)) BETWEEN 10 AND 1000 AND han_bo_sung_luc > yeu_cau_bo_sung_luc AND moc_id_minh_chung_luc_yeu_cau >= 0)"),
        @CheckConstraint(name = "ck_khieu_nai_non_request_shape", constraint = "trang_thai_khieu_nai NOT IN ('SUBMITTED','UNDER_REVIEW','ACCEPTED','REJECTED') OR (nguoi_duoc_yeu_cau_bo_sung_id IS NULL AND yeu_cau_bo_sung_luc IS NULL AND ly_do_yeu_cau_bo_sung IS NULL AND han_bo_sung_luc IS NULL AND moc_id_minh_chung_luc_yeu_cau IS NULL)"),
        @CheckConstraint(name = "ck_khieu_nai_open_review_conclusion_shape", constraint = "trang_thai_khieu_nai NOT IN ('UNDER_REVIEW','NEED_MORE_EVIDENCE') OR (ket_luan IS NULL AND giai_quyet_luc IS NULL)"),
        @CheckConstraint(name = "ck_khieu_nai_terminal_shape", constraint = "trang_thai_khieu_nai NOT IN ('ACCEPTED','REJECTED') OR (char_length(btrim(ket_luan)) BETWEEN 1 AND 5000 AND giai_quyet_luc IS NOT NULL AND giai_quyet_luc >= han_phan_hoi_ban_dau_luc)")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class KhieuNai extends Base {
    @Column(name = "tieu_de", nullable = false, length = 255)
    private String tieuDe;

    @Column(name = "noi_dung", nullable = false, length = 5000)
    private String noiDung;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_khieu_nai", nullable = false, length = 40)
    private TrangThaiKhieuNai trangThaiKhieuNai;

    @Column(name = "nop_luc", nullable = false)
    private Instant nopLuc;

    @Column(name = "han_nop_khieu_nai_ap_dung_luc", nullable = false)
    private Instant hanNopKhieuNaiApDungLuc;

    @Column(name = "tiep_nhan_luc")
    private Instant tiepNhanLuc;

    @Column(name = "thoi_han_phan_hoi_ap_dung_gio")
    private Long thoiHanPhanHoiApDungGio;

    @Column(name = "han_phan_hoi_ban_dau_luc")
    private Instant hanPhanHoiBanDauLuc;

    @Column(name = "yeu_cau_bo_sung_luc")
    private Instant yeuCauBoSungLuc;

    @Column(name = "ly_do_yeu_cau_bo_sung", length = 1000)
    private String lyDoYeuCauBoSung;

    @Column(name = "han_bo_sung_luc")
    private Instant hanBoSungLuc;

    @Column(name = "moc_id_minh_chung_luc_yeu_cau")
    private Long mocIdMinhChungLucYeuCau;

    @Column(name = "ket_luan", length = 5000)
    private String ketLuan;

    @Column(name = "giai_quyet_luc")
    private Instant giaiQuyetLuc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chuyen_di_id", nullable = false)
    private ChuyenDi chuyenDi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yeu_cau_di_chung_id", nullable = false)
    private YeuCauDiChung yeuCauDiChung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "su_co_chuyen_di_id")
    private SuCoChuyenDi suCoChuyenDi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_khieu_nai_id", nullable = false)
    private NguoiDung nguoiKhieuNai;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_bi_khieu_nai_id", nullable = false)
    private NguoiDung nguoiBiKhieuNai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_tiep_nhan_id")
    private NguoiDung nguoiTiepNhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_duoc_yeu_cau_bo_sung_id")
    private NguoiDung nguoiDuocYeuCauBoSung;

    public static KhieuNai submit(
            ChuyenDi trip,
            YeuCauDiChung booking,
            NguoiDung complainant,
            NguoiDung target,
            SuCoChuyenDi optionalIncident,
            String normalizedTitle,
            String normalizedContent,
            Instant terminalAt,
            Instant submittedAt,
            Instant appliedDeadline) {
        requirePersisted(trip, "Trip");
        requirePersisted(booking, "Booking");
        requirePersisted(complainant, "Complainant");
        requirePersisted(target, "Target");
        if (booking.getChuyenDi() == null || booking.getLoTrinhChiaSe() == null
                || trip.getLoTrinhChiaSe() == null
                || !Objects.equals(booking.getChuyenDi().getId(), trip.getId())
                || !Objects.equals(booking.getLoTrinhChiaSe().getId(), trip.getLoTrinhChiaSe().getId())) {
            throw new IllegalArgumentException("Booking không thuộc exact Trip/Route của complaint.");
        }
        NguoiDung driver = trip.getLoTrinhChiaSe().getTaiXe();
        NguoiDung passenger = booking.getHanhKhach();
        requirePersisted(driver, "Driver");
        requirePersisted(passenger, "Passenger");
        boolean passengerComplains = Objects.equals(complainant.getId(), passenger.getId())
                && Objects.equals(target.getId(), driver.getId());
        boolean driverComplains = Objects.equals(complainant.getId(), driver.getId())
                && Objects.equals(target.getId(), passenger.getId());
        if (!passengerComplains && !driverComplains) {
            throw new IllegalArgumentException("Complaint phải là exact Driver-Passenger counterparty của booking.");
        }
        if (Objects.equals(complainant.getId(), target.getId())) {
            throw new IllegalArgumentException("Không thể tự khiếu nại chính mình.");
        }
        if (optionalIncident != null) {
            requirePersisted(optionalIncident, "Incident");
            if (optionalIncident.getChuyenDi() == null || optionalIncident.getNguoiBaoCao() == null
                    || !Objects.equals(optionalIncident.getChuyenDi().getId(), trip.getId())
                    || !Objects.equals(optionalIncident.getNguoiBaoCao().getId(), complainant.getId())) {
                throw new IllegalArgumentException("Incident không thuộc cùng Trip/cùng reporter.");
            }
        }
        String title = normalize(normalizedTitle, 5, 255, "title");
        String content = normalize(normalizedContent, 20, 5000, "content");
        if (terminalAt == null || submittedAt == null || appliedDeadline == null
                || submittedAt.isBefore(terminalAt) || submittedAt.isAfter(appliedDeadline)) {
            throw new IllegalArgumentException("Mốc thời gian nộp khiếu nại không hợp lệ.");
        }

        KhieuNai complaint = new KhieuNai();
        complaint.tieuDe = title;
        complaint.noiDung = content;
        complaint.trangThaiKhieuNai = TrangThaiKhieuNai.SUBMITTED;
        complaint.nopLuc = submittedAt;
        complaint.hanNopKhieuNaiApDungLuc = appliedDeadline;
        complaint.chuyenDi = trip;
        complaint.yeuCauDiChung = booking;
        complaint.suCoChuyenDi = optionalIncident;
        complaint.nguoiKhieuNai = complainant;
        complaint.nguoiBiKhieuNai = target;
        complaint.clearReviewFields();
        return complaint;
    }

    public void claimReview(NguoiDung reviewer, long responseWindowHours, Instant startedAt) {
        requirePersisted(reviewer, "Reviewer");
        if (trangThaiKhieuNai != TrangThaiKhieuNai.SUBMITTED) {
            throw new IllegalStateException("Complaint không ở trạng thái SUBMITTED để tiếp nhận.");
        }
        if (responseWindowHours <= 0 || startedAt == null) {
            throw new IllegalArgumentException("Review response window không hợp lệ.");
        }
        this.nguoiTiepNhan = reviewer;
        this.tiepNhanLuc = startedAt;
        this.thoiHanPhanHoiApDungGio = responseWindowHours;
        this.hanPhanHoiBanDauLuc = startedAt.plus(responseWindowHours, ChronoUnit.HOURS);
        clearRequestTuple();
        this.ketLuan = null;
        this.giaiQuyetLuc = null;
        this.trangThaiKhieuNai = TrangThaiKhieuNai.UNDER_REVIEW;
    }

    public void reassignReviewer(NguoiDung newReviewer) {
        requirePersisted(newReviewer, "Reviewer");
        if (trangThaiKhieuNai != TrangThaiKhieuNai.UNDER_REVIEW
                && trangThaiKhieuNai != TrangThaiKhieuNai.NEED_MORE_EVIDENCE) {
            throw new IllegalStateException("Complaint không ở trạng thái có thể chuyển reviewer.");
        }
        if (nguoiTiepNhan == null)
            throw new IllegalStateException("Complaint chưa có reviewer.");
        this.nguoiTiepNhan = newReviewer;
    }

    public void requestMoreEvidence(NguoiDung reviewer, NguoiDung target, String reason,
            long evidenceWatermark, Instant requestedAt) {
        requireCurrentReviewer(reviewer);
        requireParticipant(target);
        if (trangThaiKhieuNai != TrangThaiKhieuNai.UNDER_REVIEW) {
            throw new IllegalStateException("Complaint không ở UNDER_REVIEW để yêu cầu bổ sung.");
        }
        if (requestedAt == null || hanPhanHoiBanDauLuc == null || requestedAt.isBefore(hanPhanHoiBanDauLuc)) {
            throw new IllegalStateException("Response window ban đầu vẫn đang mở.");
        }
        String normalizedReason = normalize(reason, 10, 1000, "reason");
        if (thoiHanPhanHoiApDungGio == null || thoiHanPhanHoiApDungGio <= 0 || evidenceWatermark < 0) {
            throw new IllegalStateException("Complaint review window/watermark không hợp lệ.");
        }
        this.nguoiDuocYeuCauBoSung = target;
        this.yeuCauBoSungLuc = requestedAt;
        this.lyDoYeuCauBoSung = normalizedReason;
        this.hanBoSungLuc = requestedAt.plus(thoiHanPhanHoiApDungGio, ChronoUnit.HOURS);
        this.mocIdMinhChungLucYeuCau = evidenceWatermark;
        this.trangThaiKhieuNai = TrangThaiKhieuNai.NEED_MORE_EVIDENCE;
    }

    public void resumeReview(NguoiDung reviewer) {
        requireCurrentReviewer(reviewer);
        if (trangThaiKhieuNai != TrangThaiKhieuNai.NEED_MORE_EVIDENCE) {
            throw new IllegalStateException("Complaint không có evidence request đang mở.");
        }
        clearRequestTuple();
        this.trangThaiKhieuNai = TrangThaiKhieuNai.UNDER_REVIEW;
    }

    public void finalizeReview(NguoiDung reviewer, TrangThaiKhieuNai outcome, String conclusion, Instant finalizedAt) {
        requireCurrentReviewer(reviewer);
        if (trangThaiKhieuNai != TrangThaiKhieuNai.UNDER_REVIEW) {
            throw new IllegalStateException("Complaint không ở trạng thái có thể kết luận.");
        }
        if (outcome != TrangThaiKhieuNai.ACCEPTED && outcome != TrangThaiKhieuNai.REJECTED) {
            throw new IllegalArgumentException("Review outcome phải là ACCEPTED hoặc REJECTED.");
        }
        String normalizedConclusion = normalize(conclusion, 1, 5000, "conclusion");
        if (finalizedAt == null || hanPhanHoiBanDauLuc == null || finalizedAt.isBefore(hanPhanHoiBanDauLuc)) {
            throw new IllegalStateException("Chưa hết response window hoặc thời điểm kết luận không hợp lệ.");
        }
        clearRequestTuple();
        this.trangThaiKhieuNai = outcome;
        this.ketLuan = normalizedConclusion;
        this.giaiQuyetLuc = finalizedAt;
    }

    public boolean isTerminalReviewState() {
        return trangThaiKhieuNai == TrangThaiKhieuNai.ACCEPTED || trangThaiKhieuNai == TrangThaiKhieuNai.REJECTED;
    }

    public boolean isParticipant(Long userId) {
        return userId != null && ((nguoiKhieuNai != null && Objects.equals(nguoiKhieuNai.getId(), userId))
                || (nguoiBiKhieuNai != null && Objects.equals(nguoiBiKhieuNai.getId(), userId)));
    }

    public boolean isCurrentReviewer(Long userId) {
        return userId != null && nguoiTiepNhan != null && Objects.equals(nguoiTiepNhan.getId(), userId);
    }

    private void requireCurrentReviewer(NguoiDung reviewer) {
        requirePersisted(reviewer, "Reviewer");
        if (nguoiTiepNhan == null || !Objects.equals(nguoiTiepNhan.getId(), reviewer.getId())) {
            throw new IllegalStateException("Actor không phải current reviewer của complaint.");
        }
    }

    private void requireParticipant(NguoiDung participant) {
        requirePersisted(participant, "Participant");
        if (!isParticipant(participant.getId())) {
            throw new IllegalArgumentException("Target không phải participant của complaint.");
        }
    }

    private void clearReviewFields() {
        this.nguoiTiepNhan = null;
        this.tiepNhanLuc = null;
        this.thoiHanPhanHoiApDungGio = null;
        this.hanPhanHoiBanDauLuc = null;
        clearRequestTuple();
        this.ketLuan = null;
        this.giaiQuyetLuc = null;
    }

    private void clearRequestTuple() {
        this.nguoiDuocYeuCauBoSung = null;
        this.yeuCauBoSungLuc = null;
        this.lyDoYeuCauBoSung = null;
        this.hanBoSungLuc = null;
        this.mocIdMinhChungLucYeuCau = null;
    }

    private static String normalize(String value, int min, int max, String field) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.length() < min || normalized.length() > max) {
            throw new IllegalArgumentException(field + " không hợp lệ.");
        }
        return normalized;
    }

    private static void requirePersisted(Base entity, String name) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalArgumentException(name + " phải được lưu trước khi tạo complaint.");
        }
    }
}
