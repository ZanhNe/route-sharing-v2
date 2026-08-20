package com.zanh.route_sharing.repository.complaint.review.jpa;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.*;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.complaint.review.ComplaintReviewRepository;
import com.zanh.route_sharing.repository.complaint.review.model.ComplaintReviewSnapshots;
import jakarta.persistence.*;
import org.hibernate.exception.ConstraintViolationException;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Repository
public class JpaComplaintReviewRepository implements ComplaintReviewRepository {
    private final EntityManager entityManager;
    private final ComplaintStaffScopeJpaSupport staffScope;

    public JpaComplaintReviewRepository(EntityManager entityManager, ComplaintStaffScopeJpaSupport staffScope) {
        this.entityManager = entityManager;
        this.staffScope = staffScope;
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintReviewSnapshots.Page<ComplaintReviewSnapshots.QueueItem> findQueue(Long actorId, String status,
            int page, int size, LocalDate date) {
        TrangThaiKhieuNai state = parseActiveState(status);
        requirePositive(actorId, "actorId");
        List<Long> schoolIds = staffScope.findActiveSchoolIds(actorId, date);
        if (schoolIds.isEmpty() || !staffScope.hasEffectivePermission(actorId, ComplaintStaffScopeJpaSupport.HANDLE_COMPLAINT)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Bạn không có phạm vi xử lý khiếu nại hiện hành.");
        }
        String order = state == TrangThaiKhieuNai.SUBMITTED
                ? " order by c.nopLuc asc, c.id asc"
                : " order by coalesce(c.giaiQuyetLuc,c.yeuCauBoSungLuc,c.tiepNhanLuc,c.nopLuc) desc, c.id desc";
        String base = " from KhieuNai c join fetch c.chuyenDi t join fetch c.yeuCauDiChung b "
                + "join fetch b.cauHinhLucGui cfg join fetch cfg.nhaTruong school "
                + "join fetch c.nguoiKhieuNai complainant join fetch c.nguoiBiKhieuNai respondent "
                + "left join fetch c.nguoiTiepNhan reviewer where c.trangThaiKhieuNai=:state and school.id in :schools";
        List<KhieuNai> rows = entityManager.createQuery("select c" + base + order, KhieuNai.class)
                .setParameter("state", state).setParameter("schools", schoolIds)
                .setFirstResult(page * size).setMaxResults(size).getResultList();
        Long total = entityManager.createQuery(
                "select count(c) from KhieuNai c join c.yeuCauDiChung b join b.cauHinhLucGui cfg "
                        + "where c.trangThaiKhieuNai=:state and cfg.nhaTruong.id in :schools", Long.class)
                .setParameter("state", state).setParameter("schools", schoolIds).getSingleResult();
        return new ComplaintReviewSnapshots.Page<>(rows.stream().map(this::queueItem).toList(), page, size, total == null ? 0 : total);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintReviewSnapshots.ReviewCase findReviewerCase(Long actorId, Long complaintId, LocalDate date) {
        KhieuNai complaint = findComplaint(complaintId, false);
        Long schoolId = staffScope.resolveComplaintSchoolId(complaintId);
        staffScope.requireHandler(actorId, schoolId, date);
        return reviewCase(complaint);
    }

    @Override
    @Transactional
    public ComplaintReviewSnapshots.Action claim(Long actorId, Long complaintId, Instant now, LocalDate date) {
        try {
            KhieuNai c = findComplaint(complaintId, true);
            Long schoolId = staffScope.resolveComplaintSchoolId(complaintId);
            staffScope.requireHandler(actorId, schoolId, date);
            NguoiDung actor = staffScope.requireUser(actorId);
            if (c.getTrangThaiKhieuNai() == TrangThaiKhieuNai.UNDER_REVIEW
                    || c.getTrangThaiKhieuNai() == TrangThaiKhieuNai.NEED_MORE_EVIDENCE) {
                if (c.isCurrentReviewer(actorId)) return action(c, false);
                throw conflict("COMPLAINT_ALREADY_ASSIGNED", "Complaint đã được reviewer khác tiếp nhận.");
            }
            if (c.isTerminalReviewState()) throw conflict("COMPLAINT_NOT_CLAIMABLE", "Complaint đã có kết quả review.");
            if (c.getTrangThaiKhieuNai() != TrangThaiKhieuNai.SUBMITTED) {
                throw conflict("COMPLAINT_NOT_CLAIMABLE", "Complaint không ở trạng thái có thể tiếp nhận.");
            }
            Long hours = currentResponseWindowHours(schoolId);
            TrangThaiKhieuNai previous = c.getTrangThaiKhieuNai();
            c.claimReview(actor, hours, now);
            appendHistory(c, LoaiThaoTacXuLyKhieuNai.CLAIMED, previous, c.getTrangThaiKhieuNai(), null, actor,
                    actor, null, now, c.getHanPhanHoiBanDauLuc(), null, null);
            entityManager.persist(ThongBao.complaintReviewStarted(c, c.getNguoiKhieuNai()));
            entityManager.persist(ThongBao.complaintReviewStarted(c, c.getNguoiBiKhieuNai()));
            entityManager.flush();
            return action(c, true);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (ConstraintViolationException ex) { throw conflict("DATA_INTEGRITY_VIOLATION", "Xung đột dữ liệu complaint review."); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw mapDomain(ex); }
        catch (PersistenceException ex) { throw ComplaintStaffScopeJpaSupport.invariant(); }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintReviewSnapshots.Page<ComplaintReviewSnapshots.EligibleReviewer> findEligibleReviewers(Long actorId,
            Long complaintId, int page, int size, LocalDate date) {
        KhieuNai complaint = findComplaint(complaintId, false);
        Long schoolId = staffScope.resolveComplaintSchoolId(complaint.getId());
        staffScope.requireReassigner(actorId, schoolId, date);
        List<Long> ids = staffScope.findEligibleReviewerIds(schoolId, date);
        int from = Math.min(page * size, ids.size());
        int to = Math.min(from + size, ids.size());
        List<ComplaintReviewSnapshots.EligibleReviewer> items = ids.subList(from, to).stream().map(id -> {
            NguoiDung u = staffScope.requireUser(id);
            return new ComplaintReviewSnapshots.EligibleReviewer(id, u.getHoTen());
        }).toList();
        return new ComplaintReviewSnapshots.Page<>(items, page, size, ids.size());
    }

    @Override
    @Transactional
    public ComplaintReviewSnapshots.Action reassign(Long actorId, Long complaintId, Long newReviewerId,
            String reason, Instant now, LocalDate date) {
        try {
            String normalized = normalize(reason, 10, 1000, "reason");
            KhieuNai c = findComplaint(complaintId, true);
            Long schoolId = staffScope.resolveComplaintSchoolId(complaintId);
            staffScope.requireReassigner(actorId, schoolId, date);
            NguoiDung actor = staffScope.requireUser(actorId);
            if (c.getTrangThaiKhieuNai() != TrangThaiKhieuNai.UNDER_REVIEW
                    && c.getTrangThaiKhieuNai() != TrangThaiKhieuNai.NEED_MORE_EVIDENCE) {
                throw conflict("COMPLAINT_NOT_REASSIGNABLE", "Complaint không ở trạng thái có thể chuyển reviewer.");
            }
            if (Objects.equals(c.getNguoiTiepNhan() == null ? null : c.getNguoiTiepNhan().getId(), newReviewerId)) {
                return action(c, false);
            }
            if (!staffScope.hasActiveStaffScope(newReviewerId, schoolId, date)
                    || !staffScope.hasEffectivePermission(newReviewerId, ComplaintStaffScopeJpaSupport.HANDLE_COMPLAINT)) {
                throw conflict("COMPLAINT_NOT_REASSIGNABLE", "Reviewer mới hiện không đủ điều kiện xử lý complaint.");
            }
            NguoiDung previousReviewer = c.getNguoiTiepNhan();
            NguoiDung newReviewer = staffScope.requireUser(newReviewerId);
            TrangThaiKhieuNai state = c.getTrangThaiKhieuNai();
            c.reassignReviewer(newReviewer);
            appendHistory(c, LoaiThaoTacXuLyKhieuNai.REASSIGNED, state, state, previousReviewer, newReviewer,
                    actor, null, now, null, null, normalized);
            entityManager.flush();
            return action(c, true);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw mapDomain(ex); }
        catch (PersistenceException ex) { throw ComplaintStaffScopeJpaSupport.invariant(); }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintReviewSnapshots.ParticipantView findParticipantView(Long actorId, Long complaintId) {
        KhieuNai c = findComplaint(complaintId, false);
        requireParticipantVisible(c, actorId, true);
        PhanHoiKhieuNai response = findResponse(c.getId());
        long count = countEvidence(c.getId(), actorId);
        String role = participantRole(c, actorId);
        boolean requestForActor = c.getNguoiDuocYeuCauBoSung() != null
                && Objects.equals(c.getNguoiDuocYeuCauBoSung().getId(), actorId);
        return new ComplaintReviewSnapshots.ParticipantView(c.getId(), c.getTrangThaiKhieuNai().name(), c.getTieuDe(), c.getNoiDung(),
                c.getNopLuc(), c.getChuyenDi().getId(), c.getYeuCauDiChung().getId(), role, c.getTiepNhanLuc(),
                c.getHanPhanHoiBanDauLuc(), requestForActor ? actorId : null,
                requestForActor ? c.getLyDoYeuCauBoSung() : null,
                requestForActor ? c.getHanBoSungLuc() : null,
                c.isTerminalReviewState() ? c.getKetLuan() : null,
                c.isTerminalReviewState() ? c.getGiaiQuyetLuc() : null,
                "RESPONDENT".equals(role) && response != null ? response.getNoiDung() : null,
                "RESPONDENT".equals(role) && response != null ? response.getPhanHoiLuc() : null, count);
    }

    @Override
    @Transactional
    public ComplaintReviewSnapshots.FormalResponse submitResponse(Long actorId, Long complaintId, String content, Instant now) {
        try {
            String normalized = normalize(content, 20, 5000, "content");
            KhieuNai c = findComplaint(complaintId, true);
            requireParticipantVisible(c, actorId, false);
            if (!Objects.equals(c.getNguoiBiKhieuNai().getId(), actorId)) throw ComplaintStaffScopeJpaSupport.contextNotFound();
            if (c.getTrangThaiKhieuNai() != TrangThaiKhieuNai.UNDER_REVIEW) {
                throw conflict("COMPLAINT_RESPONSE_WINDOW_CLOSED", "Complaint hiện không nhận phản hồi chính thức.");
            }
            if (c.getHanPhanHoiBanDauLuc() == null || now.isAfter(c.getHanPhanHoiBanDauLuc())) {
                throw conflict("COMPLAINT_RESPONSE_WINDOW_CLOSED", "Đã hết thời hạn phản hồi complaint.");
            }
            PhanHoiKhieuNai existing = findResponse(c.getId());
            if (existing != null) {
                if (Objects.equals(existing.getNoiDung(), normalized)) {
                    return new ComplaintReviewSnapshots.FormalResponse(c.getId(), existing.getId(), existing.getNoiDung(), existing.getPhanHoiLuc(), false);
                }
                throw conflict("COMPLAINT_RESPONSE_ALREADY_SUBMITTED", "Respondent đã gửi phản hồi chính thức trước đó.");
            }
            NguoiDung respondent = staffScope.requireUser(actorId);
            PhanHoiKhieuNai response = PhanHoiKhieuNai.submit(c, respondent, normalized, now);
            entityManager.persist(response);
            entityManager.flush();
            return new ComplaintReviewSnapshots.FormalResponse(c.getId(), response.getId(), response.getNoiDung(), response.getPhanHoiLuc(), true);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (ConstraintViolationException ex) { throw conflict("DATA_INTEGRITY_VIOLATION", "Xung đột phản hồi complaint."); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw mapDomain(ex); }
        catch (PersistenceException ex) { throw ComplaintStaffScopeJpaSupport.invariant(); }
    }

    @Override
    @Transactional
    public ComplaintReviewSnapshots.Action requestMoreEvidence(Long actorId, Long complaintId, Long targetParticipantId,
            String reason, Instant now, LocalDate date) {
        try {
            String normalized = normalize(reason, 10, 1000, "reason");
            KhieuNai c = findComplaint(complaintId, true);
            Long schoolId = staffScope.resolveComplaintSchoolId(complaintId);
            staffScope.requireHandler(actorId, schoolId, date);
            requireCurrentReviewer(c, actorId);
            if (c.getTrangThaiKhieuNai() == TrangThaiKhieuNai.NEED_MORE_EVIDENCE) {
                if (c.getNguoiDuocYeuCauBoSung() != null
                        && Objects.equals(c.getNguoiDuocYeuCauBoSung().getId(), targetParticipantId)
                        && Objects.equals(c.getLyDoYeuCauBoSung(), normalized)) return action(c, false);
                throw conflict("COMPLAINT_EVIDENCE_REQUEST_ALREADY_OPEN", "Complaint đã có evidence request đang mở.");
            }
            if (c.getTrangThaiKhieuNai() != TrangThaiKhieuNai.UNDER_REVIEW) {
                throw conflict("COMPLAINT_NOT_FINALIZABLE", "Complaint không ở UNDER_REVIEW.");
            }
            if (c.getHanPhanHoiBanDauLuc() == null || now.isBefore(c.getHanPhanHoiBanDauLuc())) {
                throw conflict("COMPLAINT_RESPONSE_WINDOW_OPEN", "Response window ban đầu vẫn đang mở.");
            }
            if (!c.isParticipant(targetParticipantId)) throw validation("targetParticipantId không thuộc complaint.");
            NguoiDung reviewer = staffScope.requireUser(actorId);
            NguoiDung target = staffScope.requireUser(targetParticipantId);
            long watermark = maxEvidenceIdForParticipant(c.getId(), targetParticipantId);
            TrangThaiKhieuNai previous = c.getTrangThaiKhieuNai();
            c.requestMoreEvidence(reviewer, target, normalized, watermark, now);
            long seq = appendHistory(c, LoaiThaoTacXuLyKhieuNai.MORE_EVIDENCE_REQUESTED, previous,
                    c.getTrangThaiKhieuNai(), reviewer, reviewer, reviewer, target, now, c.getHanBoSungLuc(), watermark, normalized);
            entityManager.persist(ThongBao.complaintMoreEvidenceRequested(c, target, seq));
            entityManager.flush();
            return action(c, true);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw mapDomain(ex); }
        catch (PersistenceException ex) { throw ComplaintStaffScopeJpaSupport.invariant(); }
    }

    @Override
    @Transactional
    public ComplaintReviewSnapshots.Action resume(Long actorId, Long complaintId, Instant now, LocalDate date) {
        try {
            KhieuNai c = findComplaint(complaintId, true);
            Long schoolId = staffScope.resolveComplaintSchoolId(complaintId);
            staffScope.requireHandler(actorId, schoolId, date);
            requireCurrentReviewer(c, actorId);
            if (c.getTrangThaiKhieuNai() != TrangThaiKhieuNai.NEED_MORE_EVIDENCE) {
                throw conflict("COMPLAINT_EVIDENCE_REQUEST_NOT_OPEN", "Complaint không có evidence request đang mở.");
            }
            Long targetId = c.getNguoiDuocYeuCauBoSung() == null ? null : c.getNguoiDuocYeuCauBoSung().getId();
            long currentMax = targetId == null ? 0 : maxEvidenceIdForParticipant(c.getId(), targetId);
            boolean expired = c.getHanBoSungLuc() != null && !now.isBefore(c.getHanBoSungLuc());
            boolean satisfied = c.getMocIdMinhChungLucYeuCau() != null && currentMax > c.getMocIdMinhChungLucYeuCau();
            if (!expired && !satisfied) {
                throw conflict("COMPLAINT_EVIDENCE_REQUEST_NOT_SATISFIED", "Chưa hết hạn và chưa có evidence mới theo yêu cầu.");
            }
            NguoiDung reviewer = staffScope.requireUser(actorId);
            TrangThaiKhieuNai previous = c.getTrangThaiKhieuNai();
            c.resumeReview(reviewer);
            appendHistory(c, LoaiThaoTacXuLyKhieuNai.REVIEW_RESUMED, previous, c.getTrangThaiKhieuNai(), reviewer,
                    reviewer, reviewer, null, now, null, null, null);
            entityManager.flush();
            return action(c, true);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw mapDomain(ex); }
        catch (PersistenceException ex) { throw ComplaintStaffScopeJpaSupport.invariant(); }
    }

    @Override
    @Transactional
    public ComplaintReviewSnapshots.Action finalizeReview(Long actorId, Long complaintId, String outcome,
            String conclusion, Instant now, LocalDate date) {
        try {
            TrangThaiKhieuNai terminal = parseTerminal(outcome);
            String normalized = normalize(conclusion, 1, 5000, "conclusion");
            KhieuNai c = findComplaint(complaintId, true);
            Long schoolId = staffScope.resolveComplaintSchoolId(complaintId);
            staffScope.requireHandler(actorId, schoolId, date);
            requireCurrentReviewer(c, actorId);
            if (c.isTerminalReviewState()) {
                if (c.getTrangThaiKhieuNai() == terminal && Objects.equals(c.getKetLuan(), normalized)) return action(c, false);
                throw conflict("COMPLAINT_ALREADY_FINALIZED", "Complaint đã có kết quả review cuối khác.");
            }
            if (c.getTrangThaiKhieuNai() != TrangThaiKhieuNai.UNDER_REVIEW) {
                throw conflict("COMPLAINT_NOT_FINALIZABLE", "Complaint chưa ở trạng thái có thể kết luận.");
            }
            if (c.getHanPhanHoiBanDauLuc() == null || now.isBefore(c.getHanPhanHoiBanDauLuc())) {
                throw conflict("COMPLAINT_RESPONSE_WINDOW_OPEN", "Response window ban đầu vẫn đang mở.");
            }
            NguoiDung reviewer = staffScope.requireUser(actorId);
            TrangThaiKhieuNai previous = c.getTrangThaiKhieuNai();
            c.finalizeReview(reviewer, terminal, normalized, now);
            LoaiThaoTacXuLyKhieuNai action = terminal == TrangThaiKhieuNai.ACCEPTED
                    ? LoaiThaoTacXuLyKhieuNai.ACCEPTED : LoaiThaoTacXuLyKhieuNai.REJECTED;
            appendHistory(c, action, previous, terminal, reviewer, reviewer, reviewer, null, now, null, null, null);
            entityManager.persist(ThongBao.complaintReviewFinalized(c, c.getNguoiKhieuNai()));
            entityManager.persist(ThongBao.complaintReviewFinalized(c, c.getNguoiBiKhieuNai()));
            entityManager.flush();
            return action(c, true);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw mapDomain(ex); }
        catch (PersistenceException ex) { throw ComplaintStaffScopeJpaSupport.invariant(); }
    }

    @Override
    public KhieuNai lockParticipantReviewComplaint(Long actorId, Long complaintId, Instant now) {
        KhieuNai c = findComplaint(complaintId, true);
        requireParticipantVisible(c, actorId, false);
        if (c.getTrangThaiKhieuNai() == TrangThaiKhieuNai.UNDER_REVIEW) {
            if (c.getHanPhanHoiBanDauLuc() == null || now.isAfter(c.getHanPhanHoiBanDauLuc())) {
                throw conflict("COMPLAINT_RESPONSE_WINDOW_CLOSED", "Đã hết review response window.");
            }
            return c;
        }
        if (c.getTrangThaiKhieuNai() == TrangThaiKhieuNai.NEED_MORE_EVIDENCE) {
            if (c.getNguoiDuocYeuCauBoSung() == null || !Objects.equals(c.getNguoiDuocYeuCauBoSung().getId(), actorId)) {
                throw conflict("COMPLAINT_NOT_ACCEPTING_REVIEW_EVIDENCE", "Actor không phải participant đang được yêu cầu bổ sung.");
            }
            if (c.getHanBoSungLuc() == null || now.isAfter(c.getHanBoSungLuc())) {
                throw conflict("COMPLAINT_RESPONSE_WINDOW_CLOSED", "Đã hết thời hạn bổ sung evidence.");
            }
            return c;
        }
        throw conflict("COMPLAINT_NOT_ACCEPTING_REVIEW_EVIDENCE", "Complaint hiện không nhận review evidence.");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplaintReviewSnapshots.Evidence> findExistingEvidence(Long complaintId, Long uploaderId, String fingerprint) {
        return entityManager.createQuery(
                "select e from TepMinhChung e join fetch e.nguoiTaiLen u join fetch e.khieuNai c "
                        + "where c.id=:cid and u.id=:uid and e.fileHash=:hash", TepMinhChung.class)
                .setParameter("cid", complaintId).setParameter("uid", uploaderId).setParameter("hash", fingerprint)
                .setMaxResults(1).getResultList().stream().findFirst().map(this::evidence);
    }

    @Override
    public ComplaintReviewSnapshots.Evidence persistReviewEvidence(KhieuNai complaint, Long uploaderId, String category,
            String originalFilename, String mediaType, long sizeBytes, String fingerprint, String storageKey,
            String description, Instant uploadedAt) {
        try {
            NguoiDung uploader = staffScope.requireUser(uploaderId);
            TepMinhChung entity = TepMinhChung.attachToComplaintReview(complaint, uploader, LoaiTepMinhChung.valueOf(category),
                    originalFilename, mediaType, sizeBytes, fingerprint, storageKey, description, uploadedAt);
            entityManager.persist(entity);
            entityManager.flush();
            return evidence(entity);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (ConstraintViolationException ex) { throw conflict("DATA_INTEGRITY_VIOLATION", "Xung đột review evidence."); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw ComplaintStaffScopeJpaSupport.invariant(); }
        catch (PersistenceException ex) { throw ComplaintStaffScopeJpaSupport.invariant(); }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintReviewSnapshots.Page<ComplaintReviewSnapshots.Evidence> findParticipantEvidence(Long actorId,
            Long complaintId, int page, int size) {
        KhieuNai c = findComplaint(complaintId, false);
        requireParticipantVisible(c, actorId, false);
        List<TepMinhChung> rows = entityManager.createQuery(
                "select e from TepMinhChung e join fetch e.nguoiTaiLen u where e.khieuNai.id=:cid and u.id=:uid "
                        + "order by e.taiLenLuc desc,e.id desc", TepMinhChung.class)
                .setParameter("cid", complaintId).setParameter("uid", actorId)
                .setFirstResult(page * size).setMaxResults(size).getResultList();
        long total = countEvidence(complaintId, actorId);
        return new ComplaintReviewSnapshots.Page<>(rows.stream().map(this::evidence).toList(), page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplaintReviewSnapshots.Evidence> findParticipantEvidence(Long actorId, Long complaintId, Long evidenceId) {
        KhieuNai c = findComplaint(complaintId, false);
        requireParticipantVisible(c, actorId, false);
        return entityManager.createQuery(
                "select e from TepMinhChung e join fetch e.nguoiTaiLen u where e.id=:eid and e.khieuNai.id=:cid and u.id=:uid",
                TepMinhChung.class).setParameter("eid", evidenceId).setParameter("cid", complaintId).setParameter("uid", actorId)
                .setMaxResults(1).getResultList().stream().findFirst().map(this::evidence);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintReviewSnapshots.Page<ComplaintReviewSnapshots.Evidence> findReviewerEvidence(Long actorId,
            Long complaintId, int page, int size, LocalDate date) {
        KhieuNai c = requireCurrentReviewerSensitive(actorId, complaintId, date, false);
        List<TepMinhChung> rows = entityManager.createQuery(
                "select e from TepMinhChung e join fetch e.nguoiTaiLen u where e.khieuNai.id=:cid order by e.taiLenLuc desc,e.id desc",
                TepMinhChung.class).setParameter("cid", c.getId()).setFirstResult(page * size).setMaxResults(size).getResultList();
        Long total = entityManager.createQuery("select count(e) from TepMinhChung e where e.khieuNai.id=:cid", Long.class)
                .setParameter("cid", c.getId()).getSingleResult();
        return new ComplaintReviewSnapshots.Page<>(rows.stream().map(this::evidence).toList(), page, size, total == null ? 0 : total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplaintReviewSnapshots.Evidence> findReviewerEvidence(Long actorId, Long complaintId,
            Long evidenceId, LocalDate date) {
        KhieuNai c = requireCurrentReviewerSensitive(actorId, complaintId, date, false);
        return entityManager.createQuery(
                "select e from TepMinhChung e join fetch e.nguoiTaiLen u where e.id=:eid and e.khieuNai.id=:cid", TepMinhChung.class)
                .setParameter("eid", evidenceId).setParameter("cid", c.getId()).setMaxResults(1)
                .getResultList().stream().findFirst().map(this::evidence);
    }

    @Override
    @Transactional(readOnly = true)
    public long maxEvidenceIdForParticipant(Long complaintId, Long participantId) {
        Long max = entityManager.createQuery(
                "select coalesce(max(e.id),0) from TepMinhChung e where e.khieuNai.id=:cid and e.nguoiTaiLen.id=:uid", Long.class)
                .setParameter("cid", complaintId).setParameter("uid", participantId).getSingleResult();
        return max == null ? 0L : max;
    }

    @Override
    @Transactional
    public ComplaintReviewSnapshots.Investigation findInvestigationContext(Long actorId, Long complaintId, LocalDate date,
            String ip, String userAgent, Instant now) {
        KhieuNai c = requireCurrentReviewerSensitive(actorId, complaintId, date, false);
        ChuyenDi trip = c.getChuyenDi();
        YeuCauDiChung booking = c.getYeuCauDiChung();
        Long stopCount = entityManager.createQuery("select count(s) from DiemDungHanhTrinh s where s.chuyenDi.id=:tid", Long.class)
                .setParameter("tid", trip.getId()).getSingleResult();
        SuCoChuyenDi incident = c.getSuCoChuyenDi();
        if (incident != null) recordSensitiveReadInternal(actorId, incident.getId(), LoaiTaiNguyenNhayCam.SU_CO_CHUYEN_DI,
                "Xử lý khiếu nại: đối chiếu incident liên kết", ip, userAgent, now);
        Long driverId = trip.getLoTrinhChiaSe() == null || trip.getLoTrinhChiaSe().getTaiXe() == null ? null : trip.getLoTrinhChiaSe().getTaiXe().getId();
        Long passengerId = booking.getHanhKhach() == null ? null : booking.getHanhKhach().getId();
        return new ComplaintReviewSnapshots.Investigation(c.getId(), trip.getId(), trip.getTrangThaiVanHanh().name(),
                booking.getId(), booking.getTrangThaiYeuCau().name(), driverId, passengerId, stopCount == null ? 0 : stopCount,
                incident == null ? null : incident.getId(), incident == null ? null : incident.getLoaiSuCo().name(),
                incident == null ? null : incident.getTrangThaiXuLy().name(), trip.getKetThucLuc());
    }

    @Override
    @Transactional
    public ComplaintReviewSnapshots.Page<ComplaintReviewSnapshots.Location> findLocationEvidence(Long actorId,
            Long complaintId, int page, int size, LocalDate date, String ip, String userAgent, Instant now) {
        KhieuNai c = requireCurrentReviewerSensitive(actorId, complaintId, date, false);
        Long tripId = c.getChuyenDi().getId();
        List<BanGhiDinhVi> rows = entityManager.createQuery(
                "select l from BanGhiDinhVi l where l.chuyenDi.id=:tid order by l.thuTuBanGhi asc", BanGhiDinhVi.class)
                .setParameter("tid", tripId).setFirstResult(page * size).setMaxResults(size).getResultList();
        Long total = entityManager.createQuery("select count(l) from BanGhiDinhVi l where l.chuyenDi.id=:tid", Long.class)
                .setParameter("tid", tripId).getSingleResult();
        recordSensitiveReadInternal(actorId, tripId, LoaiTaiNguyenNhayCam.DU_LIEU_DINH_VI,
                "Xử lý khiếu nại: đối chiếu lịch sử vị trí Trip", ip, userAgent, now);
        return new ComplaintReviewSnapshots.Page<>(rows.stream().map(this::location).toList(), page, size, total == null ? 0 : total);
    }

    @Override
    @Transactional
    public void recordSensitiveRead(Long actorId, Long complaintId, Long resourceId, LoaiTaiNguyenNhayCam resourceType,
            String purpose, String ip, String userAgent, Instant now, LocalDate date) {
        requireCurrentReviewerSensitive(actorId, complaintId, date, false);
        recordSensitiveReadInternal(actorId, resourceId, resourceType, purpose, ip, userAgent, now);
    }

    private KhieuNai requireCurrentReviewerSensitive(Long actorId, Long complaintId, LocalDate date, boolean lock) {
        KhieuNai c = findComplaint(complaintId, lock);
        Long schoolId = staffScope.resolveComplaintSchoolId(complaintId);
        staffScope.requireSensitiveViewer(actorId, schoolId, date);
        requireCurrentReviewer(c, actorId);
        return c;
    }

    private void recordSensitiveReadInternal(Long actorId, Long resourceId, LoaiTaiNguyenNhayCam resourceType,
            String purpose, String ip, String userAgent, Instant now) {
        NguoiDung actor = staffScope.requireUser(actorId);
        entityManager.persist(NhatKyTruyCapDuLieuNhayCam.builder().loaiTaiNguyen(resourceType).taiNguyenId(resourceId)
                .hanhDong(HanhDongTruyCap.VIEW).mucDich(normalizeOptional(purpose, 1000))
                .truyCapLuc(now).diaChiIp(normalizeClient(ip, 64, "unknown"))
                .thongTinTrinhDuyet(normalizeClient(userAgent, 1000, "unknown")).nguoiTruyCap(actor).build());
    }

    private KhieuNai findComplaint(Long complaintId, boolean lock) {
        requirePositive(complaintId, "complaintId");
        TypedQuery<KhieuNai> query = entityManager.createQuery(
                "select c from KhieuNai c join fetch c.chuyenDi trip join fetch trip.loTrinhChiaSe route "
                        + "join fetch route.taiXe driver join fetch c.yeuCauDiChung booking join fetch booking.hanhKhach passenger "
                        + "join fetch booking.cauHinhLucGui cfg join fetch cfg.nhaTruong school "
                        + "join fetch c.nguoiKhieuNai complainant join fetch c.nguoiBiKhieuNai respondent "
                        + "left join fetch c.nguoiTiepNhan reviewer left join fetch c.nguoiDuocYeuCauBoSung requested "
                        + "left join fetch c.suCoChuyenDi incident where c.id=:id", KhieuNai.class)
                .setParameter("id", complaintId).setMaxResults(1);
        if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultList().stream().findFirst().orElseThrow(ComplaintStaffScopeJpaSupport::contextNotFound);
    }

    private void requireParticipantVisible(KhieuNai c, Long actorId, boolean complainantCanSeeSubmitted) {
        requirePositive(actorId, "actorId");
        if (!c.isParticipant(actorId)) throw ComplaintStaffScopeJpaSupport.contextNotFound();
        if (c.getTrangThaiKhieuNai() == TrangThaiKhieuNai.SUBMITTED
                && (!complainantCanSeeSubmitted || !Objects.equals(c.getNguoiKhieuNai().getId(), actorId))) {
            throw ComplaintStaffScopeJpaSupport.contextNotFound();
        }
    }

    private void requireCurrentReviewer(KhieuNai c, Long actorId) {
        if (!c.isCurrentReviewer(actorId)) {
            throw conflict("COMPLAINT_NOT_ASSIGNED_TO_ACTOR", "Actor không phải current reviewer của complaint.");
        }
    }

    private Long currentResponseWindowHours(Long schoolId) {
        List<Long> values = entityManager.createQuery(
                "select cfg.thoiHanPhanHoiKhieuNaiGio from CauHinhNghiepVu cfg where cfg.nhaTruong.id=:sid", Long.class)
                .setParameter("sid", schoolId).setMaxResults(2).getResultList();
        if (values.size() != 1 || values.get(0) == null || values.get(0) <= 0) throw ComplaintStaffScopeJpaSupport.invariant();
        return values.get(0);
    }

    private long appendHistory(KhieuNai c, LoaiThaoTacXuLyKhieuNai action, TrangThaiKhieuNai previous,
            TrangThaiKhieuNai resulting, NguoiDung previousReviewer, NguoiDung resultingReviewer, NguoiDung actor,
            NguoiDung target, Instant occurredAt, Instant deadline, Long watermark, String reason) {
        Long max = entityManager.createQuery(
                "select coalesce(max(h.sequence),0) from NhatKyXuLyKhieuNai h where h.khieuNai.id=:id", Long.class)
                .setParameter("id", c.getId()).getSingleResult();
        long sequence = (max == null ? 0 : max) + 1;
        entityManager.persist(NhatKyXuLyKhieuNai.of(c, sequence, action, previous, resulting, previousReviewer,
                resultingReviewer, actor, target, occurredAt, deadline, watermark, reason));
        return sequence;
    }

    private ComplaintReviewSnapshots.QueueItem queueItem(KhieuNai c) {
        return new ComplaintReviewSnapshots.QueueItem(c.getId(), c.getTrangThaiKhieuNai().name(), c.getNopLuc(),
                c.getChuyenDi().getId(), c.getYeuCauDiChung().getId(), participantRole(c, c.getNguoiKhieuNai().getId()),
                participantRole(c, c.getNguoiBiKhieuNai().getId()), c.getNguoiTiepNhan() == null ? null : c.getNguoiTiepNhan().getId(),
                c.getTiepNhanLuc(), c.getHanPhanHoiBanDauLuc(), c.getHanBoSungLuc(), c.getGiaiQuyetLuc());
    }

    private ComplaintReviewSnapshots.ReviewCase reviewCase(KhieuNai c) {
        PhanHoiKhieuNai response = findResponse(c.getId());
        List<NhatKyXuLyKhieuNai> history = entityManager.createQuery(
                "select h from NhatKyXuLyKhieuNai h left join fetch h.reviewerTruoc left join fetch h.reviewerSau "
                        + "join fetch h.actor left join fetch h.targetParticipant where h.khieuNai.id=:id order by h.sequence", NhatKyXuLyKhieuNai.class)
                .setParameter("id", c.getId()).getResultList();
        return new ComplaintReviewSnapshots.ReviewCase(c.getId(), c.getTrangThaiKhieuNai().name(), c.getTieuDe(), c.getNoiDung(),
                c.getNopLuc(), c.getChuyenDi().getId(), c.getYeuCauDiChung().getId(), c.getNguoiKhieuNai().getId(),
                c.getNguoiBiKhieuNai().getId(), c.getNguoiTiepNhan() == null ? null : c.getNguoiTiepNhan().getId(),
                c.getTiepNhanLuc(), c.getThoiHanPhanHoiApDungGio(), c.getHanPhanHoiBanDauLuc(),
                c.getNguoiDuocYeuCauBoSung() == null ? null : c.getNguoiDuocYeuCauBoSung().getId(), c.getLyDoYeuCauBoSung(),
                c.getYeuCauBoSungLuc(), c.getHanBoSungLuc(), c.getMocIdMinhChungLucYeuCau(), c.getKetLuan(), c.getGiaiQuyetLuc(),
                response == null ? null : response.getNoiDung(), response == null ? null : response.getPhanHoiLuc(),
                countEvidence(c.getId(), c.getNguoiKhieuNai().getId()), countEvidence(c.getId(), c.getNguoiBiKhieuNai().getId()),
                history.stream().map(this::history).toList());
    }

    private ComplaintReviewSnapshots.HistoryItem history(NhatKyXuLyKhieuNai h) {
        return new ComplaintReviewSnapshots.HistoryItem(h.getSequence(), h.getThaoTac().name(), h.getTrangThaiTruoc().name(),
                h.getTrangThaiSau().name(), h.getReviewerTruoc() == null ? null : h.getReviewerTruoc().getId(),
                h.getReviewerSau() == null ? null : h.getReviewerSau().getId(), h.getActor().getId(),
                h.getTargetParticipant() == null ? null : h.getTargetParticipant().getId(), h.getOccurredAt(),
                h.getDeadlineAt(), h.getEvidenceIdWatermark(), h.getReason());
    }

    private ComplaintReviewSnapshots.Action action(KhieuNai c, boolean changed) {
        return new ComplaintReviewSnapshots.Action(c.getId(), c.getTrangThaiKhieuNai().name(),
                c.getNguoiTiepNhan() == null ? null : c.getNguoiTiepNhan().getId(), c.getTiepNhanLuc(),
                c.getHanPhanHoiBanDauLuc(), c.getHanBoSungLuc(), c.getGiaiQuyetLuc(), changed);
    }

    private PhanHoiKhieuNai findResponse(Long complaintId) {
        return entityManager.createQuery("select r from PhanHoiKhieuNai r where r.khieuNai.id=:id", PhanHoiKhieuNai.class)
                .setParameter("id", complaintId).setMaxResults(1).getResultList().stream().findFirst().orElse(null);
    }

    private long countEvidence(Long complaintId, Long uploaderId) {
        Long count = entityManager.createQuery(
                "select count(e) from TepMinhChung e where e.khieuNai.id=:cid and e.nguoiTaiLen.id=:uid", Long.class)
                .setParameter("cid", complaintId).setParameter("uid", uploaderId).getSingleResult();
        return count == null ? 0 : count;
    }

    private ComplaintReviewSnapshots.Evidence evidence(TepMinhChung e) {
        KhieuNai c = e.getKhieuNai();
        String role = c == null || e.getNguoiTaiLen() == null ? null
                : participantRole(c, e.getNguoiTaiLen().getId());
        return new ComplaintReviewSnapshots.Evidence(e.getId(), role, e.getNguoiTaiLen().getId(), e.getLoaiTep().name(),
                e.getOriginalFilename(), e.getVerifiedMediaType(), e.getSizeBytes(), e.getFileHash(), e.getStorageKey(),
                e.getMoTa(), e.getTaiLenLuc());
    }

    private ComplaintReviewSnapshots.Location location(BanGhiDinhVi l) {
        Point p = l.getToaDo();
        return new ComplaintReviewSnapshots.Location(l.getId(), l.getThuTuBanGhi(), l.getThoiGianTrinhDuyet(),
                l.getThoiGianServerNhan(), BigDecimal.valueOf(p.getY()), BigDecimal.valueOf(p.getX()), l.getDoChinhXacMet());
    }

    private static String participantRole(KhieuNai c, Long userId) {
        return c.getNguoiKhieuNai() != null && Objects.equals(c.getNguoiKhieuNai().getId(), userId) ? "COMPLAINANT" : "RESPONDENT";
    }

    private static TrangThaiKhieuNai parseActiveState(String status) {
        try {
            TrangThaiKhieuNai state = TrangThaiKhieuNai.valueOf(status == null ? "SUBMITTED" : status.trim().toUpperCase(Locale.ROOT));
            if (state == TrangThaiKhieuNai.RESOLVED || state == TrangThaiKhieuNai.CLOSED) throw validation("status không thuộc E8-03 active states.");
            return state;
        } catch (IllegalArgumentException ex) { throw validation("status không hợp lệ."); }
    }

    private static TrangThaiKhieuNai parseTerminal(String outcome) {
        try {
            TrangThaiKhieuNai state = TrangThaiKhieuNai.valueOf(outcome == null ? "" : outcome.trim().toUpperCase(Locale.ROOT));
            if (state != TrangThaiKhieuNai.ACCEPTED && state != TrangThaiKhieuNai.REJECTED) throw validation("outcome phải là ACCEPTED hoặc REJECTED.");
            return state;
        } catch (IllegalArgumentException ex) { throw validation("outcome phải là ACCEPTED hoặc REJECTED."); }
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) throw validation(field + " phải là số dương.");
    }

    private static String normalize(String value, int min, int max, String field) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.length() < min || normalized.length() > max) {
            throw validation(field + " phải từ " + min + " đến " + max + " ký tự.");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private static String normalizeClient(String value, int max, String fallback) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty()) normalized = fallback;
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    private static BusinessException concurrent() {
        return conflict("CONCURRENT_MODIFICATION", "Complaint đã thay đổi bởi yêu cầu khác.");
    }

    private static BusinessException mapDomain(RuntimeException ex) {
        String message = ex.getMessage() == null ? "Dữ liệu complaint review không hợp lệ." : ex.getMessage();
        if (message.contains("response window")) return conflict("COMPLAINT_RESPONSE_WINDOW_OPEN", message);
        if (message.contains("evidence request")) return conflict("COMPLAINT_EVIDENCE_REQUEST_NOT_OPEN", message);
        return ComplaintStaffScopeJpaSupport.invariant();
    }
}
