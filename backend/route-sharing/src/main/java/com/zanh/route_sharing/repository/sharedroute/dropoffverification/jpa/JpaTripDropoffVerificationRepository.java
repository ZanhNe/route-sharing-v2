package com.zanh.route_sharing.repository.sharedroute.dropoffverification.jpa;

import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.DiemDungHanhTrinh;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.ThongTinXacThucTraKhach;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.TripDropoffVerificationRepository;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.TripDropoffVerificationCommand;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.TripDropoffVerificationCommitResult;
import com.zanh.route_sharing.security.dropoff.DropoffCodeProtector;
import com.zanh.route_sharing.security.dropoff.model.DropoffCodeBinding;
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
public class JpaTripDropoffVerificationRepository implements TripDropoffVerificationRepository {
    private final EntityManager entityManager;
    private final DropoffCodeProtector protector;

    public JpaTripDropoffVerificationRepository(EntityManager entityManager, DropoffCodeProtector protector) {
        this.entityManager = entityManager;
        this.protector = protector;
    }

    @Override
    @Transactional
    public TripDropoffVerificationCommitResult commit(TripDropoffVerificationCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockOwnedTrip(command.actorId(), command.tripId());
            requireTripInvariant(trip);
            List<DiemDungHanhTrinh> stops = lockTripStops(trip.getId());
            requireDriverStartCompleted(stops);
            requirePassengerCountInvariant(trip);
            DiemDungHanhTrinh dropoff = firstUnresolved(stops);
            requireCurrentDropoff(dropoff);
            YeuCauDiChung booking = lockBooking(trip.getId(), dropoff);
            requireBookingInvariant(trip, dropoff, booking, stops, command.droppedOffAt());
            ThongTinXacThucTraKhach credential = lockActiveCredential(trip, booking, dropoff);

            DropoffCodeBinding binding = new DropoffCodeBinding(trip.getId(), booking.getId(), dropoff.getId());
            String expectedCode = protector.reveal(credential.protectedCode(), binding);
            if (!constantTimeEquals(expectedCode, command.dropoffCode()))
                throw invalidDropoffCode();

            booking.completeNormalDropoff(trip, command.droppedOffAt());
            dropoff.completeArrivedDropoff(command.droppedOffAt());
            trip.giamMotKhachSauTraKhach();
            credential.resolve(command.droppedOffAt());
            entityManager.persist(NhatKyTrangThaiYeuCau.droppedOff(booking, trip.getLoTrinhChiaSe().getTaiXe(),
                    command.droppedOffAt(), nextRequestHistorySequence(booking.getId())));
            entityManager.persist(ThongBao.passengerDroppedOff(booking, trip, dropoff));
            entityManager.flush();
            requirePassengerCountInvariant(trip);

            return new TripDropoffVerificationCommitResult(
                    trip.getId(), trip.getLoTrinhChiaSe().getId(), booking.getId(), dropoff.getId(), dropoff.getThuTu(),
                    booking.getTrangThaiYeuCau(), dropoff.getTrangThaiDiemDung(), booking.getXuongXeLuc(),
                    trip.getSoKhachThucTe(), booking.getHanhKhach().getId());
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
                    "select trip from ChuyenDi trip join fetch trip.loTrinhChiaSe route join fetch route.taiXe driver "
                            + "where trip.id=:tripId and driver.id=:actorId",
                    ChuyenDi.class)
                    .setParameter("tripId", tripId).setParameter("actorId", actorId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList().stream().findFirst()
                    .orElseThrow(JpaTripDropoffVerificationRepository::notFound);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private List<DiemDungHanhTrinh> lockTripStops(Long tripId) {
        try {
            return entityManager.createQuery(
                    "select stop from DiemDungHanhTrinh stop where stop.chuyenDi.id=:tripId order by stop.thuTu asc, stop.id asc",
                    DiemDungHanhTrinh.class).setParameter("tripId", tripId).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private YeuCauDiChung lockBooking(Long tripId, DiemDungHanhTrinh dropoff) {
        if (dropoff.getYeuCauDiChung() == null || dropoff.getYeuCauDiChung().getId() == null)
            throw invariantViolation();
        try {
            return entityManager
                    .createQuery("select booking from YeuCauDiChung booking join fetch booking.hanhKhach passenger "
                            + "where booking.id=:bookingId and booking.chuyenDi.id=:tripId", YeuCauDiChung.class)
                    .setParameter("bookingId", dropoff.getYeuCauDiChung().getId()).setParameter("tripId", tripId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList().stream().findFirst()
                    .orElseThrow(JpaTripDropoffVerificationRepository::invariantViolation);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private ThongTinXacThucTraKhach lockActiveCredential(ChuyenDi trip, YeuCauDiChung booking,
            DiemDungHanhTrinh dropoff) {
        try {
            List<ThongTinXacThucTraKhach> rows = entityManager.createQuery(
                    "select credential from ThongTinXacThucTraKhach credential where credential.diemDungHanhTrinh.id=:dropoffStopId",
                    ThongTinXacThucTraKhach.class).setParameter("dropoffStopId", dropoff.getId())
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
            if (rows.size() != 1 || !rows.get(0).isActive())
                throw credentialNotActive();
            ThongTinXacThucTraKhach c = rows.get(0);
            if (c.getChuyenDi() == null || c.getYeuCauDiChung() == null || c.getDiemDungHanhTrinh() == null
                    || !Objects.equals(c.getChuyenDi().getId(), trip.getId())
                    || !Objects.equals(c.getYeuCauDiChung().getId(), booking.getId())
                    || !Objects.equals(c.getDiemDungHanhTrinh().getId(), dropoff.getId()))
                throw credentialInvariant();
            return c;
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private long nextRequestHistorySequence(Long rideRequestId) {
        Long next = entityManager.createQuery(
                "select coalesce(max(event.sequence),0)+1 from NhatKyTrangThaiYeuCau event where event.yeuCauDiChung.id=:rideRequestId",
                Long.class)
                .setParameter("rideRequestId", rideRequestId).getSingleResult();
        return next == null ? 1L : next;
    }

    private static DiemDungHanhTrinh firstUnresolved(List<DiemDungHanhTrinh> stops) {
        return stops.stream().filter(s -> s.getTrangThaiDiemDung().isUnresolvedForTripProgression())
                .findFirst().orElseThrow(JpaTripDropoffVerificationRepository::noUnresolvedStop);
    }

    private static void requireTripInvariant(ChuyenDi trip) {
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS)
            throw tripNotInProgress();
        if (trip.getBatDauLuc() == null || trip.getKetThucLuc() != null || trip.getLoTrinhChiaSe() == null
                || trip.getLoTrinhChiaSe().getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || trip.getLoTrinhChiaSe().getChuyenDi() != trip || trip.getSoKhachThucTe() == null
                || trip.getSoKhachKeHoach() == null
                || trip.getSoKhachThucTe() < 0 || trip.getSoKhachThucTe() > trip.getSoKhachKeHoach())
            throw invariantViolation();
    }

    private static void requireDriverStartCompleted(List<DiemDungHanhTrinh> stops) {
        List<DiemDungHanhTrinh> starts = stops.stream().filter(s -> s.getLoaiDiemDung() == LoaiDiemDung.DRIVER_START)
                .toList();
        if (starts.size() != 1 || starts.get(0).getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                || starts.get(0).getHoanThanhLuc() == null || starts.get(0).getToaDoThucTe() == null)
            throw driverStartNotCompleted();
    }

    private static void requireCurrentDropoff(DiemDungHanhTrinh d) {
        if (d.getLoaiDiemDung() != LoaiDiemDung.DROPOFF)
            throw nextStopNotDropoff();
        if (d.getTrangThaiDiemDung() != TrangThaiDiemDung.ARRIVED)
            throw dropoffNotArrived();
        if (d.getId() == null || d.getYeuCauDiChung() == null || d.getToaDoThucTe() == null || d.getDenLuc() == null
                || d.getBatDauChoLuc() != null || d.getHanChoLuc() != null || d.getHoanThanhLuc() != null)
            throw invariantViolation();
    }

    private static void requireBookingInvariant(ChuyenDi trip, DiemDungHanhTrinh dropoff, YeuCauDiChung b,
            List<DiemDungHanhTrinh> stops, java.time.Instant droppedOffAt) {
        if (b.getTrangThaiYeuCau() != TrangThaiYeuCau.ON_BOARD)
            throw bookingNotOnBoard();
        if (b.getChuyenDi() == null || !Objects.equals(b.getChuyenDi().getId(), trip.getId())
                || b.getHanhKhach() == null
                || b.getHanhKhach().getId() == null || dropoff.getYeuCauDiChung() == null
                || !Objects.equals(dropoff.getYeuCauDiChung().getId(), b.getId()) || b.getLenXeLuc() == null
                || b.getXuongXeLuc() != null || b.getKhongDenLuc() != null || b.getTaiXeXacNhanTraLuc() != null
                || b.getHanhKhachXacNhanTraLuc() != null || b.getLyDoXacNhanThatBai() != null
                || droppedOffAt.isBefore(b.getLenXeLuc()) || droppedOffAt.isBefore(dropoff.getDenLuc()))
            throw invariantViolation();
        List<DiemDungHanhTrinh> pickups = stops.stream().filter(s -> s.getLoaiDiemDung() == LoaiDiemDung.PICKUP
                && s.getYeuCauDiChung() != null && Objects.equals(s.getYeuCauDiChung().getId(), b.getId())).toList();
        if (pickups.size() != 1 || pickups.get(0).getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                || pickups.get(0).getHoanThanhLuc() == null
                || !pickups.get(0).getHoanThanhLuc().equals(b.getLenXeLuc()))
            throw invariantViolation();
    }

    private void requirePassengerCountInvariant(ChuyenDi trip) {
        Long count = entityManager.createQuery(
                "select count(booking) from YeuCauDiChung booking where booking.chuyenDi.id=:tripId and booking.trangThaiYeuCau=:status",
                Long.class)
                .setParameter("tripId", trip.getId()).setParameter("status", TrangThaiYeuCau.ON_BOARD)
                .getSingleResult();
        if (count == null || !Objects.equals(count.intValue(), trip.getSoKhachThucTe()))
            throw invariantViolation();
    }

    private static boolean constantTimeEquals(String expected, String candidate) {
        if (expected == null || candidate == null)
            return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                candidate.getBytes(StandardCharsets.US_ASCII));
    }

    private static void requireCommand(TripDropoffVerificationCommand c) {
        if (c == null || c.actorId() == null || c.actorId() <= 0 || c.tripId() == null || c.tripId() <= 0
                || c.droppedOffAt() == null
                || c.dropoffCode() == null || !c.dropoffCode().matches("[0-9]{6}"))
            throw new IllegalArgumentException("TripDropoffVerificationCommand không hợp lệ.");
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi.");
    }

    private static BusinessException tripNotInProgress() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_NOT_IN_PROGRESS",
                "Chuyến đi không ở trạng thái đang vận hành bình thường.");
    }

    private static BusinessException driverStartNotCompleted() {
        return new BusinessException(HttpStatus.CONFLICT, "DRIVER_START_NOT_COMPLETED",
                "Điểm DRIVER_START chưa được hoàn thành.");
    }

    private static BusinessException noUnresolvedStop() {
        return new BusinessException(HttpStatus.CONFLICT, "NO_UNRESOLVED_TRIP_STOP",
                "Không còn điểm dừng chưa giải quyết.");
    }

    private static BusinessException nextStopNotDropoff() {
        return new BusinessException(HttpStatus.CONFLICT, "NEXT_TRIP_STOP_NOT_DROPOFF",
                "Điểm dừng chưa giải quyết kế tiếp không phải dropoff.");
    }

    private static BusinessException dropoffNotArrived() {
        return new BusinessException(HttpStatus.CONFLICT, "DROPOFF_NOT_ARRIVED",
                "Dropoff hiện tại chưa ở trạng thái ARRIVED.");
    }

    private static BusinessException bookingNotOnBoard() {
        return new BusinessException(HttpStatus.CONFLICT, "DROPOFF_BOOKING_NOT_ON_BOARD",
                "Booking gắn với dropoff không còn ON_BOARD.");
    }

    private static BusinessException credentialNotActive() {
        return new BusinessException(HttpStatus.CONFLICT, "DROPOFF_CREDENTIAL_NOT_ACTIVE",
                "Dropoff credential hiện không active.");
    }

    private static BusinessException invalidDropoffCode() {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_DROPOFF_CODE",
                "Mã xác nhận trả khách không chính xác.");
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Chuyến đi vừa được thay đổi đồng thời. Vui lòng tải lại dữ liệu.");
    }

    private static BusinessException dataIntegrityViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "Dữ liệu xác nhận trả khách xung đột với ràng buộc lưu trữ hiện tại.");
    }

    private static BusinessException invariantViolation() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "TRIP_DROPOFF_VERIFICATION_INVARIANT_VIOLATION",
                "Dữ liệu chuyến không nhất quán để xác nhận trả khách.");
    }

    private static BusinessException credentialInvariant() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "DROPOFF_CREDENTIAL_INVARIANT_VIOLATION",
                "Dropoff credential đang lưu không nhất quán.");
    }
}
