package com.zanh.route_sharing.repository.sharedroute.triplocation.postgis;

import com.zanh.route_sharing.domain.entity.BanGhiDinhVi;
import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.enums.NguonViTri;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.triplocation.TripLocationRepository;
import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCommitOutcome;
import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCommitResult;
import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCurrentOrdering;
import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripCurrentLocationFact;
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
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Repository
public class PostgisTripLocationRepository implements TripLocationRepository {

    private final EntityManager entityManager;

    public PostgisTripLocationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public TripLocationCommitResult record(TripLocationCommitCommand command) {
        requireCommand(command);
        try {
            ChuyenDi trip = lockOwnedTrip(command.actorId(), command.tripId());
            requireTrackingActive(trip);
            CauHinhNghiepVu configuration = lockCurrentConfiguration(trip.getId());
            requireLocationConfiguration(configuration);

            BanGhiDinhVi duplicate = findDuplicate(
                    trip.getId(),
                    command.observedAt(),
                    command.position());
            if (duplicate != null) {
                return result(
                        trip.getId(),
                        duplicate,
                        TripLocationCommitOutcome.DUPLICATE_IGNORED,
                        false,
                        configuration.getChuKyGuiViTriGiay());
            }

            BanGhiDinhVi current = findCurrentObservation(trip.getId());
            long sequence = nextSequence(trip.getId());
            boolean becomesCurrent = current == null || TripLocationCurrentOrdering.compare(
                    command.observedAt(), command.receivedAt(), sequence,
                    current.getThoiGianTrinhDuyet(), current.getThoiGianServerNhan(), current.getThuTuBanGhi()) > 0;

            BanGhiDinhVi recorded = BanGhiDinhVi.builder()
                    .toaDo(copyPoint(command.position()))
                    .thoiGianTrinhDuyet(command.observedAt())
                    .thoiGianServerNhan(command.receivedAt())
                    .doChinhXacMet(command.accuracyMeters())
                    .thuTuBanGhi(sequence)
                    .nguonViTri(NguonViTri.BROWSER_GEOLOCATION)
                    .chuyenDi(trip)
                    .build();
            entityManager.persist(recorded);

            TripLocationCommitOutcome outcome;
            if (becomesCurrent) {
                trip.recordCurrentLocation(command.position(), command.receivedAt());
                outcome = TripLocationCommitOutcome.CURRENT_RECORDED;
            } else {
                outcome = TripLocationCommitOutcome.HISTORICAL_RECORDED;
            }

            entityManager.flush();
            return result(
                    trip.getId(),
                    recorded,
                    outcome,
                    becomesCurrent,
                    configuration.getChuKyGuiViTriGiay());
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
                    .orElseThrow(PostgisTripLocationRepository::notFound);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private CauHinhNghiepVu lockCurrentConfiguration(Long tripId) {
        List<Long> schoolIds = entityManager.createQuery(
                        "select distinct snapshot.nhaTruong.id "
                                + "from YeuCauDiChung request "
                                + "join request.cauHinhLucGui snapshot "
                                + "where request.chuyenDi.id = :tripId",
                        Long.class)
                .setParameter("tripId", tripId)
                .getResultList();
        if (schoolIds.size() != 1 || schoolIds.get(0) == null) {
            throw invariantViolation();
        }
        try {
            return entityManager.createQuery(
                            "select config from CauHinhNghiepVu config "
                                    + "where config.nhaTruong.id = :schoolId",
                            CauHinhNghiepVu.class)
                    .setParameter("schoolId", schoolIds.get(0))
                    .setLockMode(LockModeType.PESSIMISTIC_READ)
                    .setMaxResults(1)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElseThrow(PostgisTripLocationRepository::configurationUnavailable);
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private BanGhiDinhVi findDuplicate(Long tripId, Instant observedAt, Point position) {
        return entityManager.createQuery(
                        "select record from BanGhiDinhVi record "
                                + "where record.chuyenDi.id = :tripId "
                                + "and record.thoiGianTrinhDuyet = :observedAt "
                                + "order by record.thuTuBanGhi asc",
                        BanGhiDinhVi.class)
                .setParameter("tripId", tripId)
                .setParameter("observedAt", observedAt)
                .getResultList()
                .stream()
                .filter(record -> sameCoordinate(record.getToaDo(), position))
                .findFirst()
                .orElse(null);
    }

    private BanGhiDinhVi findCurrentObservation(Long tripId) {
        String sql = """
                        SELECT id
                        FROM ban_ghi_dinh_vi
                        WHERE chuyen_di_id = :tripId
                        """ + TripLocationCurrentOrdering.SQL_ORDER_BY + " LIMIT 1";
        List<?> ids = entityManager.createNativeQuery(sql)
                .setParameter("tripId", tripId)
                .getResultList();
        if (ids.isEmpty() || !(ids.get(0) instanceof Number id)) {
            return ids.isEmpty() ? null : invariantResultType();
        }
        return entityManager.find(BanGhiDinhVi.class, id.longValue());
    }


    private static BanGhiDinhVi invariantResultType() {
        throw invariantViolation();
    }

    private long nextSequence(Long tripId) {
        Long next = entityManager.createQuery(
                        "select coalesce(max(record.thuTuBanGhi), 0) + 1 "
                                + "from BanGhiDinhVi record "
                                + "where record.chuyenDi.id = :tripId",
                        Long.class)
                .setParameter("tripId", tripId)
                .getSingleResult();
        return next == null ? 1L : next;
    }

    private static boolean sameCoordinate(Point left, Point right) {
        return left != null && right != null
                && !left.isEmpty() && !right.isEmpty()
                && left.getSRID() == Wgs84Coordinates.SRID
                && right.getSRID() == Wgs84Coordinates.SRID
                && Double.compare(left.getX(), right.getX()) == 0
                && Double.compare(left.getY(), right.getY()) == 0;
    }

    private static Point copyPoint(Point point) {
        Point copy = (Point) point.copy();
        copy.setSRID(Wgs84Coordinates.SRID);
        return copy;
    }

    private static void requireTrackingActive(ChuyenDi trip) {
        TrangThaiVanHanhChuyenDi status = trip.getTrangThaiVanHanh();
        if (status != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                && status != TrangThaiVanHanhChuyenDi.SECURITY_FROZEN) {
            throw notActive();
        }
        if (trip.getBatDauLuc() == null || trip.getKetThucLuc() != null
                || trip.getLoTrinhChiaSe() == null
                || trip.getLoTrinhChiaSe().getId() == null
                || trip.getLoTrinhChiaSe().getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || trip.getLoTrinhChiaSe().getChuyenDi() == null
                || !Objects.equals(trip.getId(), trip.getLoTrinhChiaSe().getChuyenDi().getId())) {
            throw invariantViolation();
        }
    }

    private static void requireLocationConfiguration(CauHinhNghiepVu configuration) {
        if (configuration == null
                || configuration.getChuKyGuiViTriGiay() == null
                || configuration.getChuKyGuiViTriGiay() <= 0
                || configuration.getSoNgayLuuViTri() == null
                || configuration.getSoNgayLuuViTri() <= 0) {
            throw configurationUnavailable();
        }
    }

    private static void requireCommand(TripLocationCommitCommand command) {
        if (command == null
                || command.actorId() == null || command.actorId() <= 0
                || command.tripId() == null || command.tripId() <= 0
                || command.position() == null || command.position().isEmpty()
                || command.position().getSRID() != Wgs84Coordinates.SRID
                || !Wgs84Coordinates.isValidLongitudeLatitude(command.position().getX(), command.position().getY())
                || command.observedAt() == null
                || command.receivedAt() == null
                || (command.accuracyMeters() != null && command.accuracyMeters().signum() < 0)) {
            throw new IllegalArgumentException("TripLocationCommitCommand không hợp lệ.");
        }
    }

    private static TripLocationCommitResult result(
            Long tripId,
            BanGhiDinhVi record,
            TripLocationCommitOutcome outcome,
            boolean currentLocationUpdated,
            Long recommendedSubmissionIntervalSeconds) {
        TripCurrentLocationFact currentFact = currentLocationUpdated
                ? new TripCurrentLocationFact(
                        tripId,
                        BigDecimal.valueOf(record.getToaDo().getY()),
                        BigDecimal.valueOf(record.getToaDo().getX()),
                        record.getThoiGianTrinhDuyet(),
                        record.getThoiGianServerNhan(),
                        record.getDoChinhXacMet(),
                        record.getThuTuBanGhi())
                : null;
        return new TripLocationCommitResult(
                tripId,
                record.getId(),
                outcome,
                record.getThoiGianTrinhDuyet(),
                record.getThoiGianServerNhan(),
                currentLocationUpdated,
                recommendedSubmissionIntervalSeconds,
                currentFact);
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "Không tìm thấy chuyến đi.");
    }

    private static BusinessException notActive() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_LOCATION_NOT_ACTIVE",
                "Chuyến đi hiện không thuộc tracking lifecycle.");
    }

    private static BusinessException configurationUnavailable() {
        return new BusinessException(HttpStatus.CONFLICT, "BUSINESS_CONFIGURATION_UNAVAILABLE",
                "Cấu hình nghiệp vụ vị trí hiện không khả dụng.");
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Chuyến đi vừa được thay đổi bởi thao tác khác. Vui lòng thử lại.");
    }

    private static BusinessException dataIntegrityViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "Không thể ghi nhận vị trí do xung đột dữ liệu.");
    }

    private static BusinessException invariantViolation() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "TRIP_LOCATION_INVARIANT_VIOLATION",
                "Dữ liệu chuyến đi không nhất quán để ghi nhận vị trí.");
    }
}
