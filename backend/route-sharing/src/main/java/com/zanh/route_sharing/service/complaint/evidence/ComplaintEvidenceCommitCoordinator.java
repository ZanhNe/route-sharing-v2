package com.zanh.route_sharing.service.complaint.evidence;

import com.zanh.route_sharing.domain.entity.KhieuNai;
import com.zanh.route_sharing.domain.enums.TrangThaiKhieuNai;
import com.zanh.route_sharing.domain.enums.LoaiTepMinhChung;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.complaint.evidence.ComplaintEvidenceRepository;
import com.zanh.route_sharing.repository.complaint.evidence.model.*;
import com.zanh.route_sharing.service.complaint.evidence.model.EvidenceUploadResult;
import com.zanh.route_sharing.service.evidence.EvidenceInspection;
import com.zanh.route_sharing.storage.evidence.*;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

@Component
public class ComplaintEvidenceCommitCoordinator {
    private final ComplaintEvidenceRepository repository;
    private final EvidenceBinaryStorage storage;
    private final Clock clock;

    public ComplaintEvidenceCommitCoordinator(ComplaintEvidenceRepository repository, EvidenceBinaryStorage storage,
            Clock clock) {
        this.repository = repository;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional
    public EvidenceUploadResult commit(Long actorId, Long complaintId, StagedBinary staged, LoaiTepMinhChung category,
            EvidenceInspection inspection,
            String originalFilename, String description) {
        KhieuNai complaint = repository.lockOwnedComplaint(actorId, complaintId);
        if (complaint.getTrangThaiKhieuNai() != TrangThaiKhieuNai.SUBMITTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "COMPLAINT_NOT_ACCEPTING_EVIDENCE",
                    "Khiếu nại hiện không nhận thêm bằng chứng.");
        }
        var existing = repository.findExisting(complaintId, actorId, staged.sha256Hex());
        if (existing.isPresent()) {
            verifyExisting(existing.get());
            return new EvidenceUploadResult(existing.get(), false);
        }

        String storageKey = deterministicStorageKey(complaintId, actorId, staged.sha256Hex());
        PromotionResult promotion;
        try {
            promotion = storage.promote(staged, storageKey);
        } catch (IOException ex) {
            throw storageUnavailable();
        }
        if (!promotion.createdNew()) {
            try {
                storage.verify(storageKey, staged.sizeBytes(), staged.sha256Hex());
            } catch (IOException ex) {
                throw storageIntegrity();
            }
        }
        if (promotion.createdNew())
            registerRollbackCleanup(storageKey);

        EvidenceCommitCommand command = new EvidenceCommitCommand(actorId, complaintId, category, originalFilename,
                inspection.verifiedMediaType(), staged.sizeBytes(), staged.sha256Hex(), storageKey, description,
                TimePolicy.now(clock));
        EvidenceMetadataRow persisted = repository.persist(command, complaint);
        return new EvidenceUploadResult(persisted, true);
    }

    private void verifyExisting(EvidenceMetadataRow row) {
        try {
            storage.verify(row.storageKey(), row.sizeBytes(), row.fingerprint());
        } catch (IOException ex) {
            throw storageIntegrity();
        }
    }

    private void registerRollbackCleanup(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive())
            return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED)
                    storage.deleteFinal(storageKey);
            }
        });
    }

    static String deterministicStorageKey(Long complaintId, Long actorId, String contentHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String seed = complaintId + ":" + actorId + ":" + contentHash;
            String objectHash = HexFormat.of().formatHex(digest.digest(seed.getBytes(StandardCharsets.UTF_8)));
            return objectHash.substring(0, 2) + "/" + objectHash;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng.", ex);
        }
    }

    private static BusinessException storageUnavailable() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "EVIDENCE_STORAGE_UNAVAILABLE",
                "Kho bằng chứng hiện không khả dụng.");
    }

    private static BusinessException storageIntegrity() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "EVIDENCE_STORAGE_INTEGRITY_VIOLATION",
                "Bằng chứng lưu trữ không còn nhất quán.");
    }
}
