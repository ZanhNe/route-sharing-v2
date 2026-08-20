package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.complaint.evidence.EvidenceUploadResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.complaint.evidence.ComplaintEvidenceRepository;
import com.zanh.route_sharing.service.ComplaintEvidenceService;
import com.zanh.route_sharing.service.complaint.evidence.*;
import com.zanh.route_sharing.service.evidence.*;
import com.zanh.route_sharing.service.complaint.evidence.model.*;
import com.zanh.route_sharing.storage.evidence.*;
import com.zanh.route_sharing.utils.PaginationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ComplaintEvidenceServiceImpl implements ComplaintEvidenceService {
    private final EvidenceBinaryStorage storage;
    private final EvidenceContentInspector inspector;
    private final EvidenceUploadLimitPolicy uploadLimit;
    private final EvidenceFilenamePolicy filenamePolicy;
    private final ComplaintEvidenceMediaPolicy mediaPolicy;
    private final ComplaintEvidenceCommitCoordinator coordinator;
    private final ComplaintEvidenceRepository repository;
    private final ComplaintEvidenceResponseMapper mapper;

    public ComplaintEvidenceServiceImpl(EvidenceBinaryStorage storage, EvidenceContentInspector inspector,
            EvidenceUploadLimitPolicy uploadLimit, EvidenceFilenamePolicy filenamePolicy,
            ComplaintEvidenceMediaPolicy mediaPolicy, ComplaintEvidenceCommitCoordinator coordinator,
            ComplaintEvidenceRepository repository, ComplaintEvidenceResponseMapper mapper) {
        this.storage = storage;
        this.inspector = inspector;
        this.uploadLimit = uploadLimit;
        this.filenamePolicy = filenamePolicy;
        this.mediaPolicy = mediaPolicy;
        this.coordinator = coordinator;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public EvidenceUploadResponse upload(Long actorId, Long complaintId, MultipartFile file, String description) {
        requirePositive(actorId, "actorId");
        requirePositive(complaintId, "complaintId");
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
            EvidenceUploadResult result = coordinator.commit(actorId, complaintId, staged, category, inspection,
                    displayFilename, normalizedDescription);
            return mapper.upload(result);
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
        requirePositive(actorId, "actorId");
        requirePositive(complaintId, "complaintId");
        if (!PaginationPolicy.isValid(page, size))
            throw validation("Thông tin phân trang không hợp lệ.");
        return mapper.page(repository.findOwnPage(actorId, complaintId, page, size));
    }

    @Override
    public EvidenceDownloadResult downloadOwn(Long actorId, Long complaintId, Long evidenceId) {
        requirePositive(actorId, "actorId");
        requirePositive(complaintId, "complaintId");
        requirePositive(evidenceId, "evidenceId");
        var row = repository.findOwnEvidence(actorId, complaintId, evidenceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EVIDENCE_NOT_FOUND",
                        "Không tìm thấy bằng chứng phù hợp."));
        try {
            VerifiedBinary verified = storage.verify(row.storageKey(), row.sizeBytes(), row.fingerprint());
            return new EvidenceDownloadResult(verified.resource(), row.originalFilename(), row.verifiedMediaType(),
                    row.sizeBytes());
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "EVIDENCE_STORAGE_INTEGRITY_VIOLATION",
                    "Bằng chứng lưu trữ không còn nhất quán.");
        }
    }

    private static String normalizeDescription(String value) {
        if (value == null || value.isBlank())
            return null;
        String normalized = value.trim();
        if (normalized.length() > 2000)
            throw validation("description không được vượt quá 2000 ký tự.");
        return normalized;
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0)
            throw validation(field + " phải là số dương.");
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
