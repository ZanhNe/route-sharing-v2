package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.enums.LoaiTaiNguyenNhayCam;
import com.zanh.route_sharing.dto.complaint.evidence.*;
import com.zanh.route_sharing.dto.complaint.review.ComplaintReviewerEvidencePageResponse;
import com.zanh.route_sharing.dto.response.PageMeta;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.complaint.review.ComplaintReviewRepository;
import com.zanh.route_sharing.repository.complaint.review.model.ComplaintReviewSnapshots;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.ComplaintReviewEvidenceService;
import com.zanh.route_sharing.service.complaint.evidence.*;
import com.zanh.route_sharing.service.evidence.*;
import com.zanh.route_sharing.service.complaint.evidence.model.*;
import com.zanh.route_sharing.service.complaint.review.ComplaintReviewEvidenceCommitCoordinator;
import com.zanh.route_sharing.storage.evidence.*;
import com.zanh.route_sharing.utils.PaginationPolicy;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class ComplaintReviewEvidenceServiceImpl implements ComplaintReviewEvidenceService {
    private final EvidenceBinaryStorage storage;
    private final EvidenceContentInspector inspector;
    private final EvidenceUploadLimitPolicy uploadLimit;
    private final EvidenceFilenamePolicy filenamePolicy;
    private final ComplaintEvidenceMediaPolicy mediaPolicy;
    private final ComplaintReviewEvidenceCommitCoordinator coordinator;
    private final ComplaintReviewRepository repository;
    private final Clock clock;

    public ComplaintReviewEvidenceServiceImpl(EvidenceBinaryStorage storage, EvidenceContentInspector inspector,
            EvidenceUploadLimitPolicy uploadLimit, EvidenceFilenamePolicy filenamePolicy,
            ComplaintEvidenceMediaPolicy mediaPolicy, ComplaintReviewEvidenceCommitCoordinator coordinator,
            ComplaintReviewRepository repository, Clock clock) {
        this.storage = storage;
        this.inspector = inspector;
        this.uploadLimit = uploadLimit;
        this.filenamePolicy = filenamePolicy;
        this.mediaPolicy = mediaPolicy;
        this.coordinator = coordinator;
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public EvidenceUploadResponse upload(Long actorId, Long complaintId, MultipartFile file, String description) {
        requireIds(actorId, complaintId);
        if (file == null || file.isEmpty())
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EVIDENCE_CONTENT_INVALID",
                    "Tệp bằng chứng không được trống.");
        uploadLimit.requireWithinLimit(file.getSize());
        String normalizedDescription = normalizeDescription(description);
        String displayFilename = filenamePolicy.normalize(file.getOriginalFilename());
        StagedBinary staged = null;
        try {
            staged = storage.stage(file.getInputStream());
            uploadLimit.requireWithinLimit(staged.sizeBytes());
            EvidenceInspection inspection = inspector.inspect(staged.path());
            var category = mediaPolicy.requireAllowed(inspection.verifiedMediaType());
            var result = coordinator.commit(actorId, complaintId, staged, category, inspection, displayFilename,
                    normalizedDescription);
            var e = result.evidence();
            return new EvidenceUploadResponse(e.evidenceId(), complaintId, e.category(), e.originalFilename(),
                    e.verifiedMediaType(), e.sizeBytes(), e.fingerprint(), e.uploadedAt(), e.description(),
                    result.created());
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "EVIDENCE_STORAGE_UNAVAILABLE",
                    "Kho bằng chứng hiện không khả dụng.");
        } finally {
            storage.cleanupStage(staged);
        }
    }

    @Override
    public EvidencePageResult listOwn(Long actorId, Long complaintId, int page, int size) {
        requireIds(actorId, complaintId);
        requirePage(page, size);
        var result = repository.findParticipantEvidence(actorId, complaintId, page, size);
        var items = result.items().stream().map(e -> new EvidenceItemResponse(e.evidenceId(), complaintId, e.category(),
                e.originalFilename(), e.verifiedMediaType(), e.sizeBytes(), e.fingerprint(), e.uploadedAt(),
                e.description())).toList();
        return new EvidencePageResult(new EvidencePageResponse(items), PageMeta.of(page, size, result.totalElements()));
    }

    @Override
    public EvidenceDownloadResult downloadOwn(Long actorId, Long complaintId, Long evidenceId) {
        requireIds(actorId, complaintId);
        requirePositive(evidenceId, "evidenceId");
        var e = repository.findParticipantEvidence(actorId, complaintId, evidenceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPLAINT_REVIEW_EVIDENCE_NOT_FOUND",
                        "Không tìm thấy review evidence phù hợp."));
        return verify(e);
    }

    @Override
    public ComplaintReviewerEvidencePageResponse listReviewer(Long actorId, Long complaintId, int page, int size) {
        requireIds(actorId, complaintId);
        requirePage(page, size);
        var result = repository.findReviewerEvidence(actorId, complaintId, page, size, businessDate());
        var items = result.items().stream().map(e -> new ComplaintReviewerEvidencePageResponse.Item(e.evidenceId(),
                e.uploaderRole(), e.category(), e.originalFilename(), e.verifiedMediaType(), e.sizeBytes(),
                e.description(), e.uploadedAt())).toList();
        return new ComplaintReviewerEvidencePageResponse(items, PageMeta.of(page, size, result.totalElements()));
    }

    @Override
    public EvidenceDownloadResult downloadReviewer(Long actorId, Long complaintId, Long evidenceId, String ip,
            String userAgent) {
        requireIds(actorId, complaintId);
        requirePositive(evidenceId, "evidenceId");
        Instant readTime = TimePolicy.now(clock);
        LocalDate date = businessDate(readTime);
        var e = repository.findReviewerEvidence(actorId, complaintId, evidenceId, date)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPLAINT_REVIEW_EVIDENCE_NOT_FOUND",
                        "Không tìm thấy review evidence phù hợp."));
        EvidenceDownloadResult result = verify(e);
        repository.recordSensitiveRead(actorId, complaintId, evidenceId, LoaiTaiNguyenNhayCam.TEP_MINH_CHUNG,
                "Xử lý khiếu nại: đọc raw evidence", ip, userAgent, readTime, date);
        return result;
    }

    private EvidenceDownloadResult verify(ComplaintReviewSnapshots.Evidence e) {
        try {
            VerifiedBinary v = storage.verify(e.storageKey(), e.sizeBytes(), e.fingerprint());
            return new EvidenceDownloadResult(v.resource(), e.originalFilename(), e.verifiedMediaType(), e.sizeBytes());
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "EVIDENCE_STORAGE_INTEGRITY_VIOLATION",
                    "Bằng chứng lưu trữ không còn nhất quán.");
        }
    }

    private LocalDate businessDate() {
        return businessDate(TimePolicy.now(clock));
    }

    private static LocalDate businessDate(Instant instant) {
        return LocalDate.ofInstant(instant, TimePolicy.BUSINESS_ZONE);
    }

    private static void requireIds(Long actorId, Long complaintId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        requirePositive(complaintId, "complaintId");
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0)
            throw validation(field + " phải là số dương.");
    }

    private static void requirePage(int page, int size) {
        if (!PaginationPolicy.isValid(page, size))
            throw validation("Thông tin phân trang không hợp lệ.");
    }

    private static String normalizeDescription(String value) {
        if (value == null || value.isBlank())
            return null;
        String n = value.trim();
        if (n.length() > 2000)
            throw validation("description không được vượt quá 2000 ký tự.");
        return n;
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
