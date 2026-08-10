package com.zanh.route_sharing.repository.sharedroute.tripformation.jpa;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.DiemDungHanhTrinh;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiLoTrinh;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.eligibility.OperationalEligibilityRepository;
import com.zanh.route_sharing.repository.sharedroute.eligibility.model.CurrentOperationalEligibility;
import com.zanh.route_sharing.repository.sharedroute.tripformation.TripFormationRepository;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationCommitResult;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationPersistedView;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationPreparation;
import com.zanh.route_sharing.repository.sharedroute.tripformation.model.TripFormationStopView;
import com.zanh.route_sharing.service.tripformation.model.PlannedTripStop;
import com.zanh.route_sharing.service.tripformation.model.TripFormationBookingSnapshot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class JpaTripFormationRepository implements TripFormationRepository {

    private static final ZoneId BUSINESS_ZONE = TimePolicy.BUSINESS_ZONE;
    private static final Set<TrangThaiYeuCau> ACTIVE_STATES = EnumSet.of(
            TrangThaiYeuCau.PENDING,
            TrangThaiYeuCau.ACCEPTED);

    private final EntityManager entityManager;
    private final OperationalEligibilityRepository eligibilityRepository;

    public JpaTripFormationRepository(
            EntityManager entityManager,
            OperationalEligibilityRepository eligibilityRepository) {
        this.entityManager = entityManager;
        this.eligibilityRepository = eligibilityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TripFormationPreparation> prepare(Long actorId, Long routeId) {
        Optional<LoTrinhChiaSe> routeOptional = findOwnedRoute(actorId, routeId, null);
        if (routeOptional.isEmpty()) {
            return Optional.empty();
        }
        LoTrinhChiaSe route = routeOptional.orElseThrow();
        TripFormationPersistedView existing = findTripView(routeId).orElse(null);
        List<YeuCauDiChung> activeRequests = findActiveRequests(routeId, null);

        CurrentContext current = currentContext(actorId, route, activeRequests, null);
        return Optional.of(new TripFormationPreparation(
                route.getId(),
                route.getVersion(),
                route.getTrangThaiLoTrinh(),
                route.getSoGheConLai(),
                route.getThoiGianKhoiHanhDuKien(),
                route.getTaiXe().getId(),
                route.getPhuongTien().getId(),
                route.getPhuongTien().getVersion(),
                route.getPhuongTien().getDongXe().getLoaiPhuongTien(),
                copy(route.getDiemXuatPhat()),
                route.getDiaChiXuatPhat(),
                copy(route.getDiemDichTaiXe()),
                route.getDiaChiDichTaiXe(),
                copy(route.getTuyenDuongGoc()),
                current.schoolId(),
                current.configurationId(),
                current.configurationVersion(),
                current.arrivalRadiusMeters(),
                current.eligibility(),
                activeRequests.stream().map(JpaTripFormationRepository::snapshot).toList(),
                existing));
    }

    @Override
    @Transactional
    public TripFormationCommitResult commit(TripFormationCommitCommand command) {
        try {
            LoTrinhChiaSe route = findOwnedRoute(
                    command.actorId(),
                    command.routeId(),
                    LockModeType.PESSIMISTIC_WRITE)
                    .orElseThrow(() -> notFound());

            if (route.getTrangThaiLoTrinh() == TrangThaiLoTrinh.LOCKED) {
                TripFormationPersistedView existing = findTripView(route.getId())
                        .orElseThrow(JpaTripFormationRepository::invariantViolation);
                return new TripFormationCommitResult(false, existing, List.of(), List.of());
            }
            if (route.getTrangThaiLoTrinh() != TrangThaiLoTrinh.OPEN) {
                throw notOpen();
            }

            requireRouteFresh(route, command.preparation());
            List<YeuCauDiChung> activeRequests = findActiveRequests(
                    route.getId(),
                    LockModeType.PESSIMISTIC_WRITE);
            if (activeRequests.stream().anyMatch(request -> request.getTrangThaiYeuCau() == TrangThaiYeuCau.PENDING)) {
                throw pendingRemains();
            }
            List<YeuCauDiChung> accepted = activeRequests.stream()
                    .filter(request -> request.getTrangThaiYeuCau() == TrangThaiYeuCau.ACCEPTED)
                    .toList();
            if (accepted.isEmpty()) {
                throw noAcceptedBookings();
            }
            requireAcceptedSetFresh(accepted, command.preparation());

            CurrentContext current = currentContext(
                    command.actorId(),
                    route,
                    activeRequests,
                    LockModeType.PESSIMISTIC_READ);
            requireCurrentContextFresh(current, command.preparation());
            if (current.eligibility() == null || !current.eligibility().eligible()) {
                throw ineligible();
            }

            Map<Long, YeuCauDiChung> acceptedById = accepted.stream()
                    .collect(Collectors.toMap(YeuCauDiChung::getId, Function.identity()));
            requireStopPlanMatches(command.orderedStops(), acceptedById.keySet());

            ChuyenDi trip = ChuyenDi.preparing(
                    route,
                    accepted.size(),
                    command.operationalRoutePlan().geometry());
            entityManager.persist(trip);
            entityManager.flush();

            for (YeuCauDiChung request : accepted) {
                request.assignToTrip(trip);
            }
            route.lockForTripFormation(trip, command.formedAt());

            List<DiemDungHanhTrinh> stops = new ArrayList<>(command.orderedStops().size());
            for (PlannedTripStop planned : command.orderedStops()) {
                YeuCauDiChung request = planned.rideRequestId() == null
                        ? null
                        : acceptedById.get(planned.rideRequestId());
                DiemDungHanhTrinh stop = DiemDungHanhTrinh.planned(
                        trip,
                        request,
                        planned.order(),
                        planned.point(),
                        planned.address(),
                        planned.type(),
                        current.arrivalRadiusMeters());
                entityManager.persist(stop);
                trip.getDanhSachDiemDung().add(stop);
                stops.add(stop);
            }

            long auditSequence = nextRouteAuditSequence(route.getId());
            entityManager.persist(NhatKyTrangThaiLoTrinh.tripFormed(
                    route,
                    route.getTaiXe(),
                    command.formedAt(),
                    auditSequence));

            List<Long> recipients = new ArrayList<>(accepted.size());
            List<Long> rideRequestIds = new ArrayList<>(accepted.size());
            for (YeuCauDiChung request : accepted) {
                entityManager.persist(ThongBao.routeLockedForTrip(request, trip));
                recipients.add(request.getHanhKhach().getId());
                rideRequestIds.add(request.getId());
            }
            entityManager.flush();

            return new TripFormationCommitResult(
                    true,
                    toView(route, trip, stops, command.formedAt()),
                    recipients,
                    rideRequestIds);
        } catch (PessimisticLockException | LockTimeoutException | OptimisticLockException exception) {
            throw concurrentModification();
        } catch (PersistenceException exception) {
            throw dataIntegrityViolation();
        }
    }

    private Optional<LoTrinhChiaSe> findOwnedRoute(
            Long actorId,
            Long routeId,
            LockModeType lockMode) {
        try {
            var query = entityManager.createQuery(
                            "select route from LoTrinhChiaSe route "
                                    + "join fetch route.taiXe driver "
                                    + "join fetch route.phuongTien vehicle "
                                    + "join fetch vehicle.dongXe model "
                                    + "where route.id = :routeId and driver.id = :actorId",
                            LoTrinhChiaSe.class)
                    .setParameter("routeId", routeId)
                    .setParameter("actorId", actorId)
                    .setMaxResults(1);
            if (lockMode != null) {
                query.setLockMode(lockMode);
            }
            return query.getResultList().stream().findFirst();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private List<YeuCauDiChung> findActiveRequests(Long routeId, LockModeType lockMode) {
        try {
            var query = entityManager.createQuery(
                            "select request from YeuCauDiChung request "
                                    + "join fetch request.hanhKhach passenger "
                                    + "join fetch request.cauHinhLucGui configuration "
                                    + "join fetch configuration.nhaTruong school "
                                    + "where request.loTrinhChiaSe.id = :routeId "
                                    + "and request.trangThaiYeuCau in :states "
                                    + "order by request.id asc",
                            YeuCauDiChung.class)
                    .setParameter("routeId", routeId)
                    .setParameter("states", ACTIVE_STATES);
            if (lockMode != null) {
                query.setLockMode(lockMode);
            }
            return query.getResultList();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private CurrentContext currentContext(
            Long actorId,
            LoTrinhChiaSe route,
            List<YeuCauDiChung> activeRequests,
            LockModeType configurationLockMode) {
        YeuCauDiChung reference = activeRequests.stream()
                .filter(request -> request.getTrangThaiYeuCau() == TrangThaiYeuCau.ACCEPTED)
                .findFirst()
                .orElse(null);
        if (reference == null) {
            return new CurrentContext(null, null, null, null, null);
        }

        Set<Long> schoolIds = activeRequests.stream()
                .filter(request -> request.getTrangThaiYeuCau() == TrangThaiYeuCau.ACCEPTED)
                .map(request -> request.getCauHinhLucGui().getNhaTruong().getId())
                .collect(Collectors.toSet());
        if (schoolIds.size() != 1) {
            throw invariantViolation();
        }
        Long schoolId = schoolIds.iterator().next();
        CauHinhNghiepVu configuration = findCurrentConfiguration(schoolId, configurationLockMode)
                .orElse(null);
        if (configuration == null) {
            return new CurrentContext(schoolId, null, null, null,
                    CurrentOperationalEligibility.ineligible());
        }
        CurrentOperationalEligibility eligibility = eligibilityRepository.evaluate(
                actorId,
                route.getId(),
                schoolId,
                LocalDate.ofInstant(route.getThoiGianKhoiHanhDuKien(), BUSINESS_ZONE));
        return new CurrentContext(
                schoolId,
                configuration.getId(),
                configuration.getVersion(),
                configuration.getBanKinhXacDinhDaDenMet(),
                eligibility);
    }

    private Optional<CauHinhNghiepVu> findCurrentConfiguration(
            Long schoolId,
            LockModeType lockMode) {
        try {
            var query = entityManager.createQuery(
                            "select configuration from CauHinhNghiepVu configuration "
                                    + "join fetch configuration.nhaTruong school "
                                    + "where school.id = :schoolId and school.dangHoatDong = true",
                            CauHinhNghiepVu.class)
                    .setParameter("schoolId", schoolId)
                    .setMaxResults(1);
            if (lockMode != null) {
                query.setLockMode(lockMode);
            }
            return query.getResultList().stream().findFirst();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private Optional<TripFormationPersistedView> findTripView(Long routeId) {
        List<ChuyenDi> trips = entityManager.createQuery(
                        "select distinct trip from ChuyenDi trip "
                                + "join fetch trip.loTrinhChiaSe route "
                                + "left join fetch trip.danhSachDiemDung stop "
                                + "left join fetch stop.yeuCauDiChung request "
                                + "where route.id = :routeId",
                        ChuyenDi.class)
                .setParameter("routeId", routeId)
                .getResultList();
        if (trips.isEmpty()) {
            return Optional.empty();
        }
        ChuyenDi trip = trips.get(0);
        return Optional.of(toView(
                trip.getLoTrinhChiaSe(),
                trip,
                trip.getDanhSachDiemDung(),
                trip.getLoTrinhChiaSe().getChotDanhSachLuc()));
    }

    private long nextRouteAuditSequence(Long routeId) {
        Long next = entityManager.createQuery(
                        "select coalesce(max(event.sequence), 0) + 1 "
                                + "from NhatKyTrangThaiLoTrinh event "
                                + "where event.loTrinhChiaSe.id = :routeId",
                        Long.class)
                .setParameter("routeId", routeId)
                .getSingleResult();
        return next == null ? 1L : next;
    }

    private static TripFormationBookingSnapshot snapshot(YeuCauDiChung request) {
        return new TripFormationBookingSnapshot(
                request.getId(),
                request.getVersion(),
                request.getHanhKhach().getId(),
                request.getTrangThaiYeuCau(),
                copy(request.getDiemDonThucTe()),
                request.getDiaChiDonThucTe(),
                copy(request.getDiemThaDeXuat()),
                request.getDiaChiDiemTha());
    }

    private static void requireRouteFresh(
            LoTrinhChiaSe route,
            TripFormationPreparation preparation) {
        if (!Objects.equals(route.getVersion(), preparation.routeVersion())
                || !Objects.equals(route.getPhuongTien().getId(), preparation.vehicleId())
                || !Objects.equals(route.getPhuongTien().getVersion(), preparation.vehicleVersion())
                || route.getPhuongTien().getDongXe().getLoaiPhuongTien() != preparation.vehicleType()) {
            throw stale();
        }
    }

    private static void requireAcceptedSetFresh(
            List<YeuCauDiChung> accepted,
            TripFormationPreparation preparation) {
        List<TripFormationBookingSnapshot> expected = preparation.activeRequests().stream()
                .filter(snapshot -> snapshot.status() == TrangThaiYeuCau.ACCEPTED)
                .toList();
        if (accepted.size() != expected.size()) {
            throw stale();
        }
        for (int index = 0; index < accepted.size(); index++) {
            YeuCauDiChung current = accepted.get(index);
            TripFormationBookingSnapshot snapshot = expected.get(index);
            if (!Objects.equals(current.getId(), snapshot.rideRequestId())
                    || !Objects.equals(current.getVersion(), snapshot.rideRequestVersion())) {
                throw stale();
            }
        }
    }

    private static void requireCurrentContextFresh(
            CurrentContext current,
            TripFormationPreparation preparation) {
        if (!Objects.equals(current.schoolId(), preparation.schoolId())
                || !Objects.equals(current.configurationId(), preparation.configurationId())
                || !Objects.equals(current.configurationVersion(), preparation.configurationVersion())
                || current.arrivalRadiusMeters() == null
                || preparation.arrivalRadiusMeters() == null
                || current.arrivalRadiusMeters().compareTo(preparation.arrivalRadiusMeters()) != 0) {
            throw stale();
        }
    }

    private static void requireStopPlanMatches(
            List<PlannedTripStop> stops,
            Set<Long> acceptedRequestIds) {
        if (stops == null || stops.size() != 2 + acceptedRequestIds.size() * 2) {
            throw planInconsistent();
        }
        for (int index = 0; index < stops.size(); index++) {
            PlannedTripStop stop = stops.get(index);
            if (stop == null || stop.order() != index + 1 || stop.type() == null
                    || stop.point() == null || stop.point().isEmpty() || stop.point().getSRID() != Wgs84Coordinates.SRID
                    || stop.address() == null || stop.address().isBlank()) {
                throw planInconsistent();
            }
        }
        if (stops.get(0).type() != LoaiDiemDung.DRIVER_START
                || stops.get(stops.size() - 1).type() != LoaiDiemDung.DRIVER_END
                || stops.get(0).rideRequestId() != null
                || stops.get(stops.size() - 1).rideRequestId() != null) {
            throw planInconsistent();
        }
        long driverStartCount = stops.stream().filter(stop -> stop.type() == LoaiDiemDung.DRIVER_START).count();
        long driverEndCount = stops.stream().filter(stop -> stop.type() == LoaiDiemDung.DRIVER_END).count();
        if (driverStartCount != 1 || driverEndCount != 1) {
            throw planInconsistent();
        }

        Set<Long> referencedRequestIds = stops.stream()
                .filter(stop -> stop.rideRequestId() != null)
                .map(PlannedTripStop::rideRequestId)
                .collect(Collectors.toSet());
        if (!referencedRequestIds.equals(acceptedRequestIds)) {
            throw planInconsistent();
        }

        Map<Long, Long> pickupCount = stops.stream()
                .filter(stop -> stop.rideRequestId() != null && stop.type() == LoaiDiemDung.PICKUP)
                .collect(Collectors.groupingBy(PlannedTripStop::rideRequestId, Collectors.counting()));
        Map<Long, Long> dropoffCount = stops.stream()
                .filter(stop -> stop.rideRequestId() != null && stop.type() == LoaiDiemDung.DROPOFF)
                .collect(Collectors.groupingBy(PlannedTripStop::rideRequestId, Collectors.counting()));
        for (Long requestId : acceptedRequestIds) {
            if (pickupCount.getOrDefault(requestId, 0L) != 1L
                    || dropoffCount.getOrDefault(requestId, 0L) != 1L) {
                throw planInconsistent();
            }
        }
    }

    private static TripFormationPersistedView toView(
            LoTrinhChiaSe route,
            ChuyenDi trip,
            List<DiemDungHanhTrinh> stops,
            java.time.Instant formedAt) {
        return new TripFormationPersistedView(
                route.getId(),
                route.getTrangThaiLoTrinh(),
                route.getChotDanhSachLuc(),
                route.getSoGheConLai(),
                route.getThoiGianKhoiHanhDuKien(),
                trip.getId(),
                trip.getTrangThaiVanHanh(),
                formedAt,
                trip.getSoKhachKeHoach(),
                trip.getSoKhachThucTe(),
                copy(trip.getTuyenDuongVanHanh()),
                stops.stream()
                        .sorted(java.util.Comparator.comparingInt(DiemDungHanhTrinh::getThuTu))
                        .map(stop -> new TripFormationStopView(
                                stop.getId(),
                                stop.getThuTu(),
                                stop.getLoaiDiemDung(),
                                stop.getTrangThaiDiemDung(),
                                stop.getYeuCauDiChung() == null ? null : stop.getYeuCauDiChung().getId(),
                                copy(stop.getToaDoKeHoach()),
                                stop.getDiaChi(),
                                stop.getBanKinhXacDinhDaDenMet()))
                        .toList());
    }

    private static Point copy(Point point) {
        if (point == null) {
            return null;
        }
        Point copy = (Point) point.copy();
        copy.setSRID(Wgs84Coordinates.SRID);
        return copy;
    }

    private static LineString copy(LineString lineString) {
        if (lineString == null) {
            return null;
        }
        LineString copy = (LineString) lineString.copy();
        copy.setSRID(Wgs84Coordinates.SRID);
        return copy;
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "SHARED_ROUTE_NOT_FOUND", "Không tìm thấy lộ trình chia sẻ.");
    }

    private static BusinessException notOpen() {
        return new BusinessException(HttpStatus.CONFLICT, "SHARED_ROUTE_NOT_OPEN", "Lộ trình không còn mở để hình thành chuyến đi.");
    }

    private static BusinessException pendingRemains() {
        return new BusinessException(HttpStatus.CONFLICT, "SHARED_ROUTE_PENDING_REQUESTS_REMAIN", "Lộ trình vẫn còn yêu cầu PENDING cần được xử lý.");
    }

    private static BusinessException noAcceptedBookings() {
        return new BusinessException(HttpStatus.CONFLICT, "SHARED_ROUTE_NO_ACCEPTED_BOOKINGS", "Lộ trình chưa có booking ACCEPTED để hình thành chuyến đi.");
    }

    private static BusinessException ineligible() {
        return new BusinessException(HttpStatus.CONFLICT, "DRIVER_OR_VEHICLE_INELIGIBLE", "Tài xế hoặc phương tiện không còn đủ điều kiện hình thành chuyến đi.");
    }

    private static BusinessException stale() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_FORMATION_STALE", "Dữ liệu lộ trình hoặc booking đã thay đổi trong lúc chuẩn bị chuyến đi. Vui lòng tải lại và thử lại.");
    }

    private static BusinessException planInconsistent() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_FORMATION_PLAN_INCONSISTENT", "Kế hoạch điểm dừng không nhất quán với tập booking đã chấp nhận.");
    }

    private static BusinessException invariantViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "TRIP_FORMATION_INVARIANT_VIOLATION", "Trạng thái lộ trình và chuyến đi đang không nhất quán.");
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(HttpStatus.CONFLICT, "SHARED_ROUTE_CONCURRENTLY_MODIFIED", "Lộ trình hoặc booking đang được xử lý bởi thao tác khác. Vui lòng thử lại.");
    }

    private static BusinessException dataIntegrityViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Dữ liệu xung đột với ràng buộc hệ thống.");
    }

    private record CurrentContext(
            Long schoolId,
            Long configurationId,
            Long configurationVersion,
            BigDecimal arrivalRadiusMeters,
            CurrentOperationalEligibility eligibility) {
    }
}
