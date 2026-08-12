package com.zanh.route_sharing.repository.sharedroute.pickuparrival.postgis;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.DiemDungHanhTrinh;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.pickuparrival.TripPickupArrivalRepository;
import com.zanh.route_sharing.repository.sharedroute.pickuparrival.model.TripPickupArrivalCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.pickuparrival.model.TripPickupArrivalCommitResult;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
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
import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Repository
public class PostgisTripPickupArrivalRepository implements TripPickupArrivalRepository {

    private final EntityManager entityManager;

    public PostgisTripPickupArrivalRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public TripPickupArrivalCommitResult commit(TripPickupArrivalCommitCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockOwnedTrip(command.actorId(), command.tripId());
            requireTripInvariant(trip);

            List<DiemDungHanhTrinh> stops = lockTripStops(trip.getId());
            requireDriverStartInvariant(stops);
            DiemDungHanhTrinh target = firstUnresolved(stops);
            requireTargetPickup(target);

            YeuCauDiChung booking = target.getYeuCauDiChung();
            requireBookingInvariant(trip, booking);
            CauHinhNghiepVu configuration = lockCurrentConfiguration(booking);
            Instant waitingDeadline = waitingDeadline(command.arrivedAt(), configuration);

            if (!isWithinArrivalRadius(target, command.currentLocation())) {
                throw outsideArrivalRadius();
            }

            target.arrivePickup(command.currentLocation(), command.arrivedAt(), waitingDeadline);
            entityManager.persist(ThongBao.driverArrivedPickup(booking, trip, target));
            entityManager.flush();

            return new TripPickupArrivalCommitResult(
                    trip.getId(),
                    trip.getLoTrinhChiaSe().getId(),
                    trip.getTrangThaiVanHanh(),
                    booking.getId(),
                    booking.getTrangThaiYeuCau(),
                    trip.getSoKhachThucTe(),
                    target.getId(),
                    target.getThuTu(),
                    target.getTrangThaiDiemDung(),
                    target.getDenLuc(),
                    target.getBatDauChoLuc(),
                    target.getHanChoLuc(),
                    booking.getHanhKhach().getId());
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
                    .orElseThrow(PostgisTripPickupArrivalRepository::notFound);
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

    private CauHinhNghiepVu lockCurrentConfiguration(YeuCauDiChung booking) {
        Long schoolId = booking.getCauHinhLucGui() == null || booking.getCauHinhLucGui().getNhaTruong() == null
                ? null
                : booking.getCauHinhLucGui().getNhaTruong().getId();
        if (schoolId == null) {
            throw invariantViolation();
        }
        try {
            return entityManager.createQuery(
                            "select config from CauHinhNghiepVu config "
                                    + "where config.nhaTruong.id = :schoolId",
                            CauHinhNghiepVu.class)
                    .setParameter("schoolId", schoolId)
                    .setLockMode(LockModeType.PESSIMISTIC_READ)
                    .setMaxResults(1)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElseThrow(PostgisTripPickupArrivalRepository::waitingPolicyNotConfigured);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private boolean isWithinArrivalRadius(DiemDungHanhTrinh stop, Point actualLocation) {
        BigDecimal radius = stop.getBanKinhXacDinhDaDenMet();
        if (radius == null || radius.signum() <= 0 || stop.getToaDoKeHoach() == null
                || stop.getToaDoKeHoach().isEmpty() || stop.getToaDoKeHoach().getSRID() != Wgs84Coordinates.SRID) {
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
                .setParameter("stopId", stop.getId())
                .getSingleResult();
        return result instanceof Boolean value ? value : Boolean.parseBoolean(String.valueOf(result));
    }

    private static Instant waitingDeadline(Instant arrivedAt, CauHinhNghiepVu config) {
        Long seconds = config == null ? null : config.getThoiGianChoKhachGiay();
        if (seconds == null || seconds < 0) {
            throw waitingPolicyNotConfigured();
        }
        try {
            return arrivedAt.plusSeconds(seconds);
        } catch (DateTimeException | ArithmeticException exception) {
            throw waitingPolicyNotConfigured();
        }
    }

    private static DiemDungHanhTrinh firstUnresolved(List<DiemDungHanhTrinh> stops) {
        return stops.stream()
                .filter(stop -> stop.getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                        && stop.getTrangThaiDiemDung() != TrangThaiDiemDung.SKIPPED
                        && stop.getTrangThaiDiemDung() != TrangThaiDiemDung.CANCELLED)
                .findFirst()
                .orElseThrow(PostgisTripPickupArrivalRepository::noUnresolvedStop);
    }

    private static void requireTripInvariant(ChuyenDi trip) {
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS) {
            throw tripNotInProgress();
        }
        if (trip.getBatDauLuc() == null
                || trip.getLoTrinhChiaSe() == null
                || trip.getLoTrinhChiaSe().getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || trip.getSoKhachThucTe() == null
                || trip.getSoKhachKeHoach() == null
                || trip.getSoKhachThucTe() < 0
                || trip.getSoKhachThucTe() > trip.getSoKhachKeHoach()) {
            throw invariantViolation();
        }
    }

    private static void requireDriverStartInvariant(List<DiemDungHanhTrinh> stops) {
        List<DiemDungHanhTrinh> driverStarts = stops.stream()
                .filter(stop -> stop.getLoaiDiemDung() == LoaiDiemDung.DRIVER_START)
                .toList();
        if (driverStarts.size() != 1) {
            throw invariantViolation();
        }
        DiemDungHanhTrinh start = driverStarts.get(0);
        if (start.getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                || start.getToaDoThucTe() == null
                || start.getHoanThanhLuc() == null
                || start.getYeuCauDiChung() != null) {
            throw invariantViolation();
        }
    }

    private static void requireTargetPickup(DiemDungHanhTrinh target) {
        if (target.getLoaiDiemDung() != LoaiDiemDung.PICKUP) {
            throw nextStopNotPickup();
        }
        if (target.getTrangThaiDiemDung() == TrangThaiDiemDung.ARRIVED) {
            throw pickupAlreadyArrived();
        }
        if (target.getTrangThaiDiemDung() != TrangThaiDiemDung.PENDING) {
            throw pickupNotArrivable();
        }
        if (target.getId() == null || target.getYeuCauDiChung() == null
                || target.getToaDoThucTe() != null || target.getDenGanLuc() != null
                || target.getDenLuc() != null || target.getBatDauChoLuc() != null
                || target.getHanChoLuc() != null || target.getHoanThanhLuc() != null) {
            throw invariantViolation();
        }
    }

    private static void requireBookingInvariant(ChuyenDi trip, YeuCauDiChung booking) {
        if (booking.getId() == null
                || booking.getTrangThaiYeuCau() != TrangThaiYeuCau.ACCEPTED) {
            if (booking.getTrangThaiYeuCau() != TrangThaiYeuCau.ACCEPTED) {
                throw pickupBookingNotAccepted();
            }
            throw invariantViolation();
        }
        if (booking.getChuyenDi() == null || !Objects.equals(booking.getChuyenDi().getId(), trip.getId())
                || booking.getHanhKhach() == null || booking.getHanhKhach().getId() == null) {
            throw invariantViolation();
        }
    }

    private static void requireCommand(TripPickupArrivalCommitCommand command) {
        if (command == null || command.actorId() == null || command.actorId() <= 0
                || command.tripId() == null || command.tripId() <= 0
                || command.arrivedAt() == null || command.currentLocation() == null
                || command.currentLocation().isEmpty() || command.currentLocation().getSRID() != Wgs84Coordinates.SRID) {
            throw new IllegalArgumentException("TripPickupArrivalCommitCommand không hợp lệ.");
        }
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi.");
    }

    private static BusinessException tripNotInProgress() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_NOT_IN_PROGRESS", "Chuyến đi chưa ở trạng thái đang vận hành.");
    }

    private static BusinessException invariantViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_PICKUP_ARRIVAL_INVARIANT_VIOLATION",
                "Dữ liệu chuyến hoặc điểm dừng không nhất quán để ghi nhận đã đến pickup.");
    }

    private static BusinessException noUnresolvedStop() {
        return new BusinessException(HttpStatus.CONFLICT, "NO_UNRESOLVED_TRIP_STOP", "Không còn điểm dừng chưa giải quyết.");
    }

    private static BusinessException nextStopNotPickup() {
        return new BusinessException(HttpStatus.CONFLICT, "NEXT_TRIP_STOP_NOT_PICKUP",
                "Điểm dừng chưa giải quyết kế tiếp không phải pickup.");
    }

    private static BusinessException pickupAlreadyArrived() {
        return new BusinessException(HttpStatus.CONFLICT, "PICKUP_ALREADY_ARRIVED", "Tài xế đã được ghi nhận đến pickup này.");
    }

    private static BusinessException pickupNotArrivable() {
        return new BusinessException(HttpStatus.CONFLICT, "PICKUP_NOT_ARRIVABLE", "Pickup hiện không thể chuyển sang ARRIVED.");
    }

    private static BusinessException pickupBookingNotAccepted() {
        return new BusinessException(HttpStatus.CONFLICT, "PICKUP_BOOKING_NOT_ACCEPTED",
                "Booking gắn với pickup không còn ở trạng thái ACCEPTED.");
    }

    private static BusinessException waitingPolicyNotConfigured() {
        return new BusinessException(HttpStatus.CONFLICT, "WAITING_POLICY_NOT_CONFIGURED",
                "Chính sách thời gian chờ hiện không khả dụng.");
    }

    private static BusinessException outsideArrivalRadius() {
        return new BusinessException(HttpStatus.CONFLICT, "DRIVER_OUTSIDE_PICKUP_ARRIVAL_RADIUS",
                "Tài xế chưa ở trong phạm vi pickup cho phép xác nhận đã đến.");
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Chuyến đi đã được xử lý đồng thời. Vui lòng tải lại dữ liệu.");
    }

    private static BusinessException dataIntegrityViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "Không thể ghi nhận đã đến pickup do ràng buộc dữ liệu.");
    }
}
