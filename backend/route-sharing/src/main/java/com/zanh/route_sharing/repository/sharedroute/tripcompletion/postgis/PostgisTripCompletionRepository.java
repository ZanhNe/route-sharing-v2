package com.zanh.route_sharing.repository.sharedroute.tripcompletion.postgis;

import com.zanh.route_sharing.domain.entity.*;
import com.zanh.route_sharing.domain.enums.*;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripcompletion.TripCompletionRepository;
import com.zanh.route_sharing.repository.sharedroute.tripcompletion.model.TripCompletionCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.tripcompletion.model.TripCompletionCommitResult;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.utils.time.TimePolicy;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Repository
public class PostgisTripCompletionRepository implements TripCompletionRepository {
    private static final Set<TrangThaiYeuCau> RESOLVED_BOOKING_STATES =
            EnumSet.of(TrangThaiYeuCau.COMPLETED, TrangThaiYeuCau.NO_SHOW, TrangThaiYeuCau.ABORTED);

    private final EntityManager entityManager;
    private final Clock clock;

    public PostgisTripCompletionRepository(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TripCompletionCommitResult commit(TripCompletionCommitCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockOwnedTrip(command.actorId(), command.tripId());
            requireSourceState(trip);
            requireTripInvariant(trip);
            requireNoActiveSafetyHold(trip.getId());

            // Critical chronology rule: obtain the business time only after the shared Trip lock is held.
            Instant completedAt = TimePolicy.now(clock);

            List<DiemDungHanhTrinh> stops = lockOrderedStops(trip.getId());
            TripStructure structure = requireTripStructure(trip, stops);
            List<YeuCauDiChung> bookings = findAttachedBookings(trip.getId());
            requirePassengerLifecycleResolution(trip, bookings);

            DiemDungHanhTrinh firstUnresolved = stops.stream()
                    .filter(stop -> stop.getTrangThaiDiemDung().isUnresolvedForTripProgression())
                    .findFirst()
                    .orElseThrow(PostgisTripCompletionRepository::invariantViolation);
            if (!Objects.equals(firstUnresolved.getId(), structure.driverEnd().getId())
                    || firstUnresolved.getLoaiDiemDung() != LoaiDiemDung.DRIVER_END) {
                throw nextStopNotDriverEnd();
            }
            requirePassengerOutcomeShapes(bookings, structure, completedAt);

            if (!isWithinDriverEndRadius(structure.driverEnd(), command.currentLocation())) {
                throw outsideEndRadius();
            }

            structure.driverEnd().completeDriverEnd(command.currentLocation(), completedAt);
            trip.completeNormally(completedAt);
            long historySequence = nextTripHistorySequence(trip.getId());
            entityManager.persist(NhatKyTrangThaiChuyenDi.driverCompleted(
                    trip, trip.getLoTrinhChiaSe().getTaiXe(), completedAt, historySequence));
            entityManager.flush();

            requirePostMutationInvariant(trip, structure.driverEnd(), completedAt);
            return new TripCompletionCommitResult(
                    trip.getId(),
                    trip.getLoTrinhChiaSe().getId(),
                    trip.getTrangThaiVanHanh(),
                    trip.getKetThucLuc(),
                    trip.getSoKhachThucTe(),
                    structure.driverEnd().getId(),
                    structure.driverEnd().getThuTu(),
                    structure.driverEnd().getTrangThaiDiemDung(),
                    structure.driverEnd().getHoanThanhLuc());
        } catch (BusinessException exception) {
            throw exception;
        } catch (PessimisticLockException | LockTimeoutException | OptimisticLockException exception) {
            throw concurrentModification();
        } catch (ConstraintViolationException exception) {
            throw dataIntegrityViolation();
        } catch (PersistenceException exception) {
            throw dataIntegrityViolation();
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw invariantViolation();
        }
    }

    private ChuyenDi lockOwnedTrip(Long actorId, Long tripId) {
        try {
            return entityManager.createQuery(
                    "select trip from ChuyenDi trip "
                            + "join fetch trip.loTrinhChiaSe route "
                            + "join fetch route.taiXe driver "
                            + "where trip.id=:tripId and driver.id=:actorId", ChuyenDi.class)
                    .setParameter("tripId", tripId)
                    .setParameter("actorId", actorId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultList().stream().findFirst()
                    .orElseThrow(PostgisTripCompletionRepository::notFound);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private List<DiemDungHanhTrinh> lockOrderedStops(Long tripId) {
        return entityManager.createQuery(
                        "select stop from DiemDungHanhTrinh stop "
                                + "left join fetch stop.yeuCauDiChung booking "
                                + "where stop.chuyenDi.id=:tripId order by stop.thuTu asc, stop.id asc",
                        DiemDungHanhTrinh.class)
                .setParameter("tripId", tripId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    private List<YeuCauDiChung> findAttachedBookings(Long tripId) {
        return entityManager.createQuery(
                        "select booking from YeuCauDiChung booking "
                                + "join fetch booking.loTrinhChiaSe route "
                                + "where booking.chuyenDi.id=:tripId order by booking.id asc",
                        YeuCauDiChung.class)
                .setParameter("tripId", tripId)
                .getResultList();
    }

    private static void requireSourceState(ChuyenDi trip) {
        if (trip.getTrangThaiVanHanh() == TrangThaiVanHanhChuyenDi.COMPLETED) {
            throw alreadyCompleted();
        }
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS) {
            throw notInProgress();
        }
    }

    private static void requireTripInvariant(ChuyenDi trip) {
        if (trip.getId() == null
                || trip.getBatDauLuc() == null
                || trip.getKetThucLuc() != null
                || trip.getSoKhachKeHoach() == null || trip.getSoKhachKeHoach() <= 0
                || trip.getSoKhachThucTe() == null || trip.getSoKhachThucTe() < 0
                || trip.getSoKhachThucTe() > trip.getSoKhachKeHoach()
                || trip.getDongBangLuc() != null || trip.getLyDoDongBang() != null
                || trip.getLoTrinhChiaSe() == null || trip.getLoTrinhChiaSe().getId() == null
                || trip.getLoTrinhChiaSe().getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || trip.getLoTrinhChiaSe().getChuyenDi() == null
                || !Objects.equals(trip.getId(), trip.getLoTrinhChiaSe().getChuyenDi().getId())
                || trip.getLoTrinhChiaSe().getTaiXe() == null || trip.getLoTrinhChiaSe().getTaiXe().getId() == null) {
            throw invariantViolation();
        }
    }

    private static TripStructure requireTripStructure(ChuyenDi trip, List<DiemDungHanhTrinh> stops) {
        int expected = 2 + 2 * trip.getSoKhachKeHoach();
        if (stops == null || stops.size() != expected) throw invariantViolation();

        Set<Integer> orders = new HashSet<>();
        int previousOrder = 0;
        List<DiemDungHanhTrinh> starts = new ArrayList<>();
        List<DiemDungHanhTrinh> ends = new ArrayList<>();
        Map<Long, DiemDungHanhTrinh> pickups = new HashMap<>();
        Map<Long, DiemDungHanhTrinh> dropoffs = new HashMap<>();

        for (DiemDungHanhTrinh stop : stops) {
            if (stop.getId() == null || stop.getThuTu() == null || stop.getThuTu() <= previousOrder
                    || !orders.add(stop.getThuTu()) || stop.getChuyenDi() == null
                    || !Objects.equals(stop.getChuyenDi().getId(), trip.getId())
                    || stop.getTrangThaiDiemDung() == null || stop.getLoaiDiemDung() == null
                    || stop.getToaDoKeHoach() == null || stop.getToaDoKeHoach().isEmpty()
                    || stop.getToaDoKeHoach().getSRID() != Wgs84Coordinates.SRID
                    || !Wgs84Coordinates.isValidLongitudeLatitude(stop.getToaDoKeHoach().getX(), stop.getToaDoKeHoach().getY())
                    || stop.getBanKinhXacDinhDaDenMet() == null || stop.getBanKinhXacDinhDaDenMet().signum() <= 0) {
                throw invariantViolation();
            }
            previousOrder = stop.getThuTu();
            if (stop.getLoaiDiemDung() == LoaiDiemDung.DRIVER_START) starts.add(stop);
            else if (stop.getLoaiDiemDung() == LoaiDiemDung.DRIVER_END) ends.add(stop);
            else {
                if (stop.getYeuCauDiChung() == null || stop.getYeuCauDiChung().getId() == null) throw invariantViolation();
                Map<Long, DiemDungHanhTrinh> target = stop.getLoaiDiemDung() == LoaiDiemDung.PICKUP ? pickups : dropoffs;
                if (target.put(stop.getYeuCauDiChung().getId(), stop) != null) throw invariantViolation();
            }
        }
        if (starts.size() != 1 || ends.size() != 1 || pickups.size() != trip.getSoKhachKeHoach()
                || dropoffs.size() != trip.getSoKhachKeHoach()) throw invariantViolation();

        DiemDungHanhTrinh start = starts.get(0);
        DiemDungHanhTrinh end = ends.get(0);
        if (stops.get(0) != start || stops.get(stops.size() - 1) != end
                || start.getYeuCauDiChung() != null || end.getYeuCauDiChung() != null
                || start.getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                || start.getToaDoThucTe() == null || start.getToaDoThucTe().isEmpty()
                || start.getToaDoThucTe().getSRID() != Wgs84Coordinates.SRID
                || !Wgs84Coordinates.isValidLongitudeLatitude(start.getToaDoThucTe().getX(), start.getToaDoThucTe().getY())
                || start.getHoanThanhLuc() == null || !start.getHoanThanhLuc().equals(trip.getBatDauLuc())
                || start.getDenGanLuc() != null || start.getDenLuc() != null || start.getBatDauChoLuc() != null || start.getHanChoLuc() != null
                || end.getTrangThaiDiemDung() != TrangThaiDiemDung.PENDING
                || end.getToaDoThucTe() != null || end.getHoanThanhLuc() != null
                || end.getDenGanLuc() != null || end.getDenLuc() != null || end.getBatDauChoLuc() != null || end.getHanChoLuc() != null) {
            throw invariantViolation();
        }
        return new TripStructure(start, end, Map.copyOf(pickups), Map.copyOf(dropoffs));
    }

    private static void requirePassengerLifecycleResolution(ChuyenDi trip, List<YeuCauDiChung> bookings) {
        if (bookings == null || bookings.size() != trip.getSoKhachKeHoach()) throw invariantViolation();
        long onBoardCount = bookings.stream().filter(b -> b.getTrangThaiYeuCau() == TrangThaiYeuCau.ON_BOARD).count();
        if (onBoardCount != trip.getSoKhachThucTe()) throw invariantViolation();
        if (onBoardCount > 0) throw hasOnboardPassengers();

        for (YeuCauDiChung booking : bookings) {
            if (booking.getId() == null || booking.getChuyenDi() == null
                    || !Objects.equals(booking.getChuyenDi().getId(), trip.getId())
                    || booking.getLoTrinhChiaSe() == null
                    || !Objects.equals(booking.getLoTrinhChiaSe().getId(), trip.getLoTrinhChiaSe().getId())) {
                throw invariantViolation();
            }
            if (!RESOLVED_BOOKING_STATES.contains(booking.getTrangThaiYeuCau())) {
                if (booking.getTrangThaiYeuCau() == TrangThaiYeuCau.ACCEPTED) {
                    throw unresolvedPassengerObligations();
                }
                throw invariantViolation();
            }
        }
    }

    private static void requirePassengerOutcomeShapes(
            List<YeuCauDiChung> bookings, TripStructure structure, Instant completedAt) {
        for (YeuCauDiChung booking : bookings) {
            DiemDungHanhTrinh pickup = structure.pickups().get(booking.getId());
            DiemDungHanhTrinh dropoff = structure.dropoffs().get(booking.getId());
            if (pickup == null || dropoff == null) throw invariantViolation();
            requirePassengerOutcomeShape(booking, pickup, dropoff, completedAt);
        }
    }

    private static void requirePassengerOutcomeShape(
            YeuCauDiChung booking, DiemDungHanhTrinh pickup, DiemDungHanhTrinh dropoff, Instant completedAt) {
        if (booking.getTrangThaiYeuCau() == TrangThaiYeuCau.COMPLETED) {
            if (booking.getChapNhanLuc() == null || booking.getLenXeLuc() == null || booking.getXuongXeLuc() == null
                    || booking.getKhongDenLuc() != null
                    || booking.getLenXeLuc().isBefore(booking.getChapNhanLuc())
                    || booking.getXuongXeLuc().isBefore(booking.getLenXeLuc())
                    || pickup.getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED || pickup.getHoanThanhLuc() == null
                    || !pickup.getHoanThanhLuc().equals(booking.getLenXeLuc())
                    || pickup.getDenLuc() == null || pickup.getBatDauChoLuc() == null || pickup.getHanChoLuc() == null
                    || !pickup.getDenLuc().equals(pickup.getBatDauChoLuc())
                    || pickup.getHoanThanhLuc().isBefore(pickup.getDenLuc())
                    || dropoff.getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED || dropoff.getToaDoThucTe() == null
                    || dropoff.getDenLuc() == null || dropoff.getHoanThanhLuc() == null
                    || dropoff.getBatDauChoLuc() != null || dropoff.getHanChoLuc() != null
                    || !dropoff.getHoanThanhLuc().equals(booking.getXuongXeLuc())
                    || dropoff.getDenLuc().isBefore(booking.getLenXeLuc())
                    || dropoff.getHoanThanhLuc().isBefore(dropoff.getDenLuc())
                    || booking.getXuongXeLuc().isAfter(completedAt)) {
                throw invariantViolation();
            }
            return;
        }
        if (booking.getTrangThaiYeuCau() == TrangThaiYeuCau.NO_SHOW) {
            if (booking.getChapNhanLuc() == null || booking.getKhongDenLuc() == null
                    || booking.getLenXeLuc() != null || booking.getXuongXeLuc() != null
                    || booking.getKhongDenLuc().isBefore(booking.getChapNhanLuc())
                    || pickup.getTrangThaiDiemDung() != TrangThaiDiemDung.SKIPPED
                    || pickup.getDenLuc() == null || pickup.getBatDauChoLuc() == null || pickup.getHanChoLuc() == null
                    || !pickup.getDenLuc().equals(pickup.getBatDauChoLuc())
                    || pickup.getHanChoLuc().isBefore(pickup.getDenLuc())
                    || booking.getKhongDenLuc().isBefore(pickup.getHanChoLuc())
                    || pickup.getHoanThanhLuc() != null
                    || booking.getKhongDenLuc().isAfter(completedAt)
                    || dropoff.getTrangThaiDiemDung() != TrangThaiDiemDung.SKIPPED
                    || hasAnyOperationalEvidence(dropoff)) {
                throw invariantViolation();
            }
            return;
        }
        if (booking.getTrangThaiYeuCau() == TrangThaiYeuCau.ABORTED) {
            if (booking.getChapNhanLuc() == null || booking.getKhongDenLuc() != null || booking.getXuongXeLuc() != null) {
                throw invariantViolation();
            }
            if (booking.getLenXeLuc() == null) {
                if (pickup.getTrangThaiDiemDung() != TrangThaiDiemDung.CANCELLED
                        || dropoff.getTrangThaiDiemDung() != TrangThaiDiemDung.CANCELLED
                        || pickup.getHoanThanhLuc() != null || dropoff.getHoanThanhLuc() != null) {
                    throw invariantViolation();
                }
            } else {
                if (booking.getLenXeLuc().isBefore(booking.getChapNhanLuc())
                        || booking.getLenXeLuc().isAfter(completedAt)
                        || pickup.getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                        || pickup.getHoanThanhLuc() == null || !pickup.getHoanThanhLuc().equals(booking.getLenXeLuc())
                        || pickup.getDenLuc() == null || pickup.getBatDauChoLuc() == null || pickup.getHanChoLuc() == null
                        || !pickup.getDenLuc().equals(pickup.getBatDauChoLuc())
                        || pickup.getHoanThanhLuc().isBefore(pickup.getDenLuc())
                        || dropoff.getTrangThaiDiemDung() != TrangThaiDiemDung.CANCELLED
                        || dropoff.getHoanThanhLuc() != null) {
                    throw invariantViolation();
                }
            }
            return;
        }
        throw unresolvedPassengerObligations();
    }

    private void requireNoActiveSafetyHold(Long tripId) {
        Long active = entityManager.createQuery(
                        "select count(intervention) from CanThiepAnToanChuyenDi intervention "
                                + "where intervention.chuyenDi.id=:tripId "
                                + "and intervention.loaiCanThiep=:type "
                                + "and intervention.trangThaiCanThiep=:state", Long.class)
                .setParameter("tripId", tripId)
                .setParameter("type", LoaiCanThiepAnToan.GIU_DE_XUONG_XE_AN_TOAN)
                .setParameter("state", TrangThaiCanThiepAnToan.DANG_THUC_HIEN)
                .getSingleResult();
        if (active == null || active != 0L) throw invariantViolation();
    }

    private boolean isWithinDriverEndRadius(DiemDungHanhTrinh driverEnd, Point actualLocation) {
        BigDecimal radius = driverEnd.getBanKinhXacDinhDaDenMet();
        if (radius == null || radius.signum() <= 0) throw invariantViolation();
        Object result = entityManager.createNativeQuery("""
                SELECT ST_DWithin(
                    stop.toa_do_ke_hoach::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    stop.ban_kinh_xac_dinh_da_den_met
                )
                FROM diem_dung_hanh_trinh stop
                WHERE stop.id=:stopId
                """)
                .setParameter("longitude", actualLocation.getX())
                .setParameter("latitude", actualLocation.getY())
                .setParameter("stopId", driverEnd.getId())
                .getSingleResult();
        return result instanceof Boolean value ? value : Boolean.parseBoolean(String.valueOf(result));
    }

    private long nextTripHistorySequence(Long tripId) {
        Long next = entityManager.createQuery(
                        "select coalesce(max(event.sequence),0)+1 from NhatKyTrangThaiChuyenDi event where event.chuyenDi.id=:tripId",
                        Long.class)
                .setParameter("tripId", tripId)
                .getSingleResult();
        return next == null ? 1L : next;
    }

    private static void requirePostMutationInvariant(ChuyenDi trip, DiemDungHanhTrinh end, Instant completedAt) {
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.COMPLETED
                || !completedAt.equals(trip.getKetThucLuc())
                || trip.getSoKhachThucTe() != 0
                || trip.getLoTrinhChiaSe().getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || end.getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                || end.getToaDoThucTe() == null
                || !completedAt.equals(end.getHoanThanhLuc())) {
            throw invariantViolation();
        }
    }

    private static boolean hasAnyOperationalEvidence(DiemDungHanhTrinh stop) {
        return stop.getToaDoThucTe() != null || stop.getDenGanLuc() != null || stop.getDenLuc() != null
                || stop.getBatDauChoLuc() != null || stop.getHanChoLuc() != null || stop.getHoanThanhLuc() != null;
    }

    private static void requireCommand(TripCompletionCommitCommand command) {
        if (command == null || command.actorId() == null || command.actorId() <= 0
                || command.tripId() == null || command.tripId() <= 0
                || command.currentLocation() == null || command.currentLocation().isEmpty()
                || command.currentLocation().getSRID() != Wgs84Coordinates.SRID
                || !Wgs84Coordinates.isValidLongitudeLatitude(command.currentLocation().getX(), command.currentLocation().getY())) {
            throw new IllegalArgumentException("TripCompletionCommitCommand không hợp lệ.");
        }
    }

    private record TripStructure(
            DiemDungHanhTrinh driverStart,
            DiemDungHanhTrinh driverEnd,
            Map<Long, DiemDungHanhTrinh> pickups,
            Map<Long, DiemDungHanhTrinh> dropoffs) {
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi.");
    }
    private static BusinessException alreadyCompleted() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_ALREADY_COMPLETED", "Chuyến đi đã được kết thúc bình thường trước đó.");
    }
    private static BusinessException notInProgress() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_NOT_IN_PROGRESS", "Chuyến đi phải ở trạng thái IN_PROGRESS để kết thúc bình thường.");
    }
    private static BusinessException hasOnboardPassengers() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_HAS_ONBOARD_PASSENGERS", "Chuyến đi vẫn còn hành khách đang trên xe.");
    }
    private static BusinessException unresolvedPassengerObligations() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_PASSENGER_OBLIGATIONS_UNRESOLVED", "Chuyến đi vẫn còn nghĩa vụ hành khách chưa được giải quyết.");
    }
    private static BusinessException nextStopNotDriverEnd() {
        return new BusinessException(HttpStatus.CONFLICT, "NEXT_TRIP_STOP_NOT_DRIVER_END", "Điểm dừng hiện tại chưa phải điểm kết thúc của tài xế.");
    }
    private static BusinessException outsideEndRadius() {
        return new BusinessException(HttpStatus.CONFLICT, "DRIVER_OUTSIDE_END_RADIUS", "Tài xế chưa ở trong phạm vi điểm kết thúc chuyến.");
    }
    private static BusinessException invariantViolation() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "TRIP_COMPLETION_INVARIANT_VIOLATION", "Dữ liệu chuyến đi không nhất quán để kết thúc bình thường.");
    }
    private static BusinessException concurrentModification() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Chuyến đi vừa được thay đổi bởi thao tác khác. Vui lòng tải lại dữ liệu.");
    }
    private static BusinessException dataIntegrityViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Không thể kết thúc chuyến do xung đột dữ liệu.");
    }
}
