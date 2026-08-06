package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.decision.RideRequestDecisionRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.decision.model.CurrentAcceptEligibility;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.RideRequestDecisionService;
import com.zanh.route_sharing.service.riderequest.decision.RideRequestActionabilityPolicy;
import com.zanh.route_sharing.service.riderequest.decision.model.RideRequestDecisionResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class RideRequestDecisionServiceImpl implements RideRequestDecisionService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RideRequestDecisionRepository repository;
    private final RideRequestActionabilityPolicy actionabilityPolicy;
    private final Clock clock;

    public RideRequestDecisionServiceImpl(
            RideRequestDecisionRepository repository,
            RideRequestActionabilityPolicy actionabilityPolicy,
            Clock clock) {
        this.repository = repository;
        this.actionabilityPolicy = actionabilityPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RideRequestDecisionResult accept(Long actorId, Long routeId, Long rideRequestId) {
        validateInput(actorId, routeId, rideRequestId);
        LoTrinhChiaSe route = ownedRoute(actorId, routeId);
        YeuCauDiChung request = rideRequest(routeId, rideRequestId);
        Instant decisionAt = clock.instant();
        requirePending(request);
        actionabilityPolicy.requireActionable(decisionAt, request.getExpiresAt());
        requireOpenRoute(route);

        CauHinhNghiepVu configuration = currentConfiguration(request);
        CurrentAcceptEligibility eligibility = repository.evaluateCurrentAcceptEligibility(
                actorId,
                routeId,
                configuration.getNhaTruong().getId(),
                LocalDate.ofInstant(route.getThoiGianKhoiHanhDuKien(), BUSINESS_ZONE));
        if (!eligibility.eligible()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "DRIVER_OR_VEHICLE_INELIGIBLE",
                    "Tài xế hoặc phương tiện không còn đủ điều kiện nhận hành khách.");
        }
        actionabilityPolicy.requireAcceptWindowOpen(
                decisionAt,
                route.getThoiGianKhoiHanhDuKien(),
                configuration.getBookingCutoffSeconds());
        if (route.getSoGheConLai() == null || route.getSoGheConLai() <= 0) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "SHARED_ROUTE_NO_REMAINING_SEATS",
                    "Lộ trình không còn ghế trống.");
        }

        route.allocateOneSeat();
        request.accept(decisionAt);
        repository.appendStateLog(NhatKyTrangThaiYeuCau.accepted(
                request,
                route.getTaiXe(),
                decisionAt));
        repository.persistNotification(ThongBao.bookingAccepted(request));
        repository.flush();

        return result(route, request, decisionAt);
    }

    @Override
    @Transactional
    public RideRequestDecisionResult reject(Long actorId, Long routeId, Long rideRequestId) {
        validateInput(actorId, routeId, rideRequestId);
        LoTrinhChiaSe route = ownedRoute(actorId, routeId);
        YeuCauDiChung request = rideRequest(routeId, rideRequestId);
        Instant decisionAt = clock.instant();
        requirePending(request);
        actionabilityPolicy.requireActionable(decisionAt, request.getExpiresAt());
        requireOpenRoute(route);

        CauHinhNghiepVu configuration = currentConfiguration(request);
        Long cooldownSeconds = configuration.getRejectionCooldownSeconds();
        if (cooldownSeconds == null || cooldownSeconds < 0) {
            throw configurationUnavailable();
        }

        request.reject(decisionAt, configuration, cooldownSeconds);
        repository.appendStateLog(NhatKyTrangThaiYeuCau.rejected(
                request,
                route.getTaiXe(),
                decisionAt));
        repository.persistNotification(ThongBao.bookingRejected(request));
        repository.flush();

        return result(route, request, decisionAt);
    }

    private LoTrinhChiaSe ownedRoute(Long actorId, Long routeId) {
        return repository.lockOwnedRoute(actorId, routeId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "SHARED_ROUTE_NOT_FOUND",
                        "Không tìm thấy lộ trình chia sẻ."));
    }

    private YeuCauDiChung rideRequest(Long routeId, Long rideRequestId) {
        return repository.lockRideRequest(routeId, rideRequestId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RIDE_REQUEST_NOT_FOUND",
                        "Không tìm thấy yêu cầu đi chung trong lộ trình."));
    }

    private CauHinhNghiepVu currentConfiguration(YeuCauDiChung request) {
        return repository.lockCurrentConfiguration(request)
                .orElseThrow(RideRequestDecisionServiceImpl::configurationUnavailable);
    }

    private static void requirePending(YeuCauDiChung request) {
        if (request.getTrangThaiYeuCau() != TrangThaiYeuCau.PENDING) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_RIDE_REQUEST_STATE",
                    "Yêu cầu đi chung không còn ở trạng thái chờ xử lý.");
        }
    }

    private static void requireOpenRoute(LoTrinhChiaSe route) {
        if (route.getTrangThaiLoTrinh() != TrangThaiLoTrinh.OPEN) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "SHARED_ROUTE_NOT_OPEN",
                    "Lộ trình không còn mở để xử lý yêu cầu đi chung.");
        }
    }

    private static RideRequestDecisionResult result(
            LoTrinhChiaSe route,
            YeuCauDiChung request,
            Instant decisionAt) {
        return new RideRequestDecisionResult(
                route.getId(),
                request.getId(),
                request.getTrangThaiYeuCau(),
                decisionAt,
                route.getSoGheConLai(),
                request.getMucHoTroDaThoaThuan(),
                request.getCooldownUntil());
    }

    private static void validateInput(Long actorId, Long routeId, Long rideRequestId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (routeId == null || routeId <= 0 || rideRequestId == null || rideRequestId <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RIDE_REQUEST_DECISION_PATH",
                    "routeId và rideRequestId phải là số dương.");
        }
    }

    private static BusinessException configurationUnavailable() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "BUSINESS_CONFIGURATION_UNAVAILABLE",
                "Không tìm thấy cấu hình nghiệp vụ hiện hành để xử lý yêu cầu.");
    }
}
