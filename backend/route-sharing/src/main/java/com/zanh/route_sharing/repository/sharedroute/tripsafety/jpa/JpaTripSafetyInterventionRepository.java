package com.zanh.route_sharing.repository.sharedroute.tripsafety.jpa;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.TripSafetyInterventionRepository;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyInterventionCommitResult;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Repository
public class JpaTripSafetyInterventionRepository implements TripSafetyInterventionRepository {
    private final EntityManager entityManager;
    private final TripSafetyInterventionJpaSupport support;
    private final SafetyStaffScopeJpaSupport safetyStaffScope;

    public JpaTripSafetyInterventionRepository(EntityManager entityManager,
                                                TripSafetyInterventionJpaSupport support,
                                                SafetyStaffScopeJpaSupport safetyStaffScope) {
        this.entityManager = entityManager;
        this.support = support;
        this.safetyStaffScope = safetyStaffScope;
    }

    @Override
    @Transactional
    public TripSafetyInterventionCommitResult ensureInitialContainment(Long actorId, Long incidentId, Instant occurredAt) {
        try {
            Long tripId = findTripIdForIncident(incidentId);
            ChuyenDi trip = lockTrip(tripId);
            SuCoChuyenDi incident = entityManager.createQuery(
                            "select i from SuCoChuyenDi i join fetch i.nguoiBaoCao reporter "
                                    + "left join fetch i.nguoiBiBaoCao target where i.id=:id", SuCoChuyenDi.class)
                    .setParameter("id", incidentId).setMaxResults(1).getResultList().stream().findFirst()
                    .orElseThrow(SafetyStaffScopeJpaSupport::safetyIncidentNotFound);
            if (actorId == null || actorId <= 0 || incident.getNguoiBaoCao() == null
                    || !Objects.equals(incident.getNguoiBaoCao().getId(), actorId)) {
                throw notFound();
            }
            if (incident.getLoaiSuCo() != com.zanh.route_sharing.domain.enums.LoaiSuCo.SOS) {
                throw new BusinessException(HttpStatus.CONFLICT, "SAFETY_INTERVENTION_INVALID_TRANSITION",
                        "Initial containment chỉ áp dụng cho SOS.");
            }
            var existing = support.latestForIncident(incidentId);
            if (existing != null) {
                return new TripSafetyInterventionCommitResult(existing, false, List.of(), List.of());
            }
            Long targetUserId = incident.getNguoiBiBaoCao() == null ? null : incident.getNguoiBiBaoCao().getId();
            return support.containInitialSos(trip, incident, incident.getNguoiBaoCao(),
                    incident.getNguonPhatHien(), targetUserId, occurredAt);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (IllegalArgumentException | IllegalStateException | PersistenceException ex) { throw invalidStoredPlan(); }
    }

    @Override
    @Transactional
    public TripSafetyInterventionCommitResult confirmSafeExit(Long actorId, Long tripId, Long interventionId,
                                                               Point position, Instant occurredAt) {
        try {
            ChuyenDi trip = lockTrip(tripId);
            NguoiDung driver = requireDriverOwnership(trip, actorId);
            CanThiepAnToanChuyenDi hold = loadOwnedIntervention(tripId, interventionId);
            return support.confirmSafeExit(trip, hold, driver, position, occurredAt);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (IllegalArgumentException ex) { throw new BusinessException(HttpStatus.CONFLICT, "SAFETY_INTERVENTION_INVALID_TRANSITION", ex.getMessage()); }
        catch (IllegalStateException | PersistenceException ex) { throw invalidStoredPlan(); }
    }

    @Override
    @Transactional
    public TripSafetyInterventionCommitResult abortTripFromHold(Long actorId, Long tripId, Long interventionId,
                                                                 Instant occurredAt) {
        try {
            ChuyenDi trip = lockTrip(tripId);
            NguoiDung driver = requireDriverOwnership(trip, actorId);
            CanThiepAnToanChuyenDi hold = loadOwnedIntervention(tripId, interventionId);
            return support.abortTripFromHold(trip, hold, driver, occurredAt);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (IllegalArgumentException | IllegalStateException | PersistenceException ex) { throw invalidStoredPlan(); }
    }

    @Override
    @Transactional
    public TripSafetyInterventionCommitResult abortTripBySafety(Long actorId, Long incidentId, Instant occurredAt,
                                                                 LocalDate businessDate) {
        try {
            Long tripId = findTripIdForIncident(incidentId);
            ChuyenDi trip = lockTrip(tripId);
            SuCoChuyenDi incident = lockIncident(incidentId);
            Long schoolId = safetyStaffScope.resolveTripSchoolId(tripId);
            safetyStaffScope.requireActiveSafetyStaffScope(actorId, schoolId, businessDate);

            // Exact same Safety-abort retry is recoverable even when the Incident has since been finalized,
            // but only by the same actor who created that material intervention and who still has current Safety scope.
            var existing = support.completedTripAbortForIncidentAndActor(incidentId, actorId);
            if (existing != null && trip.getTrangThaiVanHanh() == com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi.EMERGENCY_ABORTED) {
                return new TripSafetyInterventionCommitResult(existing, false, List.of(), List.of());
            }

            if (incident.getNguoiTiepNhan() == null || !Objects.equals(incident.getNguoiTiepNhan().getId(), actorId)) {
                throw new BusinessException(HttpStatus.CONFLICT, "INCIDENT_NOT_ASSIGNED_TO_ACTOR",
                        "Actor không phải người đang phụ trách incident.");
            }
            if (incident.getTrangThaiXuLy() != TrangThaiXuLySuCo.ACKNOWLEDGED
                    && incident.getTrangThaiXuLy() != TrangThaiXuLySuCo.INVESTIGATING) {
                throw new BusinessException(HttpStatus.CONFLICT, "SAFETY_INTERVENTION_INVALID_TRANSITION",
                        "Incident chưa ở trạng thái cho phép Safety emergency-abort Trip.");
            }
            NguoiDung actor = entityManager.find(NguoiDung.class, actorId);
            if (actor == null) throw SafetyStaffScopeJpaSupport.safetyIncidentNotFound();
            return support.abortTripBySafety(trip, incident, actor, occurredAt);
        } catch (BusinessException ex) { throw ex; }
        catch (PessimisticLockException | LockTimeoutException | OptimisticLockException ex) { throw concurrent(); }
        catch (IllegalArgumentException | IllegalStateException | PersistenceException ex) { throw invalidStoredPlan(); }
    }

    private ChuyenDi lockTrip(Long tripId) {
        if (tripId == null || tripId <= 0) throw validation();
        return entityManager.createQuery("select t from ChuyenDi t join fetch t.loTrinhChiaSe r join fetch r.taiXe d where t.id=:id", ChuyenDi.class)
                .setParameter("id", tripId).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1)
                .getResultList().stream().findFirst().orElseThrow(JpaTripSafetyInterventionRepository::notFound);
    }

    private NguoiDung requireDriverOwnership(ChuyenDi trip, Long actorId) {
        if (actorId == null || actorId <= 0 || trip.getLoTrinhChiaSe() == null || trip.getLoTrinhChiaSe().getTaiXe() == null
                || !Objects.equals(trip.getLoTrinhChiaSe().getTaiXe().getId(), actorId)) throw notFound();
        return trip.getLoTrinhChiaSe().getTaiXe();
    }

    private CanThiepAnToanChuyenDi loadOwnedIntervention(Long tripId, Long interventionId) {
        if (interventionId == null || interventionId <= 0) throw validation();
        return entityManager.createQuery(
                        "select c from CanThiepAnToanChuyenDi c join fetch c.chuyenDi t join fetch c.suCoChuyenDi i "
                                + "left join fetch c.yeuCauMucTieu b where c.id=:id and t.id=:tripId", CanThiepAnToanChuyenDi.class)
                .setParameter("id", interventionId).setParameter("tripId", tripId).setMaxResults(1)
                .getResultList().stream().findFirst().orElseThrow(JpaTripSafetyInterventionRepository::notFound);
    }

    private Long findTripIdForIncident(Long incidentId) {
        if (incidentId == null || incidentId <= 0) throw validation();
        List<Long> ids = entityManager.createQuery("select i.chuyenDi.id from SuCoChuyenDi i where i.id=:id", Long.class)
                .setParameter("id", incidentId).getResultList();
        if (ids.size() != 1) throw SafetyStaffScopeJpaSupport.safetyIncidentNotFound();
        return ids.get(0);
    }

    private SuCoChuyenDi lockIncident(Long incidentId) {
        return entityManager.createQuery("select i from SuCoChuyenDi i left join fetch i.nguoiTiepNhan h where i.id=:id", SuCoChuyenDi.class)
                .setParameter("id", incidentId).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1)
                .getResultList().stream().findFirst().orElseThrow(SafetyStaffScopeJpaSupport::safetyIncidentNotFound);
    }

    private static BusinessException validation() { return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Id không hợp lệ."); }
    private static BusinessException notFound() { return new BusinessException(HttpStatus.NOT_FOUND, "SAFETY_INTERVENTION_NOT_FOUND", "Không tìm thấy Safety intervention."); }
    private static BusinessException concurrent() { return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Trip vừa được thay đổi đồng thời."); }
    private static BusinessException invalidStoredPlan() { return new BusinessException(HttpStatus.CONFLICT, "INVALID_STORED_TRIP_PLAN", "Dữ liệu Trip không nhất quán cho Safety intervention."); }
}
