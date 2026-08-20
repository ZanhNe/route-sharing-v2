package com.zanh.route_sharing.repository.complaint.evidence.jpa;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.complaint.evidence.ComplaintEvidenceRepository;
import com.zanh.route_sharing.repository.complaint.evidence.model.*;
import jakarta.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class JpaComplaintEvidenceRepository implements ComplaintEvidenceRepository {
    private final EntityManager entityManager;
    public JpaComplaintEvidenceRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override
    public KhieuNai lockOwnedComplaint(Long actorId, Long complaintId) {
        try {
            List<KhieuNai> rows = entityManager.createQuery(
                    "select c from KhieuNai c join fetch c.nguoiKhieuNai owner where c.id=:id", KhieuNai.class)
                    .setParameter("id", complaintId).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(2).getResultList();
            if (rows.size() != 1 || rows.get(0).getNguoiKhieuNai() == null
                    || !Objects.equals(rows.get(0).getNguoiKhieuNai().getId(), actorId)) throw contextNotFound();
            return rows.get(0);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
    }

    @Override
    public Optional<EvidenceMetadataRow> findExisting(Long complaintId, Long uploaderId, String sha256Hex) {
        List<TepMinhChung> rows = entityManager.createQuery(
                "select e from TepMinhChung e join fetch e.khieuNai c join fetch e.nguoiTaiLen u "
                + "where c.id=:complaintId and u.id=:uploaderId and e.fileHash=:hash", TepMinhChung.class)
                .setParameter("complaintId", complaintId).setParameter("uploaderId", uploaderId)
                .setParameter("hash", sha256Hex).setMaxResults(2).getResultList();
        if (rows.size() > 1) throw invariant();
        return rows.stream().findFirst().map(JpaComplaintEvidenceRepository::row);
    }

    @Override
    public EvidenceMetadataRow persist(EvidenceCommitCommand command, KhieuNai complaint) {
        try {
            NguoiDung uploader = entityManager.find(NguoiDung.class, command.actorId());
            if (uploader == null) throw contextNotFound();
            TepMinhChung evidence = TepMinhChung.attachToComplaint(complaint, uploader, command.category(),
                    command.originalFilename(), command.verifiedMediaType(), command.sizeBytes(), command.sha256Hex(),
                    command.storageKey(), command.description(), command.uploadedAt());
            entityManager.persist(evidence);
            entityManager.flush();
            return row(evidence);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (PersistenceException ex) { throw dataIntegrity(); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw invariant(); }
    }

    @Override
    @Transactional(readOnly = true)
    public EvidencePageSnapshot findOwnPage(Long actorId, Long complaintId, int page, int size) {
        requireOwnComplaintRead(actorId, complaintId);
        long total = entityManager.createQuery(
                "select count(e) from TepMinhChung e where e.khieuNai.id=:complaintId and e.nguoiTaiLen.id=:actorId", Long.class)
                .setParameter("complaintId", complaintId).setParameter("actorId", actorId).getSingleResult();
        List<TepMinhChung> rows = entityManager.createQuery(
                "select e from TepMinhChung e join fetch e.khieuNai c join fetch e.nguoiTaiLen u "
                + "where c.id=:complaintId and u.id=:actorId order by e.taiLenLuc desc, e.id desc", TepMinhChung.class)
                .setParameter("complaintId", complaintId).setParameter("actorId", actorId)
                .setFirstResult(page * size).setMaxResults(size).getResultList();
        return new EvidencePageSnapshot(rows.stream().map(JpaComplaintEvidenceRepository::row).toList(), page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvidenceMetadataRow> findOwnEvidence(Long actorId, Long complaintId, Long evidenceId) {
        if (!ownsComplaint(actorId, complaintId)) throw contextNotFound();
        List<TepMinhChung> rows = entityManager.createQuery(
                "select e from TepMinhChung e join fetch e.khieuNai c join fetch e.nguoiTaiLen u "
                + "where e.id=:evidenceId and c.id=:complaintId and u.id=:actorId", TepMinhChung.class)
                .setParameter("evidenceId", evidenceId).setParameter("complaintId", complaintId)
                .setParameter("actorId", actorId).setMaxResults(2).getResultList();
        if (rows.size() > 1) throw invariant();
        return rows.stream().findFirst().map(JpaComplaintEvidenceRepository::row);
    }

    private void requireOwnComplaintRead(Long actorId, Long complaintId) { if (!ownsComplaint(actorId, complaintId)) throw contextNotFound(); }
    private boolean ownsComplaint(Long actorId, Long complaintId) {
        Long count = entityManager.createQuery(
                "select count(c) from KhieuNai c where c.id=:id and c.nguoiKhieuNai.id=:actor", Long.class)
                .setParameter("id", complaintId).setParameter("actor", actorId).getSingleResult();
        return count == 1L;
    }

    private static EvidenceMetadataRow row(TepMinhChung e) {
        if (e.getId() == null || e.getKhieuNai() == null || e.getKhieuNai().getId() == null || e.getNguoiTaiLen() == null
                || e.getNguoiTaiLen().getId() == null || e.getLoaiTep() == null || e.getTaiLenLuc() == null
                || e.getOriginalFilename() == null || e.getVerifiedMediaType() == null || e.getFileHash() == null
                || e.getStorageKey() == null || e.getSizeBytes() <= 0) throw invariant();
        return new EvidenceMetadataRow(e.getId(), e.getKhieuNai().getId(), e.getNguoiTaiLen().getId(), e.getLoaiTep(),
                e.getOriginalFilename(), e.getVerifiedMediaType(), e.getSizeBytes(), e.getFileHash(), e.getStorageKey(),
                e.getTaiLenLuc(), e.getMoTa());
    }

    private static BusinessException contextNotFound() { return new BusinessException(HttpStatus.NOT_FOUND, "EVIDENCE_CONTEXT_NOT_FOUND", "Không tìm thấy ngữ cảnh bằng chứng phù hợp."); }
    private static BusinessException concurrent() { return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Khiếu nại vừa được thay đổi đồng thời. Vui lòng thử lại."); }
    private static BusinessException dataIntegrity() { return new BusinessException(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Bằng chứng xung đột với ràng buộc dữ liệu."); }
    private static BusinessException invariant() { return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "EVIDENCE_CONTEXT_INVARIANT_VIOLATION", "Dữ liệu bằng chứng không nhất quán."); }
}
