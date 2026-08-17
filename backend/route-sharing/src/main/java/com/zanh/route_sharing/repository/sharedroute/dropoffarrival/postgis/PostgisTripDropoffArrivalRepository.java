package com.zanh.route_sharing.repository.sharedroute.dropoffarrival.postgis;

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
import com.zanh.route_sharing.repository.sharedroute.dropoffarrival.TripDropoffArrivalRepository;
import com.zanh.route_sharing.repository.sharedroute.dropoffarrival.model.TripDropoffArrivalCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.dropoffarrival.model.TripDropoffArrivalCommitResult;
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
import java.util.List;
import java.util.Objects;

@Repository
public class PostgisTripDropoffArrivalRepository implements TripDropoffArrivalRepository {

    private final EntityManager entityManager;

    public PostgisTripDropoffArrivalRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public TripDropoffArrivalCommitResult commit(TripDropoffArrivalCommitCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockOwnedTrip(command.actorId(), command.tripId());
            requireTripInvariant(trip);
            List<DiemDungHanhTrinh> stops = lockTripStops(trip.getId());
            requireDriverStartInvariant(stops);
            requirePassengerCountInvariant(trip);
            DiemDungHanhTrinh target = firstUnresolved(stops);
            requireTargetDropoff(target);
            YeuCauDiChung booking = target.getYeuCauDiChung();
            requireBookingInvariant(trip, booking, stops);
            if (!isWithinArrivalRadius(target, command.currentLocation())) {
                throw outsideArrivalRadius();
            }
            target.arriveDropoff(command.currentLocation(), command.arrivedAt());
            entityManager.persist(ThongBao.driverArrivedDropoff(booking, trip, target));
            entityManager.flush();
            return new TripDropoffArrivalCommitResult(
                    trip.getId(), trip.getLoTrinhChiaSe().getId(), trip.getTrangThaiVanHanh(),
                    booking.getId(), booking.getTrangThaiYeuCau(), trip.getSoKhachThucTe(),
                    target.getId(), target.getThuTu(), target.getTrangThaiDiemDung(), target.getDenLuc(),
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
                            "select trip from ChuyenDi trip join fetch trip.loTrinhChiaSe route "
                                    + "join fetch route.taiXe driver where trip.id = :tripId and driver.id = :actorId",
                            ChuyenDi.class)
                    .setParameter("tripId", tripId)
                    .setParameter("actorId", actorId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1).getResultList().stream().findFirst()
                    .orElseThrow(PostgisTripDropoffArrivalRepository::notFound);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private List<DiemDungHanhTrinh> lockTripStops(Long tripId) {
        try {
            return entityManager.createQuery(
                            "select stop from DiemDungHanhTrinh stop where stop.chuyenDi.id = :tripId "
                                    + "order by stop.thuTu asc, stop.id asc", DiemDungHanhTrinh.class)
                    .setParameter("tripId", tripId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
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

    private static DiemDungHanhTrinh firstUnresolved(List<DiemDungHanhTrinh> stops) {
        return stops.stream()
                .filter(stop -> stop.getTrangThaiDiemDung().isUnresolvedForTripProgression())
                .findFirst().orElseThrow(PostgisTripDropoffArrivalRepository::noUnresolvedStop);
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

    private static void requireDriverStartInvariant(List<DiemDungHanhTrinh> stops) {
        List<DiemDungHanhTrinh> starts = stops.stream()
                .filter(stop -> stop.getLoaiDiemDung() == LoaiDiemDung.DRIVER_START).toList();
        if (starts.size() != 1) throw invariantViolation();
        DiemDungHanhTrinh start = starts.get(0);
        if (start.getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                || start.getToaDoThucTe() == null || start.getHoanThanhLuc() == null
                || start.getYeuCauDiChung() != null) {
            throw invariantViolation();
        }
    }

    private static void requireTargetDropoff(DiemDungHanhTrinh target) {
        if (target.getLoaiDiemDung() != LoaiDiemDung.DROPOFF) throw nextStopNotDropoff();
        if (target.getTrangThaiDiemDung() == TrangThaiDiemDung.ARRIVED) throw dropoffAlreadyArrived();
        if (target.getTrangThaiDiemDung() != TrangThaiDiemDung.PENDING) throw dropoffNotArrivable();
        if (target.getId() == null || target.getYeuCauDiChung() == null
                || target.getToaDoThucTe() != null || target.getDenGanLuc() != null
                || target.getDenLuc() != null || target.getBatDauChoLuc() != null
                || target.getHanChoLuc() != null || target.getHoanThanhLuc() != null) {
            throw invariantViolation();
        }
    }

    private void requirePassengerCountInvariant(ChuyenDi trip) {
        Long onBoardCount = entityManager.createQuery(
                        "select count(booking) from YeuCauDiChung booking "
                                + "where booking.chuyenDi.id = :tripId and booking.trangThaiYeuCau = :status",
                        Long.class)
                .setParameter("tripId", trip.getId())
                .setParameter("status", TrangThaiYeuCau.ON_BOARD)
                .getSingleResult();
        if (onBoardCount == null || !Objects.equals(onBoardCount.intValue(), trip.getSoKhachThucTe())) {
            throw invariantViolation();
        }
    }

    private static void requireBookingInvariant(
            ChuyenDi trip, YeuCauDiChung booking, List<DiemDungHanhTrinh> stops) {
        if (booking == null || booking.getId() == null) throw invariantViolation();
        if (booking.getTrangThaiYeuCau() != TrangThaiYeuCau.ON_BOARD) throw dropoffBookingNotOnBoard();
        if (booking.getChuyenDi() == null || !Objects.equals(booking.getChuyenDi().getId(), trip.getId())
                || booking.getHanhKhach() == null || booking.getHanhKhach().getId() == null
                || booking.getLenXeLuc() == null) {
            throw invariantViolation();
        }
        List<DiemDungHanhTrinh> pickups = stops.stream()
                .filter(stop -> stop.getLoaiDiemDung() == LoaiDiemDung.PICKUP
                        && stop.getYeuCauDiChung() != null
                        && Objects.equals(stop.getYeuCauDiChung().getId(), booking.getId()))
                .toList();
        if (pickups.size() != 1) throw invariantViolation();
        DiemDungHanhTrinh pickup = pickups.get(0);
        if (pickup.getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                || pickup.getHoanThanhLuc() == null
                || !pickup.getHoanThanhLuc().equals(booking.getLenXeLuc())) {
            throw invariantViolation();
        }
    }

    private static void requireCommand(TripDropoffArrivalCommitCommand command) {
        if (command == null || command.actorId() == null || command.actorId() <= 0
                || command.tripId() == null || command.tripId() <= 0 || command.arrivedAt() == null
                || command.currentLocation() == null || command.currentLocation().isEmpty()
                || command.currentLocation().getSRID() != Wgs84Coordinates.SRID) {
            throw new IllegalArgumentException("TripDropoffArrivalCommitCommand không hợp lệ.");
        }
    }

    private static BusinessException notFound() { return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi."); }
    private static BusinessException tripNotInProgress() { return new BusinessException(HttpStatus.CONFLICT, "TRIP_NOT_IN_PROGRESS", "Chuyến đi không ở trạng thái đang vận hành bình thường."); }
    private static BusinessException invariantViolation() { return new BusinessException(HttpStatus.CONFLICT, "TRIP_DROPOFF_ARRIVAL_INVARIANT_VIOLATION", "Dữ liệu chuyến hoặc điểm trả không nhất quán để ghi nhận đã đến dropoff."); }
    private static BusinessException noUnresolvedStop() { return new BusinessException(HttpStatus.CONFLICT, "NO_UNRESOLVED_TRIP_STOP", "Không còn điểm dừng chưa giải quyết."); }
    private static BusinessException nextStopNotDropoff() { return new BusinessException(HttpStatus.CONFLICT, "NEXT_TRIP_STOP_NOT_DROPOFF", "Điểm dừng chưa giải quyết kế tiếp không phải dropoff."); }
    private static BusinessException dropoffAlreadyArrived() { return new BusinessException(HttpStatus.CONFLICT, "DROPOFF_ALREADY_ARRIVED", "Tài xế đã được ghi nhận đến dropoff này."); }
    private static BusinessException dropoffNotArrivable() { return new BusinessException(HttpStatus.CONFLICT, "DROPOFF_NOT_ARRIVABLE", "Dropoff hiện không thể chuyển sang ARRIVED."); }
    private static BusinessException dropoffBookingNotOnBoard() { return new BusinessException(HttpStatus.CONFLICT, "DROPOFF_BOOKING_NOT_ON_BOARD", "Booking gắn với dropoff không còn ở trạng thái ON_BOARD."); }
    private static BusinessException outsideArrivalRadius() { return new BusinessException(HttpStatus.CONFLICT, "DRIVER_OUTSIDE_DROPOFF_ARRIVAL_RADIUS", "Tài xế chưa ở trong phạm vi dropoff cho phép xác nhận đã đến."); }
    private static BusinessException concurrentModification() { return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Chuyến đi đã được xử lý đồng thời. Vui lòng tải lại dữ liệu."); }
    private static BusinessException dataIntegrityViolation() { return new BusinessException(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Không thể ghi nhận đã đến dropoff do ràng buộc dữ liệu."); }
}
