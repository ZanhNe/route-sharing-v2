package com.zanh.route_sharing.repository.sharedroute.boarding.jpa;

import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.DiemDungHanhTrinh;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.ThongTinXacThucLenXe;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.boarding.TripBoardingRepository;
import com.zanh.route_sharing.repository.sharedroute.boarding.model.TripBoardingCommand;
import com.zanh.route_sharing.repository.sharedroute.boarding.model.TripBoardingCommitResult;
import com.zanh.route_sharing.security.boarding.BoardingCodeProtector;
import com.zanh.route_sharing.security.boarding.model.BoardingCodeBinding;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;

@Repository
public class JpaTripBoardingRepository implements TripBoardingRepository {
    private final EntityManager entityManager;
    private final BoardingCodeProtector protector;

    public JpaTripBoardingRepository(EntityManager entityManager, BoardingCodeProtector protector) {
        this.entityManager = entityManager;
        this.protector = protector;
    }

    @Override
    @Transactional
    public TripBoardingCommitResult commit(TripBoardingCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockOwnedTrip(command.actorId(), command.tripId());
            requireTripInvariant(trip);

            List<DiemDungHanhTrinh> stops = lockTripStops(trip.getId());
            requireDriverStartCompleted(stops);
            DiemDungHanhTrinh pickup = firstUnresolved(stops);
            requireCurrentPickup(pickup);

            YeuCauDiChung booking = lockBooking(trip.getId(), pickup);
            requireBookingInvariant(trip, pickup, booking);
            ThongTinXacThucLenXe credential = lockActiveCredential(trip, booking, pickup);

            BoardingCodeBinding binding = new BoardingCodeBinding(trip.getId(), booking.getId(), pickup.getId());
            String expectedCode = protector.reveal(credential.protectedCode(), binding);
            if (!constantTimeEquals(expectedCode, command.boardingCode())) {
                throw invalidBoardingCode();
            }

            booking.board(trip, command.boardedAt());
            pickup.completeArrivedPickup(command.boardedAt());
            trip.boardOnePassenger();
            credential.resolve(command.boardedAt());
            entityManager.persist(NhatKyTrangThaiYeuCau.boarded(
                    booking,
                    trip.getLoTrinhChiaSe().getTaiXe(),
                    command.boardedAt(),
                    nextRequestHistorySequence(booking.getId())));
            entityManager.persist(ThongBao.passengerBoarded(booking, trip, pickup));
            entityManager.flush();

            return new TripBoardingCommitResult(
                    trip.getId(), trip.getLoTrinhChiaSe().getId(), booking.getId(),
                    pickup.getId(), pickup.getThuTu(), booking.getTrangThaiYeuCau(),
                    pickup.getTrangThaiDiemDung(), command.boardedAt(), trip.getSoKhachThucTe(),
                    booking.getHanhKhach().getId());
        } catch (BusinessException exception) {
            throw exception;
        } catch (PessimisticLockException | LockTimeoutException | OptimisticLockException exception) {
            throw concurrentModification();
        } catch (PersistenceException exception) {
            throw invariantViolation();
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
                    .getResultList().stream().findFirst()
                    .orElseThrow(JpaTripBoardingRepository::notFound);
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

    private YeuCauDiChung lockBooking(Long tripId, DiemDungHanhTrinh pickup) {
        if (pickup.getYeuCauDiChung() == null || pickup.getYeuCauDiChung().getId() == null) {
            throw invariantViolation();
        }
        try {
            return entityManager.createQuery(
                            "select booking from YeuCauDiChung booking "
                                    + "join fetch booking.hanhKhach passenger "
                                    + "where booking.id = :bookingId and booking.chuyenDi.id = :tripId",
                            YeuCauDiChung.class)
                    .setParameter("bookingId", pickup.getYeuCauDiChung().getId())
                    .setParameter("tripId", tripId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultList().stream().findFirst()
                    .orElseThrow(JpaTripBoardingRepository::invariantViolation);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private ThongTinXacThucLenXe lockActiveCredential(
            ChuyenDi trip, YeuCauDiChung booking, DiemDungHanhTrinh pickup) {
        try {
            List<ThongTinXacThucLenXe> rows = entityManager.createQuery(
                            "select credential from ThongTinXacThucLenXe credential "
                                    + "where credential.diemDungHanhTrinh.id = :pickupStopId",
                            ThongTinXacThucLenXe.class)
                    .setParameter("pickupStopId", pickup.getId())
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
            if (rows.size() != 1 || !rows.get(0).isActive()) {
                throw credentialNotActive();
            }
            ThongTinXacThucLenXe credential = rows.get(0);
            if (credential.getChuyenDi() == null || credential.getYeuCauDiChung() == null
                    || credential.getDiemDungHanhTrinh() == null
                    || !Objects.equals(credential.getChuyenDi().getId(), trip.getId())
                    || !Objects.equals(credential.getYeuCauDiChung().getId(), booking.getId())
                    || !Objects.equals(credential.getDiemDungHanhTrinh().getId(), pickup.getId())) {
                throw credentialInvariant();
            }
            return credential;
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
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

    private static DiemDungHanhTrinh firstUnresolved(List<DiemDungHanhTrinh> stops) {
        return stops.stream()
                .filter(stop -> stop.getTrangThaiDiemDung().isUnresolvedForTripProgression())
                .findFirst()
                .orElseThrow(JpaTripBoardingRepository::noUnresolvedStop);
    }

    private static void requireTripInvariant(ChuyenDi trip) {
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS) {
            throw tripNotInProgress();
        }
        if (trip.getBatDauLuc() == null || trip.getLoTrinhChiaSe() == null
                || trip.getLoTrinhChiaSe().getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || trip.getSoKhachThucTe() == null || trip.getSoKhachKeHoach() == null
                || trip.getSoKhachThucTe() < 0 || trip.getSoKhachThucTe() > trip.getSoKhachKeHoach()) {
            throw invariantViolation();
        }
    }

    private static void requireDriverStartCompleted(List<DiemDungHanhTrinh> stops) {
        List<DiemDungHanhTrinh> starts = stops.stream()
                .filter(stop -> stop.getLoaiDiemDung() == LoaiDiemDung.DRIVER_START)
                .toList();
        if (starts.size() != 1 || starts.get(0).getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                || starts.get(0).getHoanThanhLuc() == null) {
            throw driverStartNotCompleted();
        }
    }

    private static void requireCurrentPickup(DiemDungHanhTrinh pickup) {
        if (pickup.getLoaiDiemDung() != LoaiDiemDung.PICKUP) {
            throw nextStopNotPickup();
        }
        if (pickup.getTrangThaiDiemDung() != TrangThaiDiemDung.ARRIVED) {
            throw pickupNotArrived();
        }
        if (pickup.getDenLuc() == null || pickup.getBatDauChoLuc() == null || pickup.getHanChoLuc() == null
                || !pickup.getDenLuc().equals(pickup.getBatDauChoLuc())
                || pickup.getHanChoLuc().isBefore(pickup.getDenLuc()) || pickup.getHoanThanhLuc() != null) {
            throw invariantViolation();
        }
    }

    private static void requireBookingInvariant(
            ChuyenDi trip, DiemDungHanhTrinh pickup, YeuCauDiChung booking) {
        if (booking.getTrangThaiYeuCau() != TrangThaiYeuCau.ACCEPTED) {
            throw bookingNotAccepted();
        }
        if (booking.getChuyenDi() == null || !Objects.equals(booking.getChuyenDi().getId(), trip.getId())
                || pickup.getYeuCauDiChung() == null
                || !Objects.equals(pickup.getYeuCauDiChung().getId(), booking.getId())
                || booking.getLenXeLuc() != null) {
            throw invariantViolation();
        }
    }

    private static boolean constantTimeEquals(String expected, String candidate) {
        if (expected == null || candidate == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII), candidate.getBytes(StandardCharsets.US_ASCII));
    }

    private static void requireCommand(TripBoardingCommand command) {
        if (command == null || command.actorId() == null || command.actorId() <= 0
                || command.tripId() == null || command.tripId() <= 0
                || command.boardedAt() == null
                || command.boardingCode() == null || !command.boardingCode().matches("[0-9]{6}")) {
            throw new IllegalArgumentException("TripBoardingCommand không hợp lệ.");
        }
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi.");
    }

    private static BusinessException tripNotInProgress() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_NOT_IN_PROGRESS", "Chuyến đi chưa ở trạng thái đang vận hành.");
    }

    private static BusinessException driverStartNotCompleted() {
        return new BusinessException(HttpStatus.CONFLICT, "DRIVER_START_NOT_COMPLETED", "Điểm DRIVER_START chưa được hoàn thành.");
    }

    private static BusinessException noUnresolvedStop() {
        return new BusinessException(HttpStatus.CONFLICT, "NO_UNRESOLVED_TRIP_STOP", "Không còn điểm dừng chưa giải quyết.");
    }

    private static BusinessException nextStopNotPickup() {
        return new BusinessException(HttpStatus.CONFLICT, "NEXT_TRIP_STOP_NOT_PICKUP", "Điểm dừng chưa giải quyết kế tiếp không phải pickup.");
    }

    private static BusinessException pickupNotArrived() {
        return new BusinessException(HttpStatus.CONFLICT, "PICKUP_NOT_ARRIVED", "Pickup hiện tại chưa ở trạng thái ARRIVED.");
    }

    private static BusinessException bookingNotAccepted() {
        return new BusinessException(HttpStatus.CONFLICT, "BOOKING_NOT_ACCEPTED", "Booking gắn với pickup không còn ACCEPTED.");
    }

    private static BusinessException credentialNotActive() {
        return new BusinessException(HttpStatus.CONFLICT, "BOARDING_CREDENTIAL_NOT_ACTIVE", "Boarding credential hiện không active.");
    }

    private static BusinessException invalidBoardingCode() {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_BOARDING_CODE", "Boarding code không chính xác.");
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Chuyến đi vừa được thay đổi đồng thời. Vui lòng tải lại dữ liệu.");
    }

    private static BusinessException invariantViolation() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "TRIP_BOARDING_INVARIANT_VIOLATION", "Dữ liệu chuyến không nhất quán để xác nhận Boarding.");
    }

    private static BusinessException credentialInvariant() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "BOARDING_CREDENTIAL_INVARIANT_VIOLATION", "Boarding credential đang lưu không nhất quán.");
    }
}
