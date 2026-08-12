package com.zanh.route_sharing.repository.sharedroute.tripcancellation.jpa;

import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.DiemDungHanhTrinh;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiChuyenDi;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiLoTrinh;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.tripcancellation.TripCancellationRepository;
import com.zanh.route_sharing.repository.sharedroute.tripcancellation.model.TripCancellationCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.tripcancellation.model.TripCancellationCommitResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
public class JpaTripCancellationRepository implements TripCancellationRepository {

    private final EntityManager entityManager;

    public JpaTripCancellationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public TripCancellationCommitResult commit(TripCancellationCommitCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockOwnedTrip(command.actorId(), command.tripId());
            requireCancellableState(trip);
            requireFormedTripInvariant(trip);

            List<YeuCauDiChung> bookings = lockAttachedBookings(trip.getId());
            requireAttachedBookingInvariant(trip, bookings);

            List<DiemDungHanhTrinh> stops = lockTripStops(trip.getId());
            requireStopInvariant(trip, bookings, stops);

            LoTrinhChiaSe route = trip.getLoTrinhChiaSe();
            requireSeatInvariant(route, bookings.size());

            trip.cancelBeforeStart();

            List<Long> recipientUserIds = new ArrayList<>(bookings.size());
            List<Long> rideRequestIds = new ArrayList<>(bookings.size());
            for (YeuCauDiChung booking : bookings) {
                booking.cancelBecauseTripCancelledBeforeStart(trip, command.cancelledAt(), command.reason());
                route.restoreOneSeatAfterTripCancellationBeforeStart();
                entityManager.persist(NhatKyTrangThaiYeuCau.tripCancelledBeforeStart(
                        booking,
                        route.getTaiXe(),
                        command.cancelledAt(),
                        nextRequestHistorySequence(booking.getId())));
                recipientUserIds.add(booking.getHanhKhach().getId());
                rideRequestIds.add(booking.getId());
            }

            for (DiemDungHanhTrinh stop : stops) {
                stop.cancelBeforeStart();
            }

            route.cancelBecauseTripCancelledBeforeStart(command.cancelledAt(), command.reason());
            entityManager.persist(NhatKyTrangThaiChuyenDi.driverCancelledBeforeStart(
                    trip,
                    route.getTaiXe(),
                    command.cancelledAt(),
                    nextTripHistorySequence(trip.getId())));
            entityManager.persist(NhatKyTrangThaiLoTrinh.tripCancelledBeforeStart(
                    route,
                    route.getTaiXe(),
                    command.cancelledAt(),
                    nextRouteHistorySequence(route.getId())));

            for (YeuCauDiChung booking : bookings) {
                entityManager.persist(ThongBao.tripCancelledBeforeStart(booking, trip));
            }

            entityManager.flush();
            return new TripCancellationCommitResult(
                    trip.getId(),
                    route.getId(),
                    trip.getTrangThaiVanHanh(),
                    route.getTrangThaiLoTrinh(),
                    route.getHuyLuc(),
                    route.getLyDoHuy(),
                    bookings.size(),
                    recipientUserIds,
                    rideRequestIds);
        } catch (BusinessException exception) {
            throw exception;
        } catch (PessimisticLockException | LockTimeoutException | OptimisticLockException exception) {
            throw concurrentModification();
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
                                    + "where trip.id = :tripId and driver.id = :actorId",
                            ChuyenDi.class)
                    .setParameter("tripId", tripId)
                    .setParameter("actorId", actorId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElseThrow(JpaTripCancellationRepository::notFound);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private List<YeuCauDiChung> lockAttachedBookings(Long tripId) {
        try {
            return entityManager.createQuery(
                            "select request from YeuCauDiChung request "
                                    + "join fetch request.hanhKhach passenger "
                                    + "join fetch request.loTrinhChiaSe route "
                                    + "where request.chuyenDi.id = :tripId "
                                    + "order by request.id asc",
                            YeuCauDiChung.class)
                    .setParameter("tripId", tripId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private List<DiemDungHanhTrinh> lockTripStops(Long tripId) {
        try {
            return entityManager.createQuery(
                            "select stop from DiemDungHanhTrinh stop "
                                    + "where stop.chuyenDi.id = :tripId "
                                    + "order by stop.thuTu asc, stop.id asc",
                            DiemDungHanhTrinh.class)
                    .setParameter("tripId", tripId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private long nextTripHistorySequence(Long tripId) {
        Long next = entityManager.createQuery(
                        "select coalesce(max(event.sequence), 0) + 1 "
                                + "from NhatKyTrangThaiChuyenDi event "
                                + "where event.chuyenDi.id = :tripId",
                        Long.class)
                .setParameter("tripId", tripId)
                .getSingleResult();
        return next == null ? 1L : next;
    }

    private long nextRouteHistorySequence(Long routeId) {
        Long next = entityManager.createQuery(
                        "select coalesce(max(event.sequence), 0) + 1 "
                                + "from NhatKyTrangThaiLoTrinh event "
                                + "where event.loTrinhChiaSe.id = :routeId",
                        Long.class)
                .setParameter("routeId", routeId)
                .getSingleResult();
        return next == null ? 1L : next;
    }

    private long nextRequestHistorySequence(Long rideRequestId) {
        Long next = entityManager.createQuery(
                        "select coalesce(max(event.sequence), 0) + 1 "
                                + "from NhatKyTrangThaiYeuCau event "
                                + "where event.yeuCauDiChung.id = :rideRequestId",
                        Long.class)
                .setParameter("rideRequestId", rideRequestId)
                .getSingleResult();
        return next == null ? 1L : next;
    }

    private static void requireCancellableState(ChuyenDi trip) {
        if (trip.getTrangThaiVanHanh() == TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START) {
            throw alreadyCancelled();
        }
        if (trip.getBatDauLuc() != null || trip.getTrangThaiVanHanh() == TrangThaiVanHanhChuyenDi.IN_PROGRESS) {
            throw alreadyStarted();
        }
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.PREPARING) {
            throw notCancellable();
        }
    }

    private static void requireFormedTripInvariant(ChuyenDi trip) {
        LoTrinhChiaSe route = trip.getLoTrinhChiaSe();
        if (route == null
                || route.getId() == null
                || route.getTaiXe() == null
                || route.getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || route.getChuyenDi() == null
                || !Objects.equals(route.getChuyenDi().getId(), trip.getId())
                || route.getHuyLuc() != null
                || route.getLyDoHuy() != null
                || trip.getSoKhachKeHoach() == null
                || trip.getSoKhachKeHoach() <= 0
                || trip.getSoKhachThucTe() == null
                || trip.getSoKhachThucTe() != 0
                || trip.getBatDauLuc() != null) {
            throw invariantViolation();
        }
    }

    private static void requireAttachedBookingInvariant(ChuyenDi trip, List<YeuCauDiChung> bookings) {
        if (bookings.isEmpty() || bookings.size() != trip.getSoKhachKeHoach()) {
            throw invariantViolation();
        }
        Long routeId = trip.getLoTrinhChiaSe().getId();
        for (YeuCauDiChung booking : bookings) {
            if (booking.getId() == null
                    || booking.getTrangThaiYeuCau() != TrangThaiYeuCau.ACCEPTED
                    || booking.getChuyenDi() == null
                    || !Objects.equals(booking.getChuyenDi().getId(), trip.getId())
                    || booking.getLoTrinhChiaSe() == null
                    || !Objects.equals(booking.getLoTrinhChiaSe().getId(), routeId)
                    || booking.getHanhKhach() == null
                    || booking.getHanhKhach().getId() == null
                    || booking.getHuyLuc() != null
                    || booking.getLyDoHuy() != null) {
                throw invariantViolation();
            }
        }
    }

    private static void requireStopInvariant(
            ChuyenDi trip,
            List<YeuCauDiChung> bookings,
            List<DiemDungHanhTrinh> stops) {
        if (stops.size() != 2 + 2 * bookings.size()) {
            throw invariantViolation();
        }
        int expectedOrder = 1;
        int driverStarts = 0;
        int driverEnds = 0;
        Set<Long> pickupBookingIds = new HashSet<>();
        Set<Long> dropoffBookingIds = new HashSet<>();
        Set<Long> bookingIds = new HashSet<>();
        for (YeuCauDiChung booking : bookings) {
            bookingIds.add(booking.getId());
        }

        for (DiemDungHanhTrinh stop : stops) {
            if (stop.getId() == null
                    || stop.getChuyenDi() == null
                    || !Objects.equals(stop.getChuyenDi().getId(), trip.getId())
                    || stop.getThuTu() == null
                    || stop.getThuTu() != expectedOrder++
                    || stop.getTrangThaiDiemDung() != TrangThaiDiemDung.PENDING
                    || stop.getToaDoThucTe() != null
                    || stop.getDenGanLuc() != null
                    || stop.getDenLuc() != null
                    || stop.getBatDauChoLuc() != null
                    || stop.getHanChoLuc() != null
                    || stop.getHoanThanhLuc() != null) {
                throw invariantViolation();
            }
            if (stop.getLoaiDiemDung() == LoaiDiemDung.DRIVER_START) {
                driverStarts++;
                if (stop.getYeuCauDiChung() != null) {
                    throw invariantViolation();
                }
            } else if (stop.getLoaiDiemDung() == LoaiDiemDung.DRIVER_END) {
                driverEnds++;
                if (stop.getYeuCauDiChung() != null) {
                    throw invariantViolation();
                }
            } else {
                if (stop.getYeuCauDiChung() == null
                        || !bookingIds.contains(stop.getYeuCauDiChung().getId())) {
                    throw invariantViolation();
                }
                if (stop.getLoaiDiemDung() == LoaiDiemDung.PICKUP) {
                    if (!pickupBookingIds.add(stop.getYeuCauDiChung().getId())) {
                        throw invariantViolation();
                    }
                } else if (stop.getLoaiDiemDung() == LoaiDiemDung.DROPOFF) {
                    if (!dropoffBookingIds.add(stop.getYeuCauDiChung().getId())) {
                        throw invariantViolation();
                    }
                } else {
                    throw invariantViolation();
                }
            }
        }

        if (driverStarts != 1 || driverEnds != 1
                || !pickupBookingIds.equals(bookingIds)
                || !dropoffBookingIds.equals(bookingIds)) {
            throw invariantViolation();
        }
    }

    private static void requireSeatInvariant(LoTrinhChiaSe route, int attachedBookingCount) {
        if (route.getSoGheCungCap() == null
                || route.getSoGheConLai() == null
                || attachedBookingCount <= 0
                || route.getSoGheCungCap() < attachedBookingCount
                || route.getSoGheConLai() != route.getSoGheCungCap() - attachedBookingCount) {
            throw invariantViolation();
        }
    }

    private static void requireCommand(TripCancellationCommitCommand command) {
        if (command == null
                || command.actorId() == null || command.actorId() <= 0
                || command.tripId() == null || command.tripId() <= 0
                || command.cancelledAt() == null
                || command.reason() == null || command.reason().isBlank()
                || command.reason().trim().length() > 2000) {
            throw new IllegalArgumentException("TripCancellationCommitCommand không hợp lệ.");
        }
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi.");
    }

    private static BusinessException alreadyCancelled() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "TRIP_ALREADY_CANCELLED_BEFORE_START",
                "Chuyến đi đã được hủy trước khi bắt đầu.");
    }

    private static BusinessException alreadyStarted() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "TRIP_ALREADY_STARTED",
                "Chuyến đi đã được bắt đầu và không thể hủy theo quy trình trước Start.");
    }

    private static BusinessException notCancellable() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "TRIP_NOT_CANCELLABLE_BEFORE_START",
                "Trạng thái hiện tại của chuyến không cho phép hủy trước Start.");
    }

    private static BusinessException invariantViolation() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "TRIP_CANCELLATION_INVARIANT_VIOLATION",
                "Dữ liệu chuyến, lộ trình, booking hoặc điểm dừng không nhất quán để hủy trước Start.");
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "CONCURRENT_MODIFICATION",
                "Chuyến đi đã được xử lý đồng thời. Vui lòng tải lại dữ liệu.");
    }

    private static BusinessException dataIntegrityViolation() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "DATA_INTEGRITY_VIOLATION",
                "Không thể ghi nhận hủy chuyến do ràng buộc dữ liệu.");
    }
}
