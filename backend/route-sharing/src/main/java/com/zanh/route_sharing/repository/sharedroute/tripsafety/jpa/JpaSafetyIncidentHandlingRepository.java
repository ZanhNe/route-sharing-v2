package com.zanh.route_sharing.repository.sharedroute.tripsafety.jpa;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.*;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.SafetyIncidentHandlingRepository;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.SafetyIncidentHandlingCommitResult;
import jakarta.persistence.*;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Repository
public class JpaSafetyIncidentHandlingRepository implements SafetyIncidentHandlingRepository {
    private static final String HANDLE_INCIDENT_PERMISSION = "HANDLE_INCIDENT";
    private final EntityManager entityManager;
    private final SafetyStaffScopeJpaSupport safetyStaffScope;

    public JpaSafetyIncidentHandlingRepository(EntityManager entityManager, SafetyStaffScopeJpaSupport safetyStaffScope) {
        this.entityManager = entityManager;
        this.safetyStaffScope = safetyStaffScope;
    }

    @Override
    @Transactional
    public SafetyIncidentHandlingCommitResult claim(Long actorId, Long incidentId, Instant occurredAt, LocalDate businessDate) {
        try {
            SuCoChuyenDi incident = lockIncident(incidentId);
            Long schoolId = requireActorScope(actorId, incident, businessDate);
            NguoiDung actor = requireUser(actorId);
            if (incident.getTrangThaiXuLy() == TrangThaiXuLySuCo.ACKNOWLEDGED
                    || incident.getTrangThaiXuLy() == TrangThaiXuLySuCo.INVESTIGATING) {
                if (sameHandler(incident, actorId)) return result(incident, occurredAt, "ACKNOWLEDGED", false, schoolId);
                throw conflict("INCIDENT_ALREADY_ASSIGNED", "Incident đã được nhân sự Safety khác tiếp nhận.");
            }
            if (incident.isTerminalHandlingState()) throw conflict("INCIDENT_ALREADY_FINALIZED", "Incident đã kết thúc xử lý.");
            if (incident.getTrangThaiXuLy() != TrangThaiXuLySuCo.OPEN) {
                throw conflict("INCIDENT_NOT_CLAIMABLE", "Incident không ở trạng thái có thể tiếp nhận.");
            }
            TrangThaiXuLySuCo previous = incident.getTrangThaiXuLy();
            incident.acknowledge(actor, occurredAt);
            appendHistory(incident, LoaiThaoTacXuLySuCo.CLAIMED, previous, incident.getTrangThaiXuLy(),
                    null, actor, actor, occurredAt, null, null);
            if (incident.getNguoiBaoCao() != null) entityManager.persist(ThongBao.tripSafetyIncidentAcknowledged(incident));
            entityManager.flush();
            return result(incident, occurredAt, "ACKNOWLEDGED", true, schoolId);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (ConstraintViolationException ex) { throw conflict("DATA_INTEGRITY_VIOLATION", "Xung đột dữ liệu Safety incident."); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw mapDomain(ex); }
        catch (PersistenceException ex) { throw SafetyStaffScopeJpaSupport.invariantViolation(); }
    }

    @Override
    @Transactional
    public SafetyIncidentHandlingCommitResult investigate(Long actorId, Long incidentId, Instant occurredAt, LocalDate businessDate) {
        try {
            SuCoChuyenDi incident = lockIncident(incidentId);
            Long schoolId = requireActorScope(actorId, incident, businessDate);
            NguoiDung actor = requireUser(actorId);
            requireCurrentHandler(incident, actorId);
            if (incident.getTrangThaiXuLy() == TrangThaiXuLySuCo.INVESTIGATING) {
                return result(incident, occurredAt, "INVESTIGATING", false, schoolId);
            }
            if (incident.isTerminalHandlingState()) throw conflict("INCIDENT_ALREADY_FINALIZED", "Incident đã kết thúc xử lý.");
            TrangThaiXuLySuCo previous = incident.getTrangThaiXuLy();
            incident.beginInvestigation(actor);
            appendHistory(incident, LoaiThaoTacXuLySuCo.INVESTIGATION_STARTED, previous, incident.getTrangThaiXuLy(),
                    actor, actor, actor, occurredAt, null, null);
            entityManager.flush();
            return result(incident, occurredAt, "INVESTIGATING", true, schoolId);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw mapDomain(ex); }
        catch (PersistenceException ex) { throw SafetyStaffScopeJpaSupport.invariantViolation(); }
    }

    @Override
    @Transactional
    public SafetyIncidentHandlingCommitResult reassign(Long actorId, Long incidentId, Long newHandlerUserId, String reason,
                                                        Instant occurredAt, LocalDate businessDate) {
        try {
            SuCoChuyenDi incident = lockIncident(incidentId);
            Long schoolId = requireActorScope(actorId, incident, businessDate);
            NguoiDung actor = requireUser(actorId);
            if (incident.isTerminalHandlingState()) throw conflict("INCIDENT_REASSIGNMENT_NOT_ALLOWED", "Incident đã kết thúc xử lý.");
            if (incident.getTrangThaiXuLy() != TrangThaiXuLySuCo.ACKNOWLEDGED
                    && incident.getTrangThaiXuLy() != TrangThaiXuLySuCo.INVESTIGATING) {
                throw conflict("INCIDENT_REASSIGNMENT_NOT_ALLOWED", "Incident chưa ở trạng thái có thể chuyển người xử lý.");
            }
            if (incident.getNguoiTiepNhan() == null) throw conflict("INCIDENT_REASSIGNMENT_NOT_ALLOWED", "Incident chưa có người xử lý.");
            if (Objects.equals(incident.getNguoiTiepNhan().getId(), newHandlerUserId)) {
                return result(incident, occurredAt, "REASSIGNED", false, schoolId);
            }
            if (newHandlerUserId == null || newHandlerUserId <= 0) throw badRequest("INVALID_REASSIGNMENT_TARGET", "newHandlerUserId không hợp lệ.");
            if (!safetyStaffScope.hasActiveSafetyStaffScope(newHandlerUserId, schoolId, businessDate)
                    || !safetyStaffScope.hasEffectivePermission(newHandlerUserId, HANDLE_INCIDENT_PERMISSION)) {
                throw conflict("INCIDENT_HANDLER_NOT_ELIGIBLE", "Nhân sự được chọn hiện không đủ điều kiện xử lý incident.");
            }
            NguoiDung previousHandler = incident.getNguoiTiepNhan();
            NguoiDung newHandler = requireUser(newHandlerUserId);
            TrangThaiXuLySuCo state = incident.getTrangThaiXuLy();
            incident.reassign(newHandler);
            appendHistory(incident, LoaiThaoTacXuLySuCo.REASSIGNED, state, state,
                    previousHandler, newHandler, actor, occurredAt, normalizeRequired(reason, 1000, "reason"), null);
            entityManager.flush();
            return result(incident, occurredAt, "REASSIGNED", true, schoolId);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw mapDomain(ex); }
        catch (PersistenceException ex) { throw SafetyStaffScopeJpaSupport.invariantViolation(); }
    }

    @Override
    @Transactional
    public SafetyIncidentHandlingCommitResult finalizeIncident(Long actorId, Long incidentId, TrangThaiXuLySuCo outcome,
                                                                String safeConclusion, Instant occurredAt, LocalDate businessDate) {
        try {
            String normalized = normalizeRequired(safeConclusion, 5000, "safeConclusion");
            if (outcome != TrangThaiXuLySuCo.RESOLVED && outcome != TrangThaiXuLySuCo.FALSE_ALARM) {
                throw badRequest("INVALID_INCIDENT_OUTCOME", "outcome phải là RESOLVED hoặc FALSE_ALARM.");
            }
            SuCoChuyenDi incident = lockIncident(incidentId);
            Long schoolId = requireActorScope(actorId, incident, businessDate);
            NguoiDung actor = requireUser(actorId);
            requireCurrentHandler(incident, actorId);
            if (incident.isTerminalHandlingState()) {
                if (incident.getTrangThaiXuLy() == outcome && Objects.equals(incident.getKetLuan(), normalized)) {
                    return result(incident, occurredAt, outcome.name(), false, schoolId);
                }
                throw conflict("INCIDENT_ALREADY_FINALIZED", "Incident đã có kết quả xử lý cuối cùng khác.");
            }
            TrangThaiXuLySuCo previous = incident.getTrangThaiXuLy();
            incident.finalizeHandling(actor, outcome, normalized, occurredAt);
            LoaiThaoTacXuLySuCo action = outcome == TrangThaiXuLySuCo.RESOLVED
                    ? LoaiThaoTacXuLySuCo.RESOLVED : LoaiThaoTacXuLySuCo.FALSE_ALARM;
            appendHistory(incident, action, previous, outcome, actor, actor, actor, occurredAt, null, normalized);
            if (incident.getNguoiBaoCao() != null) entityManager.persist(ThongBao.tripSafetyIncidentFinalized(incident));
            entityManager.flush();
            return result(incident, occurredAt, outcome.name(), true, schoolId);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (IllegalArgumentException | IllegalStateException ex) { throw mapDomain(ex); }
        catch (PersistenceException ex) { throw SafetyStaffScopeJpaSupport.invariantViolation(); }
    }

    private SuCoChuyenDi lockIncident(Long incidentId) {
        if (incidentId == null || incidentId <= 0) throw badRequest("VALIDATION_ERROR", "incidentId phải là số dương.");
        return entityManager.createQuery(
                        "select i from SuCoChuyenDi i join fetch i.chuyenDi t left join fetch i.nguoiTiepNhan h "
                                + "left join fetch i.nguoiBaoCao r where i.id=:id", SuCoChuyenDi.class)
                .setParameter("id", incidentId).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1)
                .getResultList().stream().findFirst().orElseThrow(SafetyStaffScopeJpaSupport::safetyIncidentNotFound);
    }

    private Long requireActorScope(Long actorId, SuCoChuyenDi incident, LocalDate businessDate) {
        Long schoolId = safetyStaffScope.resolveTripSchoolId(incident.getChuyenDi().getId());
        safetyStaffScope.requireActiveSafetyStaffScope(actorId, schoolId, businessDate);
        return schoolId;
    }

    private NguoiDung requireUser(Long id) {
        NguoiDung user = id == null ? null : entityManager.find(NguoiDung.class, id);
        if (user == null) throw SafetyStaffScopeJpaSupport.safetyIncidentNotFound();
        return user;
    }

    private void requireCurrentHandler(SuCoChuyenDi incident, Long actorId) {
        if (!sameHandler(incident, actorId)) {
            throw conflict("INCIDENT_NOT_ASSIGNED_TO_ACTOR", "Actor không phải người đang phụ trách incident.");
        }
    }

    private static boolean sameHandler(SuCoChuyenDi incident, Long actorId) {
        return incident.getNguoiTiepNhan() != null && Objects.equals(incident.getNguoiTiepNhan().getId(), actorId);
    }

    private void appendHistory(SuCoChuyenDi incident, LoaiThaoTacXuLySuCo action,
                               TrangThaiXuLySuCo previous, TrangThaiXuLySuCo resulting,
                               NguoiDung previousHandler, NguoiDung resultingHandler, NguoiDung actor,
                               Instant occurredAt, String reason, String conclusion) {
        Long max = entityManager.createQuery(
                        "select coalesce(max(h.sequence),0) from NhatKyXuLySuCo h where h.suCoChuyenDi.id=:id", Long.class)
                .setParameter("id", incident.getId()).getSingleResult();
        entityManager.persist(NhatKyXuLySuCo.of(incident, (max == null ? 0 : max) + 1,
                action, previous, resulting, previousHandler, resultingHandler, actor, occurredAt, reason, conclusion));
    }

    private SafetyIncidentHandlingCommitResult result(SuCoChuyenDi incident, Instant changedAt, String changeType,
                                                       boolean changed, Long schoolId) {
        List<Long> safetyRecipients = changed
                ? safetyStaffScope.findEligibleUserIds(schoolId,
                    LocalDate.ofInstant(changedAt, com.zanh.route_sharing.utils.time.TimePolicy.BUSINESS_ZONE), HANDLE_INCIDENT_PERMISSION)
                : List.of();
        return new SafetyIncidentHandlingCommitResult(
                incident.getId(), incident.getChuyenDi().getId(), incident.getTrangThaiXuLy().name(),
                incident.getNguoiTiepNhan() == null ? null : incident.getNguoiTiepNhan().getId(),
                incident.getNguoiTiepNhan() == null ? null : incident.getNguoiTiepNhan().getHoTen(),
                incident.getTiepNhanLuc(), incident.getGiaiQuyetLuc(), incident.getKetLuan(), changedAt,
                changeType, changed, incident.getNguoiBaoCao() == null ? null : incident.getNguoiBaoCao().getId(), safetyRecipients);
    }

    private static String normalizeRequired(String value, int max, String field) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty()) {
            String code = field.equals("safeConclusion") ? "INCIDENT_CONCLUSION_REQUIRED" : "VALIDATION_ERROR";
            throw badRequest(code, field + " không được trống.");
        }
        if (normalized.length() > max) throw badRequest("VALIDATION_ERROR", field + " vượt quá " + max + " ký tự.");
        return normalized;
    }

    private static BusinessException mapDomain(RuntimeException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (message.contains("current incident handler")) return conflict("INCIDENT_NOT_ASSIGNED_TO_ACTOR", "Actor không phải người đang phụ trách incident.");
        if (message.contains("OPEN") || message.contains("INVESTIGATING") || message.contains("kết thúc xử lý"))
            return conflict("INCIDENT_INVALID_TRANSITION", "Chuyển trạng thái xử lý incident không hợp lệ.");
        if (message.contains("safeConclusion")) return badRequest("INCIDENT_CONCLUSION_REQUIRED", "safeConclusion không hợp lệ.");
        return SafetyStaffScopeJpaSupport.invariantViolation();
    }

    private static BusinessException badRequest(String code, String message) { return new BusinessException(HttpStatus.BAD_REQUEST, code, message); }
    private static BusinessException conflict(String code, String message) { return new BusinessException(HttpStatus.CONFLICT, code, message); }
    private static BusinessException concurrent() { return conflict("CONCURRENT_MODIFICATION", "Incident đang được cập nhật đồng thời, vui lòng tải lại."); }
}
