package com.zanh.route_sharing.repository.sharedroute.riderequest.postgis;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.domain.riderequest.RideRequestPointSnapshot;
import com.zanh.route_sharing.domain.riderequest.RideRequestPolicySnapshot;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.preview.SharedRoutePreviewRepository;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewConsistencyToken;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewEvaluation;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewEvaluationStatus;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewCriteria;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewPreparation;
import com.zanh.route_sharing.repository.sharedroute.riderequest.RideRequestCreationRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCommitCommand;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestCriteria;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluation;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestEvaluationStatus;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestGeoPoint;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPersistedView;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPreparation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class PostgisRideRequestCreationRepository implements RideRequestCreationRepository {

    private static final Set<TrangThaiYeuCau> BLOCKING_STATES = EnumSet.of(
            TrangThaiYeuCau.PENDING,
            TrangThaiYeuCau.ACCEPTED,
            TrangThaiYeuCau.ON_BOARD,
            TrangThaiYeuCau.DISPUTED);

    private final EntityManager entityManager;
    private final SharedRoutePreviewRepository previewRepository;

    public PostgisRideRequestCreationRepository(
            EntityManager entityManager,
            SharedRoutePreviewRepository previewRepository) {
        this.entityManager = entityManager;
        this.previewRepository = previewRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RideRequestEvaluation evaluate(RideRequestCriteria criteria) {
        Optional<BlockingRequest> blocking = findBlocking(criteria.actorUserId());
        if (blocking.isPresent()) {
            BlockingRequest existing = blocking.orElseThrow();
            return RideRequestEvaluation.unfinished(existing.id(), existing.status());
        }

        PreviewEvaluation preview = previewRepository.evaluate(
                new SharedRoutePreviewCriteria(
                        criteria.actorUserId(),
                        criteria.schoolId(),
                        criteria.routeId(),
                        criteria.pickupLatitude(),
                        criteria.pickupLongitude(),
                        criteria.destinationLatitude(),
                        criteria.destinationLongitude(),
                        criteria.now()));
        if (preview.status() != PreviewEvaluationStatus.ELIGIBLE) {
            return RideRequestEvaluation.ineligible(map(preview.status()));
        }

        SharedRoutePreviewPreparation source = preview.requirePreparation();
        Optional<Instant> cooldown = findActiveCooldown(
                criteria.actorUserId(),
                source.route().routeId(),
                criteria.now());
        if (cooldown.isPresent()) {
            return RideRequestEvaluation.cooldown(cooldown.orElseThrow());
        }

        PreviewConsistencyToken token = source.consistencyToken();
        RideRequestPolicySnapshot policy = new RideRequestPolicySnapshot(
                token.businessConfigId(),
                token.businessConfigVersion(),
                token.sameDestinationRadiusMeters(),
                token.destinationNearRouteRadiusMeters(),
                token.maxPickupDeviationMeters(),
                token.maxPickupDeviationSeconds(),
                token.minimumConvenienceRatioPercent(),
                Duration.ofSeconds(token.bookingCutoffSeconds()),
                Duration.ofSeconds(token.rejectionCooldownSeconds()));

        return RideRequestEvaluation.eligible(
                new RideRequestPreparation(
                        source.route().routeId(),
                        source.route().routeVersion(),
                        source.driver().id(),
                        source.vehicle().vehicleType(),
                        source.route().expectedDepartureTime(),
                        source.route().remainingSeats(),
                        source.route().suggestedSupportPerKm(),
                        source.match().matchType(),
                        source.match().dropoffType(),
                        new RideRequestGeoPoint(
                                source.match().pickupProjection().latitude(),
                                source.match().pickupProjection().longitude()),
                        new RideRequestGeoPoint(
                                source.match().proposedDropoff().latitude(),
                                source.match().proposedDropoff().longitude()),
                        policy,
                        token));
    }

    @Override
    @Transactional
    public RideRequestPersistedView commit(RideRequestCommitCommand command) {
        NguoiDung passenger = entityManager.find(
                NguoiDung.class,
                command.actorUserId(),
                LockModeType.PESSIMISTIC_WRITE);
        if (passenger == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "SHARED_ROUTE_NOT_FOUND",
                    "Không tìm thấy lộ trình chia sẻ phù hợp.");
        }

        Optional<BlockingRequest> blocking = findBlocking(command.actorUserId());
        if (blocking.isPresent()) {
            BlockingRequest existing = blocking.orElseThrow();
            throw unfinished(existing);
        }

        validateCommandConsistency(command);
        Optional<Instant> cooldown = findActiveCooldown(
                command.actorUserId(),
                command.routeId(),
                command.sentAt());
        if (cooldown.isPresent()) {
            throw cooldownActive(cooldown.orElseThrow());
        }

        if (!previewRepository.remainsCurrent(command.consistencyToken(), command.sentAt())) {
            throw stale();
        }
        requireBookingWindowOpen(command);

        LoTrinhChiaSe route = entityManager.find(
                LoTrinhChiaSe.class,
                command.routeId(),
                LockModeType.PESSIMISTIC_READ);
        if (route == null || !command.snapshot().routeVersion().equals(route.getVersion())) {
            throw stale();
        }
        NguoiDung driver = entityManager.getReference(
                NguoiDung.class,
                command.snapshot().driverId());
        CauHinhNghiepVu configuration = entityManager.getReference(
                CauHinhNghiepVu.class,
                command.snapshot().policy().configurationId());

        YeuCauDiChung rideRequest = YeuCauDiChung.pending(
                passenger,
                route,
                driver,
                configuration,
                command.snapshot(),
                command.sentAt(),
                command.note());

        try {
            entityManager.persist(rideRequest);
            entityManager.flush();

            entityManager.persist(NhatKyTrangThaiYeuCau.created(
                    rideRequest,
                    passenger,
                    command.sentAt()));
            entityManager.persist(ThongBao.bookingRequest(rideRequest, driver));
            entityManager.flush();
        } catch (PersistenceException exception) {
            throw mapConstraint(exception);
        }

        return toCreationView(rideRequest);
    }

    private Optional<BlockingRequest> findBlocking(Long actorUserId) {
        List<Object[]> results = entityManager.createQuery(
                        "select request.id, request.trangThaiYeuCau "
                                + "from YeuCauDiChung request "
                                + "where request.hanhKhach.id = :actorUserId "
                                + "and request.trangThaiYeuCau in :blockingStates "
                                + "order by request.id asc",
                        Object[].class)
                .setParameter("actorUserId", actorUserId)
                .setParameter("blockingStates", BLOCKING_STATES)
                .setMaxResults(1)
                .getResultList();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = results.get(0);
        return Optional.of(new BlockingRequest(
                (Long) row[0],
                (TrangThaiYeuCau) row[1]));
    }

    private Optional<Instant> findActiveCooldown(
            Long actorUserId,
            Long routeId,
            Instant now) {
        List<Instant> results = entityManager.createQuery(
                        "select request.cooldownUntil "
                                + "from YeuCauDiChung request "
                                + "where request.hanhKhach.id = :actorUserId "
                                + "and request.loTrinhChiaSe.id = :routeId "
                                + "and request.trangThaiYeuCau = :rejected "
                                + "and request.cooldownUntil > :now "
                                + "order by request.cooldownUntil desc, request.id desc",
                        Instant.class)
                .setParameter("actorUserId", actorUserId)
                .setParameter("routeId", routeId)
                .setParameter("rejected", TrangThaiYeuCau.REJECTED)
                .setParameter("now", now)
                .setMaxResults(1)
                .getResultList();
        return results.stream().findFirst();
    }

    private static RideRequestEvaluationStatus map(PreviewEvaluationStatus source) {
        return switch (source) {
            case NOT_FOUND_OR_INACCESSIBLE -> RideRequestEvaluationStatus.NOT_FOUND_OR_INACCESSIBLE;
            case ROUTE_UNAVAILABLE -> RideRequestEvaluationStatus.ROUTE_UNAVAILABLE;
            case SELF_ROUTE -> RideRequestEvaluationStatus.SELF_ROUTE;
            case DRIVER_OR_VEHICLE_INELIGIBLE ->
                    RideRequestEvaluationStatus.DRIVER_OR_VEHICLE_INELIGIBLE;
            case NO_LONGER_MATCHES -> RideRequestEvaluationStatus.NO_LONGER_MATCHES;
            case ELIGIBLE -> throw new IllegalArgumentException("ELIGIBLE không phải lỗi");
        };
    }

    private static void validateCommandConsistency(RideRequestCommitCommand command) {
        PreviewConsistencyToken token = command.consistencyToken();
        RideRequestPolicySnapshot policy = command.snapshot().policy();
        if (!command.actorUserId().equals(token.actorUserId())
                || !command.routeId().equals(token.routeId())
                || !command.snapshot().routeVersion().equals(token.routeVersion())
                || !command.snapshot().driverId().equals(token.driverId())
                || !policy.configurationId().equals(token.businessConfigId())
                || !policy.configurationVersion().equals(token.businessConfigVersion())) {
            throw stale();
        }
    }

    private static void requireBookingWindowOpen(RideRequestCommitCommand command) {
        final Instant cutoffBoundary;
        try {
            cutoffBoundary = command.consistencyToken().expectedDepartureTime()
                    .minusSeconds(command.consistencyToken().bookingCutoffSeconds());
        } catch (RuntimeException exception) {
            throw cutoffReached();
        }
        if (!command.sentAt().isBefore(cutoffBoundary)) {
            throw cutoffReached();
        }
    }

    /**
     * Builds the immutable response returned immediately after creating a
     * {@code PENDING} ride request.
     */
    private static RideRequestPersistedView toCreationView(YeuCauDiChung entity) {
        return new RideRequestPersistedView(
                entity.getId(),
                entity.getLoTrinhChiaSe().getId(),
                TrangThaiYeuCau.PENDING,
                entity.getGuiLuc(),
                entity.getLoaiGhepTuyen(),
                entity.getLoaiDiemTha(),
                new RideRequestPointSnapshot(
                        entity.getDiemDonThucTe(),
                        entity.getDiaChiDonThucTe()),
                new RideRequestPointSnapshot(
                        entity.getDiemDichCuoiCungMongMuon(),
                        entity.getDiaChiDichCuoiCung()),
                new RideRequestPointSnapshot(
                        entity.getDiemThaDeXuat(),
                        entity.getDiaChiDiemTha()),
                entity.getKhoangCachLechDeDonMet(),
                entity.getThoiGianLechDeDonGiay(),
                entity.getTongKhoangCachMongMuonMet(),
                entity.getKhoangCachDuocPhucVuMet(),
                entity.getKhoangCachConLaiMet(),
                entity.getTyLeTienDuong(),
                entity.getMucHoTroGoiYMoiKmLucGui(),
                entity.getMucHoTroHanhKhachDeNghi(),
                null);
    }

    private static BusinessException unfinished(BlockingRequest existing) {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "UNFINISHED_RIDE_REQUEST_ALREADY_EXISTS",
                "Bạn đang có một yêu cầu hoặc chuyến đi chưa kết thúc.",
                Map.of(
                        "existingRideRequestId", existing.id().toString(),
                        "status", existing.status().name()));
    }

    private static BusinessException cooldownActive(Instant cooldownUntil) {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "RIDE_REQUEST_REJECTION_COOLDOWN_ACTIVE",
                "Bạn cần chờ hết thời gian tạm nghỉ trước khi gửi lại yêu cầu cho lộ trình này.",
                Map.of("cooldownUntil", cooldownUntil.toString()));
    }

    private static BusinessException stale() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "RIDE_REQUEST_STALE",
                "Lộ trình đã thay đổi trong lúc xử lý yêu cầu. Vui lòng tải lại và thử lại.");
    }

    private static BusinessException cutoffReached() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "SHARED_ROUTE_BOOKING_CUTOFF_REACHED",
                "Lộ trình đã hết thời gian nhận yêu cầu đi chung.");
    }

    private static BusinessException mapConstraint(PersistenceException exception) {
        String constraint = constraintName(exception);
        if ("uk_yeu_cau_hanh_khach_blocking".equals(constraint)) {
            return new BusinessException(
                    HttpStatus.CONFLICT,
                    "UNFINISHED_RIDE_REQUEST_ALREADY_EXISTS",
                    "Bạn đang có một yêu cầu hoặc chuyến đi chưa kết thúc.");
        }
        return new BusinessException(
                HttpStatus.CONFLICT,
                "CONCURRENT_MODIFICATION",
                "Dữ liệu đã thay đổi bởi một yêu cầu khác. Vui lòng thử lại.");
    }

    private static String constraintName(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
            current = current.getCause();
        }
        return null;
    }

    private record BlockingRequest(Long id, TrangThaiYeuCau status) {
    }
}
