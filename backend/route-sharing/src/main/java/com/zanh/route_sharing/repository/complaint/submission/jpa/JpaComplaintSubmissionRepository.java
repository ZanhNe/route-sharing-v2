package com.zanh.route_sharing.repository.complaint.submission.jpa;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.complaint.submission.ComplaintSubmissionRepository;
import com.zanh.route_sharing.repository.complaint.submission.model.ComplaintSubmissionCommand;
import com.zanh.route_sharing.repository.complaint.submission.model.ComplaintSubmissionResult;
import com.zanh.route_sharing.utils.time.TimePolicy;
import jakarta.persistence.*;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Repository
public class JpaComplaintSubmissionRepository implements ComplaintSubmissionRepository {
    private static final Set<TrangThaiVanHanhChuyenDi> ELIGIBLE_TRIP_STATES = EnumSet.of(
            TrangThaiVanHanhChuyenDi.COMPLETED,
            TrangThaiVanHanhChuyenDi.EMERGENCY_ABORTED,
            TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START);
    private static final Set<TrangThaiYeuCau> RESOLVED_OPERATIONAL_BOOKING_STATES = EnumSet.of(
            TrangThaiYeuCau.COMPLETED,
            TrangThaiYeuCau.NO_SHOW,
            TrangThaiYeuCau.ABORTED);
    private static final String DUPLICATE_CONSTRAINT = "uk_khieu_nai_nguoi_booking";

    private final EntityManager entityManager;
    private final Clock clock;

    public JpaComplaintSubmissionRepository(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ComplaintSubmissionResult commit(ComplaintSubmissionCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockTrip(command.tripId());
            YeuCauDiChung booking = findExactBooking(trip, command.rideRequestId());
            ParticipantPair pair = requireParticipantPair(trip, booking, command.actorId());
            requireEligibleTripState(trip);
            requireBookingOutcomeCompatibility(trip, booking);

            Instant terminalAt = resolveTerminalAt(trip);
            CauHinhNghiepVu policy = lockCurrentPolicy(booking);
            long filingHours = requireFilingHours(policy);
            Instant submittedAt = TimePolicy.now(clock);
            Instant deadline = calculateDeadline(terminalAt, filingHours);
            if (submittedAt.isBefore(terminalAt)) {
                throw invariantViolation();
            }
            if (submittedAt.isAfter(deadline)) {
                throw filingWindowExpired();
            }

            SuCoChuyenDi incident = command.incidentId() == null
                    ? null
                    : findVisibleIncident(trip.getId(), command.actorId(), command.incidentId());

            Long existingId = findExistingComplaintId(command.actorId(), booking.getId());
            if (existingId != null) {
                throw duplicate(existingId);
            }

            KhieuNai complaint = KhieuNai.submit(
                    trip,
                    booking,
                    pair.complainant(),
                    pair.target(),
                    incident,
                    command.title(),
                    command.content(),
                    terminalAt,
                    submittedAt,
                    deadline);
            entityManager.persist(complaint);
            entityManager.flush();

            return new ComplaintSubmissionResult(
                    complaint.getId(),
                    trip.getId(),
                    booking.getId(),
                    pair.target().getId(),
                    complaint.getTrangThaiKhieuNai(),
                    complaint.getNopLuc(),
                    complaint.getHanNopKhieuNaiApDungLuc(),
                    incident == null ? null : incident.getId());
        } catch (BusinessException exception) {
            throw exception;
        } catch (PessimisticLockException | LockTimeoutException | OptimisticLockException exception) {
            throw concurrentModification();
        } catch (PersistenceException exception) {
            if (isDuplicateConstraint(exception)) {
                throw duplicate(null);
            }
            throw dataIntegrityViolation();
        } catch (IllegalArgumentException | IllegalStateException | DateTimeException exception) {
            throw invariantViolation();
        }
    }

    private ChuyenDi lockTrip(Long tripId) {
        List<ChuyenDi> rows = entityManager.createQuery(
                        "select trip from ChuyenDi trip "
                                + "join fetch trip.loTrinhChiaSe route "
                                + "join fetch route.taiXe driver "
                                + "where trip.id=:tripId",
                        ChuyenDi.class)
                .setParameter("tripId", tripId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(2)
                .getResultList();
        if (rows.size() != 1) {
            throw contextNotFound();
        }
        ChuyenDi trip = rows.get(0);
        if (trip.getLoTrinhChiaSe() == null || trip.getLoTrinhChiaSe().getId() == null
                || trip.getLoTrinhChiaSe().getTaiXe() == null || trip.getLoTrinhChiaSe().getTaiXe().getId() == null
                || trip.getTrangThaiVanHanh() == null) {
            throw invariantViolation();
        }
        return trip;
    }

    private static void requireEligibleTripState(ChuyenDi trip) {
        if (!ELIGIBLE_TRIP_STATES.contains(trip.getTrangThaiVanHanh())) {
            throw tripNotEligible();
        }
    }

    private YeuCauDiChung findExactBooking(ChuyenDi trip, Long bookingId) {
        List<YeuCauDiChung> rows = entityManager.createQuery(
                        "select booking from YeuCauDiChung booking "
                                + "join fetch booking.hanhKhach passenger "
                                + "join fetch booking.loTrinhChiaSe route "
                                + "join fetch booking.cauHinhLucGui originalConfig "
                                + "join fetch originalConfig.nhaTruong school "
                                + "where booking.id=:bookingId and booking.chuyenDi.id=:tripId",
                        YeuCauDiChung.class)
                .setParameter("bookingId", bookingId)
                .setParameter("tripId", trip.getId())
                .setMaxResults(2)
                .getResultList();
        if (rows.size() != 1) {
            throw contextNotFound();
        }
        YeuCauDiChung booking = rows.get(0);
        if (booking.getLoTrinhChiaSe() == null
                || !Objects.equals(booking.getLoTrinhChiaSe().getId(), trip.getLoTrinhChiaSe().getId())
                || booking.getHanhKhach() == null || booking.getHanhKhach().getId() == null
                || booking.getCauHinhLucGui() == null || booking.getCauHinhLucGui().getNhaTruong() == null
                || booking.getCauHinhLucGui().getNhaTruong().getId() == null) {
            throw invariantViolation();
        }
        return booking;
    }

    private static ParticipantPair requireParticipantPair(ChuyenDi trip, YeuCauDiChung booking, Long actorId) {
        NguoiDung driver = trip.getLoTrinhChiaSe().getTaiXe();
        NguoiDung passenger = booking.getHanhKhach();
        if (Objects.equals(actorId, passenger.getId())) {
            return new ParticipantPair(passenger, driver);
        }
        if (Objects.equals(actorId, driver.getId())) {
            return new ParticipantPair(driver, passenger);
        }
        throw contextNotFound();
    }

    private static void requireBookingOutcomeCompatibility(ChuyenDi trip, YeuCauDiChung booking) {
        if (booking.getTrangThaiYeuCau() == null) {
            throw invariantViolation();
        }
        if (trip.getTrangThaiVanHanh() == TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START) {
            if (booking.getTrangThaiYeuCau() != TrangThaiYeuCau.CANCELLED_BY_DRIVER) {
                throw invariantViolation();
            }
            return;
        }
        if (!RESOLVED_OPERATIONAL_BOOKING_STATES.contains(booking.getTrangThaiYeuCau())) {
            throw invariantViolation();
        }
    }

    private Instant resolveTerminalAt(ChuyenDi trip) {
        return switch (trip.getTrangThaiVanHanh()) {
            case COMPLETED -> resolveCompletedAt(trip);
            case EMERGENCY_ABORTED -> resolveEmergencyAbortedAt(trip);
            case CANCELLED_BEFORE_START -> resolveCancelledBeforeStartAt(trip);
            default -> throw tripNotEligible();
        };
    }

    private Instant resolveCompletedAt(ChuyenDi trip) {
        if (trip.getBatDauLuc() == null || trip.getKetThucLuc() == null
                || trip.getKetThucLuc().isBefore(trip.getBatDauLuc())) {
            throw invariantViolation();
        }
        NhatKyTrangThaiChuyenDi event = uniqueTerminalHistory(trip.getId(), TrangThaiVanHanhChuyenDi.COMPLETED);
        if (event.getTrangThaiTruoc() != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                || !"DRIVER_COMPLETED_TRIP".equals(event.getReasonCode())
                || !trip.getKetThucLuc().equals(event.getOccurredAt())) {
            throw invariantViolation();
        }
        return trip.getKetThucLuc();
    }

    private Instant resolveEmergencyAbortedAt(ChuyenDi trip) {
        if (trip.getBatDauLuc() == null || trip.getKetThucLuc() == null
                || trip.getKetThucLuc().isBefore(trip.getBatDauLuc())) {
            throw invariantViolation();
        }
        NhatKyTrangThaiChuyenDi event = uniqueTerminalHistory(trip.getId(), TrangThaiVanHanhChuyenDi.EMERGENCY_ABORTED);
        if ((event.getTrangThaiTruoc() != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                && event.getTrangThaiTruoc() != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN)
                || !"TRIP_EMERGENCY_ABORTED".equals(event.getReasonCode())
                || !trip.getKetThucLuc().equals(event.getOccurredAt())) {
            throw invariantViolation();
        }
        return trip.getKetThucLuc();
    }

    private Instant resolveCancelledBeforeStartAt(ChuyenDi trip) {
        if (trip.getBatDauLuc() != null || trip.getKetThucLuc() != null) {
            throw invariantViolation();
        }
        NhatKyTrangThaiChuyenDi event = uniqueTerminalHistory(trip.getId(), TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START);
        if (event.getTrangThaiTruoc() != TrangThaiVanHanhChuyenDi.PREPARING
                || !"DRIVER_CANCELLED_TRIP_BEFORE_START".equals(event.getReasonCode())
                || event.getOccurredAt() == null) {
            throw invariantViolation();
        }
        return event.getOccurredAt();
    }

    private NhatKyTrangThaiChuyenDi uniqueTerminalHistory(Long tripId, TrangThaiVanHanhChuyenDi terminalState) {
        List<NhatKyTrangThaiChuyenDi> rows = entityManager.createQuery(
                        "select event from NhatKyTrangThaiChuyenDi event "
                                + "where event.chuyenDi.id=:tripId and event.trangThaiSau=:state "
                                + "order by event.sequence",
                        NhatKyTrangThaiChuyenDi.class)
                .setParameter("tripId", tripId)
                .setParameter("state", terminalState)
                .setMaxResults(2)
                .getResultList();
        if (rows.size() != 1) {
            throw invariantViolation();
        }
        return rows.get(0);
    }

    private CauHinhNghiepVu lockCurrentPolicy(YeuCauDiChung booking) {
        Long schoolId = booking.getCauHinhLucGui().getNhaTruong().getId();
        List<CauHinhNghiepVu> rows = entityManager.createQuery(
                        "select configuration from CauHinhNghiepVu configuration "
                                + "where configuration.nhaTruong.id=:schoolId",
                        CauHinhNghiepVu.class)
                .setParameter("schoolId", schoolId)
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .setMaxResults(2)
                .getResultList();
        if (rows.size() != 1) {
            throw policyUnavailable();
        }
        return rows.get(0);
    }

    private static long requireFilingHours(CauHinhNghiepVu policy) {
        Long hours = policy.getThoiHanNopKhieuNaiGio();
        if (hours == null || hours <= 0) {
            throw policyUnavailable();
        }
        return hours;
    }

    private static Instant calculateDeadline(Instant terminalAt, long hours) {
        try {
            return terminalAt.plus(hours, ChronoUnit.HOURS);
        } catch (ArithmeticException | DateTimeException exception) {
            throw policyUnavailable();
        }
    }

    private SuCoChuyenDi findVisibleIncident(Long tripId, Long actorId, Long incidentId) {
        List<SuCoChuyenDi> rows = entityManager.createQuery(
                        "select incident from SuCoChuyenDi incident "
                                + "join fetch incident.nguoiBaoCao reporter "
                                + "where incident.id=:incidentId and incident.chuyenDi.id=:tripId and reporter.id=:actorId",
                        SuCoChuyenDi.class)
                .setParameter("incidentId", incidentId)
                .setParameter("tripId", tripId)
                .setParameter("actorId", actorId)
                .setMaxResults(2)
                .getResultList();
        if (rows.size() != 1) {
            throw incidentNotFound();
        }
        return rows.get(0);
    }

    private Long findExistingComplaintId(Long actorId, Long bookingId) {
        List<Long> ids = entityManager.createQuery(
                        "select complaint.id from KhieuNai complaint "
                                + "where complaint.nguoiKhieuNai.id=:actorId and complaint.yeuCauDiChung.id=:bookingId "
                                + "order by complaint.id",
                        Long.class)
                .setParameter("actorId", actorId)
                .setParameter("bookingId", bookingId)
                .setMaxResults(2)
                .getResultList();
        if (ids.size() > 1) {
            throw invariantViolation();
        }
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static void requireCommand(ComplaintSubmissionCommand command) {
        if (command == null || command.actorId() == null || command.actorId() <= 0
                || command.tripId() == null || command.tripId() <= 0
                || command.rideRequestId() == null || command.rideRequestId() <= 0
                || command.title() == null || command.content() == null
                || (command.incidentId() != null && command.incidentId() <= 0)) {
            throw validation();
        }
    }

    private static boolean isDuplicateConstraint(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof ConstraintViolationException violation
                    && DUPLICATE_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static BusinessException validation() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Thông tin nộp khiếu nại không hợp lệ.");
    }

    private static BusinessException contextNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "COMPLAINT_CONTEXT_NOT_FOUND", "Không tìm thấy ngữ cảnh khiếu nại phù hợp.");
    }

    private static BusinessException incidentNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "COMPLAINT_INCIDENT_NOT_FOUND", "Không tìm thấy incident phù hợp để tham chiếu.");
    }

    private static BusinessException tripNotEligible() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_NOT_ELIGIBLE_FOR_COMPLAINT", "Chuyến đi chưa thuộc trạng thái cho phép nộp khiếu nại.");
    }

    private static BusinessException filingWindowExpired() {
        return new BusinessException(HttpStatus.CONFLICT, "COMPLAINT_FILING_WINDOW_EXPIRED", "Đã quá thời hạn nộp khiếu nại.");
    }

    private static BusinessException duplicate(Long existingId) {
        Map<String, String> errors = existingId == null ? null : Map.of("existingComplaintId", existingId.toString());
        return new BusinessException(HttpStatus.CONFLICT, "COMPLAINT_ALREADY_SUBMITTED", "Khiếu nại cho booking này đã được nộp trước đó.", errors);
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Ngữ cảnh khiếu nại vừa được thay đổi đồng thời. Vui lòng thử lại.");
    }

    private static BusinessException dataIntegrityViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Không thể ghi nhận khiếu nại do ràng buộc dữ liệu.");
    }

    private static BusinessException invariantViolation() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "COMPLAINT_CONTEXT_INVARIANT_VIOLATION", "Dữ liệu Trip/booking lịch sử không nhất quán để nộp khiếu nại.");
    }

    private static BusinessException policyUnavailable() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "COMPLAINT_POLICY_UNAVAILABLE", "Chính sách thời hạn khiếu nại hiện không khả dụng.");
    }

    private record ParticipantPair(NguoiDung complainant, NguoiDung target) {
    }
}
