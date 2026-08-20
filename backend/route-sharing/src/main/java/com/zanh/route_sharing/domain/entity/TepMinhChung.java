package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiTepMinhChung;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "tep_minh_chung", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tep_minh_chung_complaint_uploader_hash", columnNames = { "khieu_nai_id",
                "nguoi_tai_len_id", "file_hash" })
}, indexes = {
        @Index(name = "idx_tep_minh_chung_khieu_nai", columnList = "khieu_nai_id"),
        @Index(name = "idx_tep_minh_chung_su_co", columnList = "su_co_chuyen_di_id"),
        @Index(name = "idx_tep_minh_chung_khieu_nai_time", columnList = "khieu_nai_id,tai_len_luc,id")
}, check = {
        @CheckConstraint(name = "ck_tep_minh_chung_xor", constraint = "(khieu_nai_id IS NOT NULL AND su_co_chuyen_di_id IS NULL) "
                + "OR (khieu_nai_id IS NULL AND su_co_chuyen_di_id IS NOT NULL)"),
        @CheckConstraint(name = "ck_tep_minh_chung_size", constraint = "size_bytes > 0"),
        @CheckConstraint(name = "ck_tep_minh_chung_hash", constraint = "file_hash ~ '^[0-9a-f]{64}$'"),
        @CheckConstraint(name = "ck_tep_minh_chung_storage_key", constraint = "char_length(btrim(storage_key)) BETWEEN 1 AND 512"),
        @CheckConstraint(name = "ck_tep_minh_chung_filename", constraint = "char_length(btrim(original_filename)) BETWEEN 1 AND 255"),
        @CheckConstraint(name = "ck_tep_minh_chung_media_type", constraint = "char_length(btrim(verified_media_type)) BETWEEN 1 AND 255"),
        @CheckConstraint(name = "ck_tep_minh_chung_complaint_category", constraint = "khieu_nai_id IS NULL OR loai_tep <> 'OTHER'")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TepMinhChung extends Base {
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_tep", nullable = false, length = 20)
    private LoaiTepMinhChung loaiTep;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "verified_media_type", nullable = false, length = 255)
    private String verifiedMediaType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "mo_ta", length = 2000)
    private String moTa;

    @Column(name = "tai_len_luc", nullable = false)
    private Instant taiLenLuc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_tai_len_id", nullable = false)
    private NguoiDung nguoiTaiLen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khieu_nai_id")
    private KhieuNai khieuNai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "su_co_chuyen_di_id")
    private SuCoChuyenDi suCoChuyenDi;

    public static TepMinhChung attachToComplaint(
            KhieuNai complaint,
            NguoiDung uploader,
            LoaiTepMinhChung category,
            String originalFilename,
            String verifiedMediaType,
            long sizeBytes,
            String sha256Hex,
            String storageKey,
            String optionalDescription,
            Instant uploadedAt) {
        requirePersisted(complaint, "Complaint");
        requirePersisted(uploader, "Uploader");
        if (complaint.getNguoiKhieuNai() == null || complaint.getNguoiKhieuNai().getId() == null
                || !Objects.equals(complaint.getNguoiKhieuNai().getId(), uploader.getId())) {
            throw new IllegalArgumentException("Uploader phải là complainant của complaint.");
        }
        if (category == null || category == LoaiTepMinhChung.OTHER) {
            throw new IllegalArgumentException("Loại bằng chứng complaint không hợp lệ.");
        }
        if (sizeBytes <= 0 || uploadedAt == null) {
            throw new IllegalArgumentException("Kích thước/thời điểm upload không hợp lệ.");
        }
        String filename = normalizeRequired(originalFilename, 255, "originalFilename");
        String mediaType = normalizeRequired(verifiedMediaType, 255, "verifiedMediaType").toLowerCase(Locale.ROOT);
        String hash = normalizeRequired(sha256Hex, 64, "sha256Hex").toLowerCase(Locale.ROOT);
        if (!hash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("SHA-256 fingerprint không hợp lệ.");
        }
        String key = normalizeRequired(storageKey, 512, "storageKey");
        String description = normalizeOptional(optionalDescription, 2000, "description");

        TepMinhChung evidence = new TepMinhChung();
        evidence.loaiTep = category;
        evidence.originalFilename = filename;
        evidence.verifiedMediaType = mediaType;
        evidence.sizeBytes = sizeBytes;
        evidence.fileHash = hash;
        evidence.storageKey = key;
        evidence.moTa = description;
        evidence.taiLenLuc = uploadedAt;
        evidence.nguoiTaiLen = uploader;
        evidence.khieuNai = complaint;
        evidence.suCoChuyenDi = null;
        return evidence;
    }

    public static TepMinhChung attachToComplaintReview(
            KhieuNai complaint,
            NguoiDung uploader,
            LoaiTepMinhChung category,
            String originalFilename,
            String verifiedMediaType,
            long sizeBytes,
            String sha256Hex,
            String storageKey,
            String optionalDescription,
            Instant uploadedAt) {
        requirePersisted(complaint, "Complaint");
        requirePersisted(uploader, "Uploader");
        boolean complainant = complaint.getNguoiKhieuNai() != null
                && Objects.equals(complaint.getNguoiKhieuNai().getId(), uploader.getId());
        boolean respondent = complaint.getNguoiBiKhieuNai() != null
                && Objects.equals(complaint.getNguoiBiKhieuNai().getId(), uploader.getId());
        if (!complainant && !respondent) {
            throw new IllegalArgumentException("Uploader phải là exact participant của complaint.");
        }
        if (category == null || category == LoaiTepMinhChung.OTHER) {
            throw new IllegalArgumentException("Loại bằng chứng complaint không hợp lệ.");
        }
        if (sizeBytes <= 0 || uploadedAt == null) {
            throw new IllegalArgumentException("Kích thước/thời điểm upload không hợp lệ.");
        }
        String filename = normalizeRequired(originalFilename, 255, "originalFilename");
        String mediaType = normalizeRequired(verifiedMediaType, 255, "verifiedMediaType").toLowerCase(Locale.ROOT);
        String hash = normalizeRequired(sha256Hex, 64, "sha256Hex").toLowerCase(Locale.ROOT);
        if (!hash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("SHA-256 fingerprint không hợp lệ.");
        }
        String key = normalizeRequired(storageKey, 512, "storageKey");
        String description = normalizeOptional(optionalDescription, 2000, "description");
        TepMinhChung evidence = new TepMinhChung();
        evidence.loaiTep = category;
        evidence.originalFilename = filename;
        evidence.verifiedMediaType = mediaType;
        evidence.sizeBytes = sizeBytes;
        evidence.fileHash = hash;
        evidence.storageKey = key;
        evidence.moTa = description;
        evidence.taiLenLuc = uploadedAt;
        evidence.nguoiTaiLen = uploader;
        evidence.khieuNai = complaint;
        evidence.suCoChuyenDi = null;
        return evidence;
    }

    private static void requirePersisted(Base entity, String name) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalArgumentException(name + " phải được lưu trước.");
        }
    }

    private static String normalizeRequired(String value, int max, String field) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty() || normalized.length() > max) {
            throw new IllegalArgumentException(field + " không hợp lệ.");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, int max, String field) {
        if (value == null || value.isBlank())
            return null;
        String normalized = value.trim();
        if (normalized.length() > max)
            throw new IllegalArgumentException(field + " không hợp lệ.");
        return normalized;
    }
}
