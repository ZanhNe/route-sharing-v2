package com.zanh.route_sharing.repository.sharedroute.dropoffverification.jpa;

import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.DiemDungHanhTrinh;
import com.zanh.route_sharing.domain.entity.ThongTinXacThucTraKhach;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.PassengerDropoffCodeRepository;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.PassengerDropoffCodeCommand;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.PassengerDropoffCodeResult;
import com.zanh.route_sharing.security.dropoff.DropoffCodeGenerator;
import com.zanh.route_sharing.security.dropoff.DropoffCodeProtector;
import com.zanh.route_sharing.security.dropoff.model.DropoffCodeBinding;
import com.zanh.route_sharing.security.dropoff.model.ProtectedDropoffCode;
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
public class JpaPassengerDropoffCodeRepository implements PassengerDropoffCodeRepository {
    private final EntityManager entityManager;
    private final DropoffCodeGenerator generator;
    private final DropoffCodeProtector protector;

    public JpaPassengerDropoffCodeRepository(EntityManager entityManager, DropoffCodeGenerator generator,
            DropoffCodeProtector protector) {
        this.entityManager = entityManager;
        this.generator = generator;
        this.protector = protector;
    }

    @Override
    @Transactional
    public PassengerDropoffCodeResult getOrCreate(PassengerDropoffCodeCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockTrip(command.actorId(), command.tripId());
            List<DiemDungHanhTrinh> stops = lockTripStops(trip.getId());
            DiemDungHanhTrinh dropoff = firstUnresolved(stops);
            requireEligibleTripAndDropoff(trip, dropoff);
            YeuCauDiChung booking = dropoff.getYeuCauDiChung();
            requireBookingBinding(trip, dropoff, booking, stops);
            if (!Objects.equals(booking.getHanhKhach().getId(), command.actorId()))
                throw codeNotAvailable();

            DropoffCodeBinding binding = new DropoffCodeBinding(trip.getId(), booking.getId(), dropoff.getId());
            List<ThongTinXacThucTraKhach> rows = lockCredential(dropoff.getId());
            final String code;
            if (rows.isEmpty()) {
                code = generator.generate();
                requireGeneratedCode(code);
                ProtectedDropoffCode protectedCode = protector.protect(code, binding);
                entityManager.persist(
                        ThongTinXacThucTraKhach.activate(trip, booking, dropoff, protectedCode, command.requestedAt()));
                entityManager.flush();
            } else if (rows.size() == 1) {
                ThongTinXacThucTraKhach credential = rows.get(0);
                requireCredentialBinding(credential, trip, booking, dropoff);
                if (!credential.isActive())
                    throw credentialInvariant();
                code = protector.reveal(credential.protectedCode(), binding);
                requireGeneratedCode(code);
            } else {
                throw credentialInvariant();
            }
            return new PassengerDropoffCodeResult(trip.getId(), booking.getId(), dropoff.getId(), dropoff.getThuTu(),
                    code);
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
                    "select trip from ChuyenDi trip join fetch trip.loTrinhChiaSe route "
                            + "where trip.id=:tripId and exists (select 1 from YeuCauDiChung ownRequest "
                            + "where ownRequest.chuyenDi=trip and ownRequest.hanhKhach.id=:actorId)",
                    ChuyenDi.class)
                    .setParameter("tripId", tripId).setParameter("actorId", actorId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList().stream().findFirst()
                    .orElseThrow(JpaPassengerDropoffCodeRepository::notFound);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private List<DiemDungHanhTrinh> lockTripStops(Long tripId) {
        try {
            return entityManager.createQuery(
                    "select stop from DiemDungHanhTrinh stop left join fetch stop.yeuCauDiChung booking "
                            + "left join fetch booking.hanhKhach passenger where stop.chuyenDi.id=:tripId "
                            + "order by stop.thuTu asc, stop.id asc",
                    DiemDungHanhTrinh.class)
                    .setParameter("tripId", tripId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private List<ThongTinXacThucTraKhach> lockCredential(Long dropoffStopId) {
        try {
            return entityManager.createQuery(
                    "select credential from ThongTinXacThucTraKhach credential where credential.diemDungHanhTrinh.id=:dropoffStopId",
                    ThongTinXacThucTraKhach.class).setParameter("dropoffStopId", dropoffStopId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private static DiemDungHanhTrinh firstUnresolved(List<DiemDungHanhTrinh> stops) {
        return stops.stream().filter(s -> s.getTrangThaiDiemDung().isUnresolvedForTripProgression())
                .findFirst().orElseThrow(JpaPassengerDropoffCodeRepository::noUnresolvedStop);
    }

    private static void requireEligibleTripAndDropoff(ChuyenDi trip, DiemDungHanhTrinh dropoff) {
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS || trip.getBatDauLuc() == null
                || trip.getKetThucLuc() != null || trip.getLoTrinhChiaSe() == null
                || trip.getLoTrinhChiaSe().getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || trip.getLoTrinhChiaSe().getChuyenDi() != trip
                || dropoff.getLoaiDiemDung() != LoaiDiemDung.DROPOFF
                || dropoff.getTrangThaiDiemDung() != TrangThaiDiemDung.ARRIVED
                || dropoff.getToaDoThucTe() == null || dropoff.getDenLuc() == null
                || dropoff.getBatDauChoLuc() != null || dropoff.getHanChoLuc() != null
                || dropoff.getHoanThanhLuc() != null) {
            throw codeNotAvailable();
        }
    }

    private static void requireBookingBinding(ChuyenDi trip, DiemDungHanhTrinh dropoff, YeuCauDiChung booking,
            List<DiemDungHanhTrinh> stops) {
        if (booking == null || booking.getId() == null || booking.getHanhKhach() == null
                || booking.getHanhKhach().getId() == null
                || booking.getTrangThaiYeuCau() != TrangThaiYeuCau.ON_BOARD || booking.getChuyenDi() == null
                || !Objects.equals(booking.getChuyenDi().getId(), trip.getId())
                || !Objects.equals(dropoff.getYeuCauDiChung().getId(), booking.getId())
                || booking.getLenXeLuc() == null || booking.getXuongXeLuc() != null || booking.getKhongDenLuc() != null
                || booking.getTaiXeXacNhanTraLuc() != null || booking.getHanhKhachXacNhanTraLuc() != null
                || booking.getLyDoXacNhanThatBai() != null || dropoff.getDenLuc().isBefore(booking.getLenXeLuc()))
            throw codeNotAvailable();
        List<DiemDungHanhTrinh> pickups = stops.stream().filter(s -> s.getLoaiDiemDung() == LoaiDiemDung.PICKUP
                && s.getYeuCauDiChung() != null && Objects.equals(s.getYeuCauDiChung().getId(), booking.getId()))
                .toList();
        if (pickups.size() != 1 || pickups.get(0).getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                || pickups.get(0).getHoanThanhLuc() == null
                || !pickups.get(0).getHoanThanhLuc().equals(booking.getLenXeLuc()))
            throw codeNotAvailable();
    }

    private static void requireCredentialBinding(ThongTinXacThucTraKhach c, ChuyenDi t, YeuCauDiChung b,
            DiemDungHanhTrinh d) {
        if (c.getChuyenDi() == null || c.getYeuCauDiChung() == null || c.getDiemDungHanhTrinh() == null
                || !Objects.equals(c.getChuyenDi().getId(), t.getId())
                || !Objects.equals(c.getYeuCauDiChung().getId(), b.getId())
                || !Objects.equals(c.getDiemDungHanhTrinh().getId(), d.getId()))
            throw credentialInvariant();
    }

    private static void requireGeneratedCode(String code) {
        if (code == null || !code.matches("[0-9]{6}"))
            throw credentialInvariant();
    }

    private static void requireCommand(PassengerDropoffCodeCommand c) {
        if (c == null || c.actorId() == null || c.actorId() <= 0 || c.tripId() == null || c.tripId() <= 0
                || c.requestedAt() == null)
            throw new IllegalArgumentException("PassengerDropoffCodeCommand không hợp lệ.");
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi.");
    }

    private static BusinessException noUnresolvedStop() {
        return new BusinessException(HttpStatus.CONFLICT, "NO_UNRESOLVED_TRIP_STOP",
                "Không còn điểm dừng chưa giải quyết.");
    }

    private static BusinessException codeNotAvailable() {
        return new BusinessException(HttpStatus.CONFLICT, "DROPOFF_CODE_NOT_AVAILABLE",
                "Mã xác nhận trả khách hiện không khả dụng.");
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Chuyến đi vừa được thay đổi đồng thời. Vui lòng tải lại dữ liệu.");
    }

    private static BusinessException credentialInvariant() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "DROPOFF_CREDENTIAL_INVARIANT_VIOLATION",
                "Dropoff credential đang lưu không nhất quán.");
    }
}
