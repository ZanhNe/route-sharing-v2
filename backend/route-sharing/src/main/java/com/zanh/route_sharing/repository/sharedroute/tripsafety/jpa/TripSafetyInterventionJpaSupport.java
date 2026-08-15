package com.zanh.route_sharing.repository.sharedroute.tripsafety.jpa;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.*;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyInterventionCommitResult;
import com.zanh.route_sharing.repository.sharedroute.tripsafety.model.TripSafetyInterventionSnapshot;
import jakarta.persistence.EntityManager;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class TripSafetyInterventionJpaSupport {
    private static final Set<TrangThaiYeuCau> ACTIVE_BOOKING_STATES = EnumSet.of(TrangThaiYeuCau.ACCEPTED, TrangThaiYeuCau.ON_BOARD);
    private static final Set<TrangThaiDiemDung> UNFINISHED_STOP_STATES = EnumSet.of(
            TrangThaiDiemDung.PENDING, TrangThaiDiemDung.APPROACHING, TrangThaiDiemDung.ARRIVED);
    private static final String HOLD_REASON = "Chuyến đang tạm dừng để xử lý một tình huống an toàn.";
    private static final String PARTICIPANT_ABORT_REASON = "Việc tham gia chuyến đi được kết thúc vì lý do an toàn.";
    private static final String TRIP_ABORT_REASON = "Chuyến đi được kết thúc khẩn cấp vì lý do an toàn.";

    private final EntityManager entityManager;

    public TripSafetyInterventionJpaSupport(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public TripSafetyInterventionCommitResult containInitialSos(
            ChuyenDi trip, SuCoChuyenDi incident, NguoiDung reporter, NguonPhatHienSuCo reporterSource,
            Long reportedParticipantId, Instant occurredAt) {
        requireStoredPlanConsistent(trip);
        YeuCauDiChung targetBooking = reportedParticipantId == null ? null : findPassengerBookingOrNull(trip.getId(), reportedParticipantId);
        NguoiDung driver = requireDriver(trip);

        if (reporterSource == NguonPhatHienSuCo.PASSENGER) {
            if (reportedParticipantId != null && !Objects.equals(driver.getId(), reportedParticipantId)) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "REPORTED_PARTICIPANT_NOT_FOUND", "Không tìm thấy Driver target trong Trip.");
            }
            return abortWholeTrip(trip, incident, null, reporter, occurredAt);
        }

        if (reportedParticipantId == null) {
            return abortWholeTrip(trip, incident, null, reporter, occurredAt);
        }
        if (targetBooking == null || !targetBooking.getTrangThaiYeuCau().isActiveTripParticipant()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "REPORTED_PARTICIPANT_NOT_FOUND", "Passenger target không còn active trong Trip.");
        }

        CanThiepAnToanChuyenDi activeHold = findActiveHold(trip.getId());
        if (targetBooking.getTrangThaiYeuCau() == TrangThaiYeuCau.ACCEPTED) {
            return abortParticipant(trip, incident, targetBooking, reporter, occurredAt, activeHold != null);
        }
        if (targetBooking.getTrangThaiYeuCau() == TrangThaiYeuCau.ON_BOARD) {
            if (activeHold != null) {
                if (activeHold.getYeuCauMucTieu() != null
                        && Objects.equals(activeHold.getYeuCauMucTieu().getId(), targetBooking.getId())) {
                    return noOp(activeHold);
                }
                activeHold.dungDoChuyenDiKetThuc(reporter, occurredAt);
                return abortWholeTrip(trip, incident, activeHold, reporter, occurredAt);
            }
            return startSafeExitHold(trip, incident, targetBooking, reporter, occurredAt);
        }
        throw invalidStoredPlan();
    }

    public TripSafetyInterventionCommitResult confirmSafeExit(
            ChuyenDi trip, CanThiepAnToanChuyenDi hold, NguoiDung driver, Point position, Instant occurredAt) {
        if (hold.getTrangThaiCanThiep() == TrangThaiCanThiepAnToan.HOAN_TAT
                && hold.getLoaiCanThiep() == LoaiCanThiepAnToan.GIU_DE_XUONG_XE_AN_TOAN
                && hold.getXuongXeKhanCapLuc() != null && hold.getToaDoXuongXeKhanCap() != null) {
            return noOp(hold);
        }
        requireStoredPlanConsistent(trip);
        requireActiveHoldForTrip(trip, hold);
        YeuCauDiChung target = hold.getYeuCauMucTieu();
        if (target == null || target.getTrangThaiYeuCau() != TrangThaiYeuCau.ON_BOARD) throw invalidStoredPlan();

        TrangThaiYeuCau previousBookingState = target.abortForSafety(trip);
        trip.giamMotKhachDangTrenXe();
        int invalidated = resolveActiveCredentials(target, hold, occurredAt);
        cancelUnfinishedTargetStops(target, hold, occurredAt);

        Duration holdDuration = Duration.between(hold.getKhoiTaoLuc(), occurredAt);
        if (holdDuration.isNegative()) throw invalidStoredPlan();
        if (!holdDuration.isZero()) extendUnrelatedWaitingDeadlines(trip, target, hold, holdDuration, occurredAt);

        hold.hoanTatXuongXeAnToan(driver, occurredAt, position, trip.getSoKhachThucTe(), invalidated);
        appendBookingHistory(target, previousBookingState, driver, occurredAt, hold);
        TrangThaiVanHanhChuyenDi previousTripState = trip.getTrangThaiVanHanh();
        trip.ketThucGiuAnToanVaTiepTuc();
        appendTripHistory(trip, previousTripState, trip.getTrangThaiVanHanh(), driver, occurredAt,
                "SAFETY_HOLD_RESUMED", hold);
        entityManager.persist(ThongBao.passengerSafetyParticipationAborted(hold, target));
        activePassengers(trip.getId()).forEach(b -> entityManager.persist(ThongBao.tripSafetyHoldResumed(hold, b.getHanhKhach())));
        entityManager.flush();
        return changed(hold, commonRealtimeRecipients(trip), participantRecipients(trip, target));
    }

    public TripSafetyInterventionCommitResult abortTripFromHold(
            ChuyenDi trip, CanThiepAnToanChuyenDi hold, NguoiDung driver, Instant occurredAt) {
        TripSafetyInterventionSnapshot existing = latestCompletedAbortForIncident(hold.getSuCoChuyenDi().getId());
        if (existing != null && trip.getTrangThaiVanHanh() == TrangThaiVanHanhChuyenDi.EMERGENCY_ABORTED) {
            return new TripSafetyInterventionCommitResult(existing, false, List.of(), List.of());
        }
        requireStoredPlanConsistent(trip);
        requireActiveHoldForTrip(trip, hold);
        hold.dungDoChuyenDiKetThuc(driver, occurredAt);
        return abortWholeTrip(trip, hold.getSuCoChuyenDi(), hold, driver, occurredAt);
    }

    public TripSafetyInterventionCommitResult abortTripBySafety(
            ChuyenDi trip, SuCoChuyenDi incident, NguoiDung actor, Instant occurredAt) {
        TripSafetyInterventionSnapshot existing = latestCompletedAbortForIncident(incident.getId());
        if (existing != null && trip.getTrangThaiVanHanh() == TrangThaiVanHanhChuyenDi.EMERGENCY_ABORTED) {
            return new TripSafetyInterventionCommitResult(existing, false, List.of(), List.of());
        }
        if (trip.getTrangThaiVanHanh() == TrangThaiVanHanhChuyenDi.EMERGENCY_ABORTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "TRIP_ALREADY_EMERGENCY_ABORTED",
                    "Trip đã emergency-abort bởi intervention khác.");
        }
        requireStoredPlanConsistent(trip);
        CanThiepAnToanChuyenDi hold = findActiveHold(trip.getId());
        if (hold != null) hold.dungDoChuyenDiKetThuc(actor, occurredAt);
        return abortWholeTrip(trip, incident, hold, actor, occurredAt);
    }

    public CanThiepAnToanChuyenDi findActiveHold(Long tripId) {
        List<CanThiepAnToanChuyenDi> rows = entityManager.createQuery(
                        "select c from CanThiepAnToanChuyenDi c left join fetch c.yeuCauMucTieu b "
                                + "join fetch c.suCoChuyenDi i where c.chuyenDi.id=:tripId "
                                + "and c.loaiCanThiep=:type and c.trangThaiCanThiep=:state order by c.thuTuCanThiep",
                        CanThiepAnToanChuyenDi.class)
                .setParameter("tripId", tripId).setParameter("type", LoaiCanThiepAnToan.GIU_DE_XUONG_XE_AN_TOAN)
                .setParameter("state", TrangThaiCanThiepAnToan.DANG_THUC_HIEN).getResultList();
        if (rows.size() > 1) throw invalidStoredPlan();
        return rows.isEmpty() ? null : rows.get(0);
    }

    public TripSafetyInterventionSnapshot completedTripAbortForIncidentAndActor(Long incidentId, Long actorId) {
        if (incidentId == null || actorId == null) return null;
        return entityManager.createQuery(
                        "select c from CanThiepAnToanChuyenDi c join fetch c.chuyenDi t "
                                + "where c.suCoChuyenDi.id=:incidentId and c.nguoiKhoiTao.id=:actorId "
                                + "and c.loaiCanThiep=:type and c.trangThaiCanThiep=:state "
                                + "order by c.thuTuCanThiep desc", CanThiepAnToanChuyenDi.class)
                .setParameter("incidentId", incidentId).setParameter("actorId", actorId)
                .setParameter("type", LoaiCanThiepAnToan.HUY_CHUYEN_KHAN_CAP)
                .setParameter("state", TrangThaiCanThiepAnToan.HOAN_TAT)
                .setMaxResults(1).getResultList().stream().findFirst().map(this::snapshot).orElse(null);
    }

    public TripSafetyInterventionSnapshot latestForIncident(Long incidentId) {
        return entityManager.createQuery(
                        "select c from CanThiepAnToanChuyenDi c left join fetch c.yeuCauMucTieu b "
                                + "join fetch c.chuyenDi t where c.suCoChuyenDi.id=:id order by c.thuTuCanThiep desc",
                        CanThiepAnToanChuyenDi.class)
                .setParameter("id", incidentId).setMaxResults(1).getResultList().stream()
                .findFirst().map(this::snapshot).orElse(null);
    }

    public TripSafetyInterventionSnapshot snapshot(CanThiepAnToanChuyenDi c) {
        Point p = c.getToaDoXuongXeKhanCap();
        String targetStatus = c.getYeuCauMucTieu() == null ? null : c.getYeuCauMucTieu().getTrangThaiYeuCau().name();
        Instant changedAt = c.getKetThucLuc() != null ? c.getKetThucLuc() : c.getKhoiTaoLuc();
        return new TripSafetyInterventionSnapshot(c.getId(), c.getSuCoChuyenDi().getId(), c.getChuyenDi().getId(),
                c.getLoaiCanThiep().name(), c.getTrangThaiCanThiep().name(), c.getChuyenDi().getTrangThaiVanHanh().name(),
                c.getYeuCauMucTieu() == null ? null : c.getYeuCauMucTieu().getId(), targetStatus,
                c.getChuyenDi().getSoKhachThucTe(), c.getXuongXeKhanCapLuc(),
                p == null ? null : BigDecimal.valueOf(p.getY()), p == null ? null : BigDecimal.valueOf(p.getX()),
                changedAt, changeType(c));
    }

    public void requireStoredPlanConsistent(ChuyenDi trip) {
        if (trip == null || trip.getId() == null || trip.getBatDauLuc() == null || trip.getSoKhachThucTe() == null
                || trip.getSoKhachKeHoach() == null || trip.getSoKhachKeHoach() <= 0
                || trip.getLoTrinhChiaSe() == null || trip.getLoTrinhChiaSe().getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || trip.getLoTrinhChiaSe().getChuyenDi() == null
                || !Objects.equals(trip.getLoTrinhChiaSe().getChuyenDi().getId(), trip.getId())) throw invalidStoredPlan();

        List<YeuCauDiChung> bookings = entityManager.createQuery(
                        "select b from YeuCauDiChung b where b.chuyenDi.id=:tripId order by b.id", YeuCauDiChung.class)
                .setParameter("tripId", trip.getId()).getResultList();
        if (bookings.size() != trip.getSoKhachKeHoach()) throw invalidStoredPlan();
        long onBoard = bookings.stream().filter(b -> b.getTrangThaiYeuCau() == TrangThaiYeuCau.ON_BOARD).count();
        if (trip.getSoKhachThucTe().longValue() != onBoard) throw invalidStoredPlan();
        for (YeuCauDiChung booking : bookings) {
            if (booking.getLoTrinhChiaSe() == null || !Objects.equals(booking.getLoTrinhChiaSe().getId(), trip.getLoTrinhChiaSe().getId())) {
                throw invalidStoredPlan();
            }
        }

        List<DiemDungHanhTrinh> stops = entityManager.createQuery(
                        "select s from DiemDungHanhTrinh s left join fetch s.yeuCauDiChung b where s.chuyenDi.id=:tripId order by s.thuTu,s.id",
                        DiemDungHanhTrinh.class).setParameter("tripId", trip.getId()).getResultList();
        if (stops.size() != 2 + 2 * bookings.size()) throw invalidStoredPlan();
        long driverStarts = stops.stream().filter(s -> s.getLoaiDiemDung()==LoaiDiemDung.DRIVER_START).count();
        long driverEnds = stops.stream().filter(s -> s.getLoaiDiemDung()==LoaiDiemDung.DRIVER_END).count();
        if (driverStarts != 1 || driverEnds != 1) throw invalidStoredPlan();
        DiemDungHanhTrinh driverStart = stops.stream().filter(s -> s.getLoaiDiemDung()==LoaiDiemDung.DRIVER_START).findFirst().orElseThrow(TripSafetyInterventionJpaSupport::invalidStoredPlan);
        if (driverStart.getTrangThaiDiemDung()!=TrangThaiDiemDung.COMPLETED || driverStart.getHoanThanhLuc()==null) throw invalidStoredPlan();
        for (YeuCauDiChung booking : bookings) {
            long pickup = stops.stream().filter(s -> s.getYeuCauDiChung()!=null && Objects.equals(s.getYeuCauDiChung().getId(),booking.getId()) && s.getLoaiDiemDung()==LoaiDiemDung.PICKUP).count();
            long dropoff = stops.stream().filter(s -> s.getYeuCauDiChung()!=null && Objects.equals(s.getYeuCauDiChung().getId(),booking.getId()) && s.getLoaiDiemDung()==LoaiDiemDung.DROPOFF).count();
            if (pickup != 1 || dropoff != 1) throw invalidStoredPlan();
        }

        CanThiepAnToanChuyenDi hold = findActiveHold(trip.getId());
        if (trip.getTrangThaiVanHanh() == TrangThaiVanHanhChuyenDi.IN_PROGRESS) {
            if (hold != null || trip.getDongBangLuc() != null || trip.getLyDoDongBang() != null) throw invalidStoredPlan();
        } else if (trip.getTrangThaiVanHanh() == TrangThaiVanHanhChuyenDi.SECURITY_FROZEN) {
            if (hold == null || trip.getDongBangLuc() == null || !Objects.equals(trip.getDongBangLuc(), hold.getKhoiTaoLuc())
                    || !Objects.equals(trip.getLyDoDongBang(), hold.getLyDoAnToan())
                    || hold.getYeuCauMucTieu() == null || hold.getYeuCauMucTieu().getTrangThaiYeuCau() != TrangThaiYeuCau.ON_BOARD) {
                throw invalidStoredPlan();
            }
        } else {
            throw new BusinessException(HttpStatus.CONFLICT, "TRIP_NOT_OPERATIONAL_FOR_SAFETY", "Trip không còn operational cho Safety intervention.");
        }
    }

    private TripSafetyInterventionCommitResult abortParticipant(ChuyenDi trip, SuCoChuyenDi incident, YeuCauDiChung target,
                                                                 NguoiDung actor, Instant occurredAt, boolean keepFrozen) {
        int before = trip.getSoKhachThucTe();
        CanThiepAnToanChuyenDi intervention = CanThiepAnToanChuyenDi.hoanTatNgay(trip, incident, target,
                nextInterventionSequence(trip.getId()), LoaiCanThiepAnToan.LOAI_HANH_KHACH, actor, occurredAt,
                PARTICIPANT_ABORT_REASON, before, before, 0);
        entityManager.persist(intervention); entityManager.flush();
        TrangThaiYeuCau previous = target.abortForSafety(trip);
        int invalidated = resolveActiveCredentials(target, intervention, occurredAt);
        intervention.setSoXacThucLenXeVoHieuHoa(invalidated);
        cancelUnfinishedTargetStops(target, intervention, occurredAt);
        appendBookingHistory(target, previous, actor, occurredAt, intervention);
        entityManager.persist(ThongBao.passengerSafetyParticipationAborted(intervention, target));
        entityManager.flush();
        if (keepFrozen && trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN) throw invalidStoredPlan();
        return changed(intervention, List.of(), participantRecipients(trip, target));
    }

    private TripSafetyInterventionCommitResult startSafeExitHold(ChuyenDi trip, SuCoChuyenDi incident, YeuCauDiChung target,
                                                                  NguoiDung actor, Instant occurredAt) {
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS || findActiveHold(trip.getId()) != null) throw invalidStoredPlan();
        CanThiepAnToanChuyenDi hold = CanThiepAnToanChuyenDi.batDauGiuDeXuongXeAnToan(trip, incident, target,
                nextInterventionSequence(trip.getId()), actor, occurredAt, HOLD_REASON, trip.getSoKhachThucTe());
        entityManager.persist(hold); entityManager.flush();
        TrangThaiVanHanhChuyenDi previous = trip.getTrangThaiVanHanh();
        trip.batDauGiuAnToan(occurredAt, HOLD_REASON);
        appendTripHistory(trip, previous, trip.getTrangThaiVanHanh(), actor, occurredAt, "SAFETY_HOLD_STARTED", hold);
        activePassengers(trip.getId()).forEach(b -> entityManager.persist(ThongBao.tripSafetyHoldStarted(hold, b.getHanhKhach())));
        entityManager.flush();
        return changed(hold, commonRealtimeRecipients(trip), List.of());
    }

    private TripSafetyInterventionCommitResult abortWholeTrip(ChuyenDi trip, SuCoChuyenDi incident,
                                                               CanThiepAnToanChuyenDi stoppedHold,
                                                               NguoiDung actor, Instant occurredAt) {
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                && trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN) {
            throw new BusinessException(HttpStatus.CONFLICT, "TRIP_NOT_OPERATIONAL_FOR_SAFETY", "Trip không còn operational để emergency-abort.");
        }
        List<YeuCauDiChung> preActive = activePassengers(trip.getId());
        List<Long> preRealtime = recipientIdsWithDriver(trip, preActive);
        int before = trip.getSoKhachThucTe();
        CanThiepAnToanChuyenDi abort = CanThiepAnToanChuyenDi.hoanTatNgay(trip, incident, null,
                nextInterventionSequence(trip.getId()), LoaiCanThiepAnToan.HUY_CHUYEN_KHAN_CAP, actor, occurredAt,
                TRIP_ABORT_REASON, before, 0, 0);
        entityManager.persist(abort); entityManager.flush();

        int invalidated = 0;
        for (YeuCauDiChung booking : preActive) {
            TrangThaiYeuCau previous = booking.abortForSafety(trip);
            appendBookingHistory(booking, previous, actor, occurredAt, abort);
            invalidated += resolveActiveCredentials(booking, abort, occurredAt);
        }
        abort.setSoXacThucLenXeVoHieuHoa(invalidated);
        cancelAllUnfinishedStops(trip, abort, occurredAt);
        TrangThaiVanHanhChuyenDi previousTrip = trip.getTrangThaiVanHanh();
        trip.huyKhanCap(occurredAt);
        appendTripHistory(trip, previousTrip, trip.getTrangThaiVanHanh(), actor, occurredAt, "TRIP_EMERGENCY_ABORTED", abort);
        requireDriverAndPreActiveNotifications(trip, preActive, abort);
        entityManager.flush();
        return changed(abort, preRealtime, List.of());
    }

    private int resolveActiveCredentials(YeuCauDiChung booking, CanThiepAnToanChuyenDi intervention, Instant occurredAt) {
        List<ThongTinXacThucLenXe> credentials = entityManager.createQuery(
                        "select c from ThongTinXacThucLenXe c where c.yeuCauDiChung.id=:id and c.voHieuHoaLuc is null",
                        ThongTinXacThucLenXe.class).setParameter("id", booking.getId()).getResultList();
        if (credentials.size() > 1) throw invalidStoredPlan();
        for (ThongTinXacThucLenXe c : credentials) c.resolveForSafety(occurredAt, intervention);
        return credentials.size();
    }

    private void cancelUnfinishedTargetStops(YeuCauDiChung target, CanThiepAnToanChuyenDi intervention, Instant occurredAt) {
        List<DiemDungHanhTrinh> stops = entityManager.createQuery(
                        "select s from DiemDungHanhTrinh s where s.yeuCauDiChung.id=:id order by s.thuTu", DiemDungHanhTrinh.class)
                .setParameter("id", target.getId()).getResultList();
        for (DiemDungHanhTrinh stop : stops) if (UNFINISHED_STOP_STATES.contains(stop.getTrangThaiDiemDung())) {
            TrangThaiDiemDung before = stop.getTrangThaiDiemDung(); Instant deadline = stop.getHanChoLuc();
            stop.cancelForSafety();
            entityManager.persist(ChiTietCanThiepDiemDung.of(intervention, stop, before, stop.getTrangThaiDiemDung(), deadline, stop.getHanChoLuc(), occurredAt));
        }
    }

    private void cancelAllUnfinishedStops(ChuyenDi trip, CanThiepAnToanChuyenDi intervention, Instant occurredAt) {
        List<DiemDungHanhTrinh> stops = entityManager.createQuery(
                        "select s from DiemDungHanhTrinh s where s.chuyenDi.id=:id order by s.thuTu", DiemDungHanhTrinh.class)
                .setParameter("id", trip.getId()).getResultList();
        for (DiemDungHanhTrinh stop : stops) if (UNFINISHED_STOP_STATES.contains(stop.getTrangThaiDiemDung())) {
            TrangThaiDiemDung before = stop.getTrangThaiDiemDung(); Instant deadline = stop.getHanChoLuc();
            stop.cancelForSafety();
            entityManager.persist(ChiTietCanThiepDiemDung.of(intervention, stop, before, stop.getTrangThaiDiemDung(), deadline, stop.getHanChoLuc(), occurredAt));
        }
    }

    private void extendUnrelatedWaitingDeadlines(ChuyenDi trip, YeuCauDiChung target, CanThiepAnToanChuyenDi intervention,
                                                  Duration duration, Instant occurredAt) {
        List<DiemDungHanhTrinh> stops = entityManager.createQuery(
                        "select s from DiemDungHanhTrinh s left join fetch s.yeuCauDiChung b where s.chuyenDi.id=:id "
                                + "and s.loaiDiemDung=:type and s.trangThaiDiemDung=:state order by s.thuTu",
                        DiemDungHanhTrinh.class).setParameter("id", trip.getId()).setParameter("type", LoaiDiemDung.PICKUP)
                .setParameter("state", TrangThaiDiemDung.ARRIVED).getResultList();
        for (DiemDungHanhTrinh stop : stops) {
            YeuCauDiChung booking = stop.getYeuCauDiChung();
            if (booking == null || Objects.equals(booking.getId(), target.getId()) || !booking.getTrangThaiYeuCau().isActiveTripParticipant()) continue;
            Instant oldDeadline = stop.extendWaitingDeadlineForSafety(duration);
            entityManager.persist(ChiTietCanThiepDiemDung.of(intervention, stop, TrangThaiDiemDung.ARRIVED,
                    TrangThaiDiemDung.ARRIVED, oldDeadline, stop.getHanChoLuc(), occurredAt));
        }
    }

    private void appendTripHistory(ChuyenDi trip, TrangThaiVanHanhChuyenDi previous, TrangThaiVanHanhChuyenDi next,
                                   NguoiDung actor, Instant occurredAt, String reason, CanThiepAnToanChuyenDi intervention) {
        Long max = entityManager.createQuery("select coalesce(max(h.sequence),0) from NhatKyTrangThaiChuyenDi h where h.chuyenDi.id=:id", Long.class)
                .setParameter("id", trip.getId()).getSingleResult();
        entityManager.persist(NhatKyTrangThaiChuyenDi.safetyTransition(trip, actor, occurredAt, (max == null ? 0 : max) + 1,
                previous, next, reason, intervention));
    }

    private void appendBookingHistory(YeuCauDiChung booking, TrangThaiYeuCau previous, NguoiDung actor,
                                      Instant occurredAt, CanThiepAnToanChuyenDi intervention) {
        Long max = entityManager.createQuery("select coalesce(max(h.sequence),0) from NhatKyTrangThaiYeuCau h where h.yeuCauDiChung.id=:id", Long.class)
                .setParameter("id", booking.getId()).getSingleResult();
        entityManager.persist(NhatKyTrangThaiYeuCau.safetyAborted(booking, actor, occurredAt, (max == null ? 0 : max) + 1,
                previous, intervention));
    }

    private long nextInterventionSequence(Long tripId) {
        Long max = entityManager.createQuery("select coalesce(max(c.thuTuCanThiep),0) from CanThiepAnToanChuyenDi c where c.chuyenDi.id=:id", Long.class)
                .setParameter("id", tripId).getSingleResult();
        return (max == null ? 0 : max) + 1;
    }

    private List<YeuCauDiChung> activePassengers(Long tripId) {
        return entityManager.createQuery(
                        "select b from YeuCauDiChung b join fetch b.hanhKhach p where b.chuyenDi.id=:id "
                                + "and b.trangThaiYeuCau in :states order by b.id", YeuCauDiChung.class)
                .setParameter("id", tripId).setParameter("states", ACTIVE_BOOKING_STATES).getResultList();
    }

    private YeuCauDiChung findPassengerBookingOrNull(Long tripId, Long userId) {
        List<YeuCauDiChung> rows = entityManager.createQuery(
                        "select b from YeuCauDiChung b join fetch b.hanhKhach p where b.chuyenDi.id=:tripId and p.id=:userId",
                        YeuCauDiChung.class).setParameter("tripId", tripId).setParameter("userId", userId).getResultList();
        if (rows.size() > 1) throw invalidStoredPlan();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private NguoiDung requireDriver(ChuyenDi trip) {
        NguoiDung d = trip.getLoTrinhChiaSe() == null ? null : trip.getLoTrinhChiaSe().getTaiXe();
        if (d == null || d.getId() == null) throw invalidStoredPlan();
        return d;
    }

    private List<Long> commonRealtimeRecipients(ChuyenDi trip) {
        return recipientIdsWithDriver(trip, activePassengers(trip.getId()));
    }

    private List<Long> recipientIdsWithDriver(ChuyenDi trip, List<YeuCauDiChung> bookings) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>(); ids.add(requireDriver(trip).getId());
        bookings.forEach(b -> ids.add(b.getHanhKhach().getId())); return List.copyOf(ids);
    }

    private List<Long> participantRecipients(ChuyenDi trip, YeuCauDiChung target) {
        return List.of(requireDriver(trip).getId(), target.getHanhKhach().getId());
    }

    private void requireDriverAndPreActiveNotifications(ChuyenDi trip, List<YeuCauDiChung> preActive,
                                                         CanThiepAnToanChuyenDi abort) {
        entityManager.persist(ThongBao.tripEmergencyAborted(abort, requireDriver(trip)));
        for (YeuCauDiChung b : preActive) entityManager.persist(ThongBao.tripEmergencyAborted(abort, b.getHanhKhach()));
    }

    private TripSafetyInterventionCommitResult changed(CanThiepAnToanChuyenDi c, List<Long> common, List<Long> participant) {
        return new TripSafetyInterventionCommitResult(snapshot(c), true, common, participant);
    }

    private TripSafetyInterventionCommitResult noOp(CanThiepAnToanChuyenDi c) {
        return new TripSafetyInterventionCommitResult(snapshot(c), false, List.of(), List.of());
    }

    private TripSafetyInterventionSnapshot latestCompletedAbortForIncident(Long incidentId) {
        return entityManager.createQuery(
                        "select c from CanThiepAnToanChuyenDi c join fetch c.chuyenDi t where c.suCoChuyenDi.id=:id "
                                + "and c.loaiCanThiep=:type and c.trangThaiCanThiep=:state order by c.thuTuCanThiep desc",
                        CanThiepAnToanChuyenDi.class).setParameter("id", incidentId)
                .setParameter("type", LoaiCanThiepAnToan.HUY_CHUYEN_KHAN_CAP).setParameter("state", TrangThaiCanThiepAnToan.HOAN_TAT)
                .setMaxResults(1).getResultList().stream().findFirst().map(this::snapshot).orElse(null);
    }

    private void requireActiveHoldForTrip(ChuyenDi trip, CanThiepAnToanChuyenDi hold) {
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN || hold == null || !hold.dangThucHien()
                || hold.getChuyenDi() == null || !Objects.equals(hold.getChuyenDi().getId(), trip.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "SAFETY_INTERVENTION_NOT_ACTIVE", "Safety hold không còn active.");
        }
        CanThiepAnToanChuyenDi current = findActiveHold(trip.getId());
        if (current == null || !Objects.equals(current.getId(), hold.getId())) throw invalidStoredPlan();
    }

    private static String changeType(CanThiepAnToanChuyenDi c) {
        if (c.getLoaiCanThiep() == LoaiCanThiepAnToan.LOAI_HANH_KHACH) return "PASSENGER_ABORTED";
        if (c.getLoaiCanThiep() == LoaiCanThiepAnToan.HUY_CHUYEN_KHAN_CAP) return "TRIP_EMERGENCY_ABORTED";
        if (c.getTrangThaiCanThiep() == TrangThaiCanThiepAnToan.DANG_THUC_HIEN) return "SAFE_EXIT_HOLD_STARTED";
        if (c.getTrangThaiCanThiep() == TrangThaiCanThiepAnToan.HOAN_TAT) return "SAFE_EXIT_HOLD_RESUMED";
        return "SAFE_EXIT_HOLD_STOPPED";
    }

    private static BusinessException invalidStoredPlan() {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_STORED_TRIP_PLAN",
                "Dữ liệu Trip/booking/stop/intervention không nhất quán cho Safety intervention.");
    }
}
