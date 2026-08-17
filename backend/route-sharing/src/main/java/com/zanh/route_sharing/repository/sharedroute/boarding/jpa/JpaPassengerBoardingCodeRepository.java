package com.zanh.route_sharing.repository.sharedroute.boarding.jpa;

import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.DiemDungHanhTrinh;
import com.zanh.route_sharing.domain.entity.ThongTinXacThucLenXe;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.boarding.PassengerBoardingCodeRepository;
import com.zanh.route_sharing.repository.sharedroute.boarding.model.PassengerBoardingCodeCommand;
import com.zanh.route_sharing.repository.sharedroute.boarding.model.PassengerBoardingCodeResult;
import com.zanh.route_sharing.security.boarding.BoardingCodeGenerator;
import com.zanh.route_sharing.security.boarding.BoardingCodeProtector;
import com.zanh.route_sharing.security.boarding.model.BoardingCodeBinding;
import com.zanh.route_sharing.security.boarding.model.ProtectedBoardingCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Repository
public class JpaPassengerBoardingCodeRepository implements PassengerBoardingCodeRepository {
    private final EntityManager entityManager;
    private final BoardingCodeGenerator generator;
    private final BoardingCodeProtector protector;

    public JpaPassengerBoardingCodeRepository(
            EntityManager entityManager,
            BoardingCodeGenerator generator,
            BoardingCodeProtector protector) {
        this.entityManager = entityManager;
        this.generator = generator;
        this.protector = protector;
    }

    @Override
    @Transactional
    public PassengerBoardingCodeResult getOrCreate(PassengerBoardingCodeCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockTrip(command.actorId(), command.tripId());
            List<DiemDungHanhTrinh> stops = lockTripStops(trip.getId());
            DiemDungHanhTrinh pickup = firstUnresolved(stops);
            requireBoardingActiveTripAndPickup(trip, pickup);

            YeuCauDiChung booking = pickup.getYeuCauDiChung();
            requireBookingBinding(trip, pickup, booking);
            if (!Objects.equals(booking.getHanhKhach().getId(), command.actorId())) {
                throw codeNotAvailable();
            }

            BoardingCodeBinding binding = new BoardingCodeBinding(trip.getId(), booking.getId(), pickup.getId());
            List<ThongTinXacThucLenXe> rows = lockCredential(pickup.getId());
            final String code;
            if (rows.isEmpty()) {
                code = generator.generate();
                requireGeneratedCode(code);
                ProtectedBoardingCode protectedCode = protector.protect(code, binding);
                entityManager.persist(ThongTinXacThucLenXe.activate(
                        trip, booking, pickup, protectedCode, command.requestedAt()));
                entityManager.flush();
            } else if (rows.size() == 1) {
                ThongTinXacThucLenXe credential = rows.get(0);
                requireCredentialBinding(credential, trip, booking, pickup);
                if (!credential.isActive()) {
                    throw credentialInvariant();
                }
                code = protector.reveal(credential.protectedCode(), binding);
                requireGeneratedCode(code);
            } else {
                throw credentialInvariant();
            }
            return new PassengerBoardingCodeResult(
                    trip.getId(), booking.getId(), pickup.getId(), pickup.getThuTu(), code);
        } catch (BusinessException exception) {
            throw exception;
        } catch (PessimisticLockException | LockTimeoutException | OptimisticLockException exception) {
            throw concurrentModification();
        } catch (PersistenceException exception) {
            throw credentialInvariant();
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw credentialInvariant();
        }
    }

    private ChuyenDi lockTrip(Long actorId, Long tripId) {
        try {
            return entityManager.createQuery(
                    "select trip from ChuyenDi trip "
                            + "join fetch trip.loTrinhChiaSe route "
                            + "where trip.id = :tripId "
                            + "and exists (select 1 from YeuCauDiChung ownRequest "
                            + "where ownRequest.chuyenDi = trip and ownRequest.hanhKhach.id = :actorId)",
                    ChuyenDi.class)
                    .setParameter("tripId", tripId)
                    .setParameter("actorId", actorId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultList().stream().findFirst()
                    .orElseThrow(JpaPassengerBoardingCodeRepository::notFound);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private List<DiemDungHanhTrinh> lockTripStops(Long tripId) {
        try {
            return entityManager.createQuery(
                    "select stop from DiemDungHanhTrinh stop "
                            + "left join fetch stop.yeuCauDiChung booking "
                            + "left join fetch booking.hanhKhach passenger "
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

    private List<ThongTinXacThucLenXe> lockCredential(Long pickupStopId) {
        try {
            return entityManager.createQuery(
                    "select credential from ThongTinXacThucLenXe credential "
                            + "where credential.diemDungHanhTrinh.id = :pickupStopId",
                    ThongTinXacThucLenXe.class)
                    .setParameter("pickupStopId", pickupStopId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private static DiemDungHanhTrinh firstUnresolved(List<DiemDungHanhTrinh> stops) {
        return stops.stream()
                .filter(stop -> stop.getTrangThaiDiemDung().isUnresolvedForTripProgression())
                .findFirst()
                .orElseThrow(JpaPassengerBoardingCodeRepository::noUnresolvedStop);
    }

    private static void requireBoardingActiveTripAndPickup(ChuyenDi trip, DiemDungHanhTrinh pickup) {
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                || trip.getBatDauLuc() == null
                || trip.getLoTrinhChiaSe() == null
                || trip.getLoTrinhChiaSe().getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || pickup.getLoaiDiemDung() != LoaiDiemDung.PICKUP
                || pickup.getTrangThaiDiemDung() != TrangThaiDiemDung.ARRIVED
                || pickup.getDenLuc() == null
                || pickup.getBatDauChoLuc() == null
                || pickup.getHanChoLuc() == null) {
            throw codeNotAvailable();
        }
    }

    private static void requireBookingBinding(
            ChuyenDi trip, DiemDungHanhTrinh pickup, YeuCauDiChung booking) {
        if (booking == null || booking.getId() == null || booking.getHanhKhach() == null
                || booking.getHanhKhach().getId() == null
                || booking.getTrangThaiYeuCau() != TrangThaiYeuCau.ACCEPTED
                || booking.getChuyenDi() == null
                || !Objects.equals(booking.getChuyenDi().getId(), trip.getId())
                || pickup.getYeuCauDiChung() == null
                || !Objects.equals(pickup.getYeuCauDiChung().getId(), booking.getId())) {
            throw codeNotAvailable();
        }
    }

    private static void requireCredentialBinding(
            ThongTinXacThucLenXe credential,
            ChuyenDi trip,
            YeuCauDiChung booking,
            DiemDungHanhTrinh pickup) {
        if (credential.getChuyenDi() == null || credential.getYeuCauDiChung() == null
                || credential.getDiemDungHanhTrinh() == null
                || !Objects.equals(credential.getChuyenDi().getId(), trip.getId())
                || !Objects.equals(credential.getYeuCauDiChung().getId(), booking.getId())
                || !Objects.equals(credential.getDiemDungHanhTrinh().getId(), pickup.getId())) {
            throw credentialInvariant();
        }
    }

    private static void requireCommand(PassengerBoardingCodeCommand command) {
        if (command == null || command.actorId() == null || command.actorId() <= 0
                || command.tripId() == null || command.tripId() <= 0 || command.requestedAt() == null) {
            throw new IllegalArgumentException("PassengerBoardingCodeCommand không hợp lệ.");
        }
    }

    private static void requireGeneratedCode(String code) {
        if (code == null || !code.matches("[0-9]{6}")) {
            throw credentialInvariant();
        }
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi.");
    }

    private static BusinessException noUnresolvedStop() {
        return new BusinessException(HttpStatus.CONFLICT, "NO_UNRESOLVED_TRIP_STOP",
                "Không còn điểm dừng chưa giải quyết.");
    }

    private static BusinessException codeNotAvailable() {
        return new BusinessException(HttpStatus.CONFLICT, "BOARDING_CODE_NOT_AVAILABLE",
                "Boarding code hiện không khả dụng cho Passenger trên pickup hiện tại.");
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Chuyến đi vừa được thay đổi đồng thời. Vui lòng tải lại dữ liệu.");
    }

    private static BusinessException credentialInvariant() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "BOARDING_CREDENTIAL_INVARIANT_VIOLATION",
                "Boarding credential đang lưu không nhất quán.");
    }
}
