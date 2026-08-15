package com.zanh.route_sharing.repository.sharedroute.tripsafety.jpa;

import com.zanh.route_sharing.domain.entity.BanGhiDinhVi;
import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.SuCoChuyenDi;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.LoaiSuCo;
import com.zanh.route_sharing.domain.enums.NguonPhatHienSuCo;
import com.zanh.route_sharing.domain.enums.TrangThaiHoSoThanhVien;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCurrentOrdering;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.TripSafetyIncidentRepository;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.SafetyIncidentSummarySnapshot;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyIncidentCommand;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyIncidentCommitResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import org.hibernate.exception.ConstraintViolationException;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
public class JpaTripSafetyIncidentRepository implements TripSafetyIncidentRepository {
    private static final String HANDLE_INCIDENT_PERMISSION = "HANDLE_INCIDENT";
    private static final Set<TrangThaiVanHanhChuyenDi> REPORTING_ACTIVE_TRIP_STATES = EnumSet.of(
            TrangThaiVanHanhChuyenDi.IN_PROGRESS,
            TrangThaiVanHanhChuyenDi.SECURITY_FROZEN);
    private static final Set<TrangThaiXuLySuCo> UNRESOLVED_INCIDENT_STATES = EnumSet.of(
            TrangThaiXuLySuCo.OPEN,
            TrangThaiXuLySuCo.ACKNOWLEDGED,
            TrangThaiXuLySuCo.INVESTIGATING);

    private final EntityManager entityManager;
    private final SafetyStaffScopeJpaSupport safetyStaffScope;
    public JpaTripSafetyIncidentRepository(EntityManager entityManager, SafetyStaffScopeJpaSupport safetyStaffScope) {
        this.entityManager = entityManager;
        this.safetyStaffScope = safetyStaffScope;
    }

    @Override
    @Transactional
    public TripSafetyIncidentCommitResult commit(TripSafetyIncidentCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockTrip(command.tripId());
            ReporterContext reporter = resolveReporterIdentity(trip, command.actorId());

            // E6-06 exact SOS retry must be resolved before checking current target/Trip activity,
            // because the first committed SOS may itself have ABORTED the target/Trip.
            if (command.type() == LoaiSuCo.SOS) {
                SuCoChuyenDi existing = findUnresolvedSos(trip.getId(), command.actorId(), command.reportedParticipantId());
                if (existing != null) {
                    return result(existing, false, List.of(), null, false, List.of(), List.of());
                }
            }

            requireTripReportingActive(trip);
            requireReporterActive(reporter);
            NguoiDung reportedParticipant = resolveReportedParticipant(trip, reporter, command.reportedParticipantId());
            Long schoolId = safetyStaffScope.resolveTripSchoolId(trip.getId());
            BanGhiDinhVi currentLocation = findCurrentLocation(trip.getId());
            requireCurrentLocationConsistency(trip, currentLocation);

            SuCoChuyenDi incident = SuCoChuyenDi.participantReported(
                    trip, reporter.booking(), reporter.user(), reportedParticipant, reporter.source(),
                    command.type(), command.description(), command.reportedAt(),
                    currentLocation == null ? trip.getViTriCuoiCung() : currentLocation.getToaDo(),
                    currentLocation == null ? null : currentLocation.getThoiGianTrinhDuyet(),
                    currentLocation == null ? trip.getNhanTinHieuCuoiLuc() : currentLocation.getThoiGianServerNhan(),
                    trip.getTrangThaiGiamSat(),
                    trip.getNhanTinHieuCuoiLuc() != null ? trip.getNhanTinHieuCuoiLuc() : trip.getBatDauLuc());
            entityManager.persist(incident);
            entityManager.flush();

            LocalDate businessDate = command.reportedAt()
                    .atZone(com.zanh.route_sharing.utils.time.TimePolicy.BUSINESS_ZONE).toLocalDate();
            List<Long> recipientIds = safetyStaffScope.findEligibleUserIds(schoolId, businessDate, HANDLE_INCIDENT_PERMISSION);
            if (!recipientIds.isEmpty()) {
                List<NguoiDung> recipients = entityManager.createQuery(
                                "select user from NguoiDung user where user.id in :ids order by user.id", NguoiDung.class)
                        .setParameter("ids", recipientIds).getResultList();
                if (recipients.size() != recipientIds.size()) throw invariantViolation();
                for (NguoiDung recipient : recipients) entityManager.persist(ThongBao.tripSafetyIncidentReported(incident, recipient));
            }

            entityManager.flush();
            return result(incident, true, recipientIds, null, false, List.of(), List.of());
        } catch (BusinessException exception) {
            throw exception;
        } catch (PessimisticLockException | LockTimeoutException | OptimisticLockException exception) {
            throw concurrentModification();
        } catch (ConstraintViolationException exception) {
            throw dataIntegrityViolation();
        } catch (PersistenceException exception) {
            throw invariantViolation();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw mapDomainFailure(exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SafetyIncidentSummarySnapshot findAuthorizedSummary(Long actorId, Long incidentId, LocalDate businessDate) {
        if (actorId == null || actorId <= 0 || incidentId == null || incidentId <= 0 || businessDate == null) {
            throw new IllegalArgumentException("Safety incident query không hợp lệ.");
        }
        SuCoChuyenDi incident = entityManager.createQuery(
                        "select incident from SuCoChuyenDi incident "
                                + "join fetch incident.chuyenDi trip "
                                + "where incident.id = :incidentId",
                        SuCoChuyenDi.class)
                .setParameter("incidentId", incidentId)
                .setMaxResults(1)
                .getResultList().stream().findFirst()
                .orElseThrow(JpaTripSafetyIncidentRepository::safetyIncidentNotFound);
        Long schoolId = safetyStaffScope.resolveTripSchoolId(incident.getChuyenDi().getId());
        if (!safetyStaffScope.hasActiveSafetyStaffScope(actorId, schoolId, businessDate)) {
            throw safetyIncidentNotFound();
        }
        return new SafetyIncidentSummarySnapshot(
                incident.getId(),
                incident.getChuyenDi().getId(),
                incident.getLoaiSuCo(),
                incident.getMucDo(),
                incident.getTrangThaiXuLy(),
                incident.getNguonPhatHien(),
                incident.getBaoCaoLuc());
    }

    private ChuyenDi lockTrip(Long tripId) {
        try {
            return entityManager.createQuery(
                            "select trip from ChuyenDi trip "
                                    + "join fetch trip.loTrinhChiaSe route "
                                    + "join fetch route.taiXe driver "
                                    + "where trip.id = :tripId",
                            ChuyenDi.class)
                    .setParameter("tripId", tripId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultList().stream().findFirst()
                    .orElseThrow(JpaTripSafetyIncidentRepository::tripNotFound);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private ReporterContext resolveReporterIdentity(ChuyenDi trip, Long actorId) {
        NguoiDung driver = trip.getLoTrinhChiaSe().getTaiXe();
        if (driver != null && Objects.equals(driver.getId(), actorId)) {
            return new ReporterContext(driver, null, NguonPhatHienSuCo.DRIVER);
        }

        List<YeuCauDiChung> bookings = entityManager.createQuery(
                        "select booking from YeuCauDiChung booking "
                                + "join fetch booking.hanhKhach passenger "
                                + "where booking.chuyenDi.id = :tripId and passenger.id = :actorId",
                        YeuCauDiChung.class)
                .setParameter("tripId", trip.getId())
                .setParameter("actorId", actorId)
                .getResultList();
        if (bookings.isEmpty()) {
            throw tripNotFound();
        }
        if (bookings.size() != 1) {
            throw invariantViolation();
        }
        YeuCauDiChung booking = bookings.get(0);
        return new ReporterContext(booking.getHanhKhach(), booking, NguonPhatHienSuCo.PASSENGER);
    }

    private static void requireReporterActive(ReporterContext reporter) {
        if (reporter.source() == NguonPhatHienSuCo.PASSENGER
                && (reporter.booking() == null || !reporter.booking().getTrangThaiYeuCau().isActiveTripParticipant())) {
            throw reporterNotActive();
        }
    }

    private NguoiDung resolveReportedParticipant(
            ChuyenDi trip,
            ReporterContext reporter,
            Long reportedParticipantId) {
        if (reportedParticipantId == null) {
            return null;
        }
        if (Objects.equals(reporter.user().getId(), reportedParticipantId)) {
            throw invalidReportedParticipant();
        }
        NguoiDung driver = trip.getLoTrinhChiaSe().getTaiXe();
        if (reporter.source() == NguonPhatHienSuCo.PASSENGER) {
            if (driver == null || !Objects.equals(driver.getId(), reportedParticipantId)) {
                throw reportedParticipantNotFound();
            }
            return driver;
        }

        List<NguoiDung> passengers = entityManager.createQuery(
                        "select booking.hanhKhach from YeuCauDiChung booking "
                                + "where booking.chuyenDi.id = :tripId "
                                + "and booking.hanhKhach.id = :reportedParticipantId "
                                + "and booking.trangThaiYeuCau in :activeStates",
                        NguoiDung.class)
                .setParameter("tripId", trip.getId())
                .setParameter("reportedParticipantId", reportedParticipantId)
                .setParameter("activeStates", TrangThaiYeuCau.activeTripParticipantStates())
                .getResultList();
        if (passengers.size() != 1) {
            throw reportedParticipantNotFound();
        }
        return passengers.get(0);
    }

    private SuCoChuyenDi findUnresolvedSos(Long tripId, Long reporterId, Long reportedParticipantId) {
        String targetPredicate = reportedParticipantId == null
                ? "and incident.nguoiBiBaoCao is null "
                : "and incident.nguoiBiBaoCao.id = :targetId ";
        var query = entityManager.createQuery(
                        "select incident from SuCoChuyenDi incident "
                                + "where incident.chuyenDi.id = :tripId "
                                + "and incident.nguoiBaoCao.id = :reporterId "
                                + targetPredicate
                                + "and incident.loaiSuCo = :type "
                                + "and incident.trangThaiXuLy in :states "
                                + "order by incident.baoCaoLuc asc, incident.id asc", SuCoChuyenDi.class)
                .setParameter("tripId", tripId).setParameter("reporterId", reporterId)
                .setParameter("type", LoaiSuCo.SOS).setParameter("states", UNRESOLVED_INCIDENT_STATES);
        if (reportedParticipantId != null) query.setParameter("targetId", reportedParticipantId);
        List<SuCoChuyenDi> incidents = query.getResultList();
        if (incidents.size() > 1) throw invariantViolation();
        return incidents.isEmpty() ? null : incidents.get(0);
    }

    private BanGhiDinhVi findCurrentLocation(Long tripId) {
        String sql = """
                SELECT id
                FROM ban_ghi_dinh_vi
                WHERE chuyen_di_id = :tripId
                """ + TripLocationCurrentOrdering.SQL_ORDER_BY + " LIMIT 1";
        List<?> ids = entityManager.createNativeQuery(sql)
                .setParameter("tripId", tripId)
                .getResultList();
        if (ids.isEmpty()) {
            return null;
        }
        Object id = ids.get(0);
        if (!(id instanceof Number number)) {
            throw invariantViolation();
        }
        return entityManager.find(BanGhiDinhVi.class, number.longValue());
    }

    private static void requireCurrentLocationConsistency(ChuyenDi trip, BanGhiDinhVi currentLocation) {
        if (currentLocation == null) {
            return;
        }
        Point tripPoint = trip.getViTriCuoiCung();
        if (tripPoint == null || tripPoint.isEmpty()
                || currentLocation.getToaDo() == null || currentLocation.getToaDo().isEmpty()
                || Double.compare(tripPoint.getX(), currentLocation.getToaDo().getX()) != 0
                || Double.compare(tripPoint.getY(), currentLocation.getToaDo().getY()) != 0
                || !Objects.equals(trip.getNhanTinHieuCuoiLuc(), currentLocation.getThoiGianServerNhan())) {
            throw invariantViolation();
        }
    }

    private static void requireTripReportingActive(ChuyenDi trip) {
        if (!REPORTING_ACTIVE_TRIP_STATES.contains(trip.getTrangThaiVanHanh()) || trip.getBatDauLuc() == null) {
            throw reportingNotActive();
        }
        if (trip.getTrangThaiGiamSat() == null) {
            throw invariantViolation();
        }
    }

    private static void requireCommand(TripSafetyIncidentCommand command) {
        if (command == null || command.actorId() == null || command.actorId() <= 0
                || command.tripId() == null || command.tripId() <= 0
                || command.type() == null || command.reportedAt() == null) {
            throw new IllegalArgumentException("TripSafetyIncidentCommand không hợp lệ.");
        }
        if (command.reportedParticipantId() != null && command.reportedParticipantId() <= 0) {
            throw new IllegalArgumentException("reportedParticipantId phải là số dương.");
        }
    }

    private static TripSafetyIncidentCommitResult result(
            SuCoChuyenDi incident, boolean created, List<Long> recipients,
            com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyInterventionSnapshot intervention,
            boolean interventionChanged, List<Long> tripStateRecipients, List<Long> participantRecipients) {
        return new TripSafetyIncidentCommitResult(
                incident.getId(), incident.getChuyenDi().getId(), incident.getLoaiSuCo(), incident.getMucDo(),
                incident.getTrangThaiXuLy(), incident.getNguonPhatHien(), incident.getBaoCaoLuc(), created, recipients,
                intervention, interventionChanged, tripStateRecipients, participantRecipients);
    }

    private static BusinessException mapDomainFailure(RuntimeException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("manual reporting scope")) {
            return new BusinessException(HttpStatus.BAD_REQUEST, "INCIDENT_TYPE_NOT_REPORTABLE",
                    "Loại sự cố không thuộc phạm vi báo cáo thủ công của E6-04.");
        }
        if (message.contains("Ordinary incident phải có description")) {
            return new BusinessException(HttpStatus.BAD_REQUEST, "INCIDENT_DESCRIPTION_REQUIRED",
                    "Sự cố thông thường phải có mô tả.");
        }
        if (message.contains("description không được vượt")) {
            return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "description không được vượt quá 5000 ký tự.");
        }
        return invariantViolation();
    }

    private static BusinessException tripNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi.");
    }

    private static BusinessException reportingNotActive() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_SAFETY_REPORT_NOT_ACTIVE",
                "Chuyến đi hiện không ở lifecycle cho phép báo sự cố/SOS.");
    }

    private static BusinessException reporterNotActive() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_SAFETY_REPORTER_NOT_ACTIVE",
                "Passenger hiện không còn là participant active của chuyến đi.");
    }

    private static BusinessException invalidReportedParticipant() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_REPORTED_PARTICIPANT",
                "reportedParticipantId không hợp lệ cho reporter hiện tại.");
    }

    private static BusinessException reportedParticipantNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "REPORTED_PARTICIPANT_NOT_FOUND",
                "Không tìm thấy participant phù hợp trong phạm vi chuyến đi.");
    }

    private static BusinessException safetyIncidentNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "SAFETY_INCIDENT_NOT_FOUND",
                "Không tìm thấy safety incident.");
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Chuyến đi vừa được thay đổi đồng thời. Vui lòng tải lại dữ liệu.");
    }

    private static BusinessException dataIntegrityViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "Xung đột ràng buộc dữ liệu khi ghi nhận safety incident.");
    }

    private static BusinessException invariantViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_SAFETY_INCIDENT_INVARIANT_VIOLATION",
                "Dữ liệu chuyến đi không nhất quán để ghi nhận safety incident.");
    }

    private record ReporterContext(
            NguoiDung user,
            YeuCauDiChung booking,
            NguonPhatHienSuCo source) {
    }
}
