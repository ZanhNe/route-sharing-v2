package com.zanh.route_sharing.repository.sharedroute.tripstart.postgis;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.DiemDungHanhTrinh;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiChuyenDi;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.eligibility.OperationalEligibilityRepository;
import com.zanh.route_sharing.repository.sharedroute.eligibility.model.CurrentOperationalEligibility;
import com.zanh.route_sharing.repository.sharedroute.tripstart.TripStartRepository;
import com.zanh.route_sharing.repository.sharedroute.tripstart.model.TripStartCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.tripstart.model.TripStartCommitResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class PostgisTripStartRepository implements TripStartRepository {

    private static final ZoneId BUSINESS_ZONE = TimePolicy.BUSINESS_ZONE;

    private final EntityManager entityManager;
    private final OperationalEligibilityRepository eligibilityRepository;

    public PostgisTripStartRepository(
            EntityManager entityManager,
            OperationalEligibilityRepository eligibilityRepository) {
        this.entityManager = entityManager;
        this.eligibilityRepository = eligibilityRepository;
    }

    @Override
    @Transactional
    public TripStartCommitResult commit(TripStartCommitCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockOwnedTrip(command.actorId(), command.tripId());

            if (trip.getBatDauLuc() != null) {
                throw alreadyStarted();
            }
            if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.PREPARING) {
                throw notStartable();
            }
            requireFormedTripInvariant(trip);

            DiemDungHanhTrinh driverStart = lockDriverStart(trip.getId());
            requireDriverStartInvariant(driverStart);

            List<YeuCauDiChung> attachedBookings = findAttachedBookings(trip.getId());
            Long schoolId = requireAttachedBookingInvariant(trip, attachedBookings);
            requireCurrentEligibility(command.actorId(), trip, schoolId);

            if (!isWithinStartRadius(driverStart, command.currentLocation())) {
                throw outsideStartRadius();
            }

            trip.start(command.startedAt());
            driverStart.completeDriverStart(command.currentLocation(), command.startedAt());

            long historySequence = nextTripHistorySequence(trip.getId());
            entityManager.persist(NhatKyTrangThaiChuyenDi.driverStarted(
                    trip,
                    trip.getLoTrinhChiaSe().getTaiXe(),
                    command.startedAt(),
                    historySequence));

            List<Long> recipients = new ArrayList<>(attachedBookings.size());
            List<Long> rideRequestIds = new ArrayList<>(attachedBookings.size());
            for (YeuCauDiChung booking : attachedBookings) {
                entityManager.persist(ThongBao.tripStarted(booking, trip));
                recipients.add(booking.getHanhKhach().getId());
                rideRequestIds.add(booking.getId());
            }

            entityManager.flush();
            return new TripStartCommitResult(
                    trip.getId(),
                    trip.getLoTrinhChiaSe().getId(),
                    trip.getTrangThaiVanHanh(),
                    trip.getBatDauLuc(),
                    trip.getSoKhachThucTe(),
                    driverStart.getId(),
                    driverStart.getTrangThaiDiemDung(),
                    recipients,
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
                            + "join fetch route.phuongTien vehicle "
                            + "where trip.id = :tripId and driver.id = :actorId",
                    ChuyenDi.class)
                    .setParameter("tripId", tripId)
                    .setParameter("actorId", actorId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElseThrow(PostgisTripStartRepository::notFound);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private DiemDungHanhTrinh lockDriverStart(Long tripId) {
        try {
            List<DiemDungHanhTrinh> rows = entityManager.createQuery(
                    "select stop from DiemDungHanhTrinh stop "
                            + "where stop.chuyenDi.id = :tripId "
                            + "and stop.loaiDiemDung = :type",
                    DiemDungHanhTrinh.class)
                    .setParameter("tripId", tripId)
                    .setParameter("type", LoaiDiemDung.DRIVER_START)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
            if (rows.size() != 1) {
                throw invariantViolation();
            }
            return rows.get(0);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private List<YeuCauDiChung> findAttachedBookings(Long tripId) {
        return entityManager.createQuery(
                "select request from YeuCauDiChung request "
                        + "join fetch request.hanhKhach passenger "
                        + "join fetch request.cauHinhLucGui configuration "
                        + "join fetch configuration.nhaTruong school "
                        + "where request.chuyenDi.id = :tripId "
                        + "order by request.id asc",
                YeuCauDiChung.class)
                .setParameter("tripId", tripId)
                .getResultList();
    }

    private Long requireAttachedBookingInvariant(
            ChuyenDi trip,
            List<YeuCauDiChung> bookings) {
        if (bookings.isEmpty()
                || trip.getSoKhachKeHoach() == null
                || bookings.size() != trip.getSoKhachKeHoach()) {
            throw invariantViolation();
        }
        if (bookings.stream().anyMatch(booking -> booking.getTrangThaiYeuCau() != TrangThaiYeuCau.ACCEPTED)) {
            throw invariantViolation();
        }
        Set<Long> schoolIds = bookings.stream()
                .map(booking -> booking.getCauHinhLucGui().getNhaTruong().getId())
                .collect(Collectors.toSet());
        if (schoolIds.size() != 1 || schoolIds.contains(null)) {
            throw invariantViolation();
        }
        return schoolIds.iterator().next();
    }

    private void requireCurrentEligibility(Long actorId, ChuyenDi trip, Long schoolId) {
        CurrentOperationalEligibility eligibility = eligibilityRepository.evaluate(
                actorId,
                trip.getLoTrinhChiaSe().getId(),
                schoolId,
                LocalDate.ofInstant(
                        trip.getLoTrinhChiaSe().getThoiGianKhoiHanhDuKien(),
                        BUSINESS_ZONE));
        if (eligibility == null || !eligibility.eligible()) {
            throw ineligible();
        }
    }

    private boolean isWithinStartRadius(DiemDungHanhTrinh driverStart, Point actualLocation) {
        BigDecimal radius = driverStart.getBanKinhXacDinhDaDenMet();
        if (radius == null || radius.signum() <= 0) {
            throw invariantViolation();
        }
        Object result = entityManager.createNativeQuery("""
                SELECT ST_DWithin(
                    stop.toa_do_ke_hoach::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    stop.ban_kinh_xac_dinh_da_den_met
                )
                FROM diem_dung_hanh_trinh stop
                WHERE stop.id = :stopId
                """)
                .setParameter("longitude", actualLocation.getX())
                .setParameter("latitude", actualLocation.getY())
                .setParameter("stopId", driverStart.getId())
                .getSingleResult();
        return result instanceof Boolean value
                ? value
                : Boolean.parseBoolean(String.valueOf(result));
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

    private static void requireFormedTripInvariant(ChuyenDi trip) {
        if (trip.getLoTrinhChiaSe() == null
                || trip.getLoTrinhChiaSe().getId() == null
                || trip.getLoTrinhChiaSe().getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || trip.getLoTrinhChiaSe().getChuyenDi() == null
                || !Objects.equals(trip.getId(), trip.getLoTrinhChiaSe().getChuyenDi().getId())
                || trip.getSoKhachThucTe() == null
                || trip.getSoKhachThucTe() != 0) {
            throw invariantViolation();
        }
    }

    private static void requireDriverStartInvariant(DiemDungHanhTrinh driverStart) {
        if (driverStart.getId() == null
                || driverStart.getToaDoKeHoach() == null
                || driverStart.getToaDoKeHoach().isEmpty()
                || driverStart.getToaDoKeHoach().getSRID() != Wgs84Coordinates.SRID
                || driverStart.getTrangThaiDiemDung() != TrangThaiDiemDung.PENDING
                || driverStart.getToaDoThucTe() != null
                || driverStart.getHoanThanhLuc() != null
                || driverStart.getYeuCauDiChung() != null) {
            throw invariantViolation();
        }
    }

    private static void requireCommand(TripStartCommitCommand command) {
        if (command == null
                || command.actorId() == null || command.actorId() <= 0
                || command.tripId() == null || command.tripId() <= 0
                || command.startedAt() == null
                || command.currentLocation() == null
                || command.currentLocation().isEmpty()
                || command.currentLocation().getSRID() != Wgs84Coordinates.SRID) {
            throw new IllegalArgumentException("TripStartCommitCommand không hợp lệ.");
        }
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi.");
    }

    private static BusinessException alreadyStarted() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_ALREADY_STARTED",
                "Chuyến đi đã được bắt đầu trước đó.");
    }

    private static BusinessException notStartable() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_NOT_STARTABLE", "Chuyến đi hiện không thể bắt đầu.");
    }

    private static BusinessException ineligible() {
        return new BusinessException(HttpStatus.CONFLICT, "DRIVER_OR_VEHICLE_INELIGIBLE",
                "Tài xế hoặc phương tiện hiện không còn đủ điều kiện bắt đầu chuyến đi.");
    }

    private static BusinessException outsideStartRadius() {
        return new BusinessException(HttpStatus.CONFLICT, "DRIVER_OUTSIDE_START_RADIUS",
                "Tài xế chưa ở trong phạm vi điểm bắt đầu chuyến.");
    }

    private static BusinessException invariantViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_START_INVARIANT_VIOLATION",
                "Dữ liệu chuyến đi không nhất quán để bắt đầu.");
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Chuyến đi vừa được thay đổi bởi thao tác khác. Vui lòng tải lại dữ liệu.");
    }

    private static BusinessException dataIntegrityViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "Không thể ghi nhận bắt đầu chuyến do xung đột dữ liệu.");
    }
}
