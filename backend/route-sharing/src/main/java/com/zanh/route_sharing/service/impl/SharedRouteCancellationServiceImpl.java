package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiLoTrinh;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.sharedroute.cancellation.CancelSharedRouteResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.cancellation.SharedRouteCancellationRepository;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.SharedRouteCancellationService;
import com.zanh.route_sharing.service.sharedroute.cancellation.SharedRouteCancellationResponseMapper;
import com.zanh.route_sharing.service.sharedroute.cancellation.model.SharedRouteCancellationResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class SharedRouteCancellationServiceImpl implements SharedRouteCancellationService {
    private final SharedRouteCancellationRepository repository;
    private final SharedRouteCancellationResponseMapper mapper;
    private final Clock clock;

    public SharedRouteCancellationServiceImpl(
            SharedRouteCancellationRepository repository,
            SharedRouteCancellationResponseMapper mapper,
            Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CancelSharedRouteResponse cancelOwnedRoute(Long driverId, Long routeId, String reason) {
        validateInput(driverId, routeId, reason);
        String normalizedReason = reason.trim();

        LoTrinhChiaSe route = repository.lockOwnedRoute(driverId, routeId)
                .orElseThrow(SharedRouteCancellationServiceImpl::routeNotFound);
        requireOpenRoute(route);
        if (repository.existsTripForRoute(routeId) || route.getChuyenDi() != null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "SHARED_ROUTE_ALREADY_ASSIGNED_TO_TRIP",
                    "Lộ trình đã hình thành chuyến đi và phải dùng quy trình hủy chuyến.");
        }

        List<YeuCauDiChung> activeRequests = repository.lockActiveRequests(routeId);
        Instant cancelledAt = clock.instant();
        int pendingCancelled = 0;
        int acceptedCancelled = 0;
        int seatsRestored = 0;
        int passengersNotified = 0;

        try {
            for (YeuCauDiChung request : activeRequests) {
                TrangThaiYeuCau previous = request.cancelBecauseRouteCancelledByDriver(
                        cancelledAt, normalizedReason);
                if (previous == TrangThaiYeuCau.PENDING) {
                    pendingCancelled++;
                } else if (previous == TrangThaiYeuCau.ACCEPTED) {
                    route.releaseOneSeat();
                    acceptedCancelled++;
                    seatsRestored++;
                } else {
                    throw new IllegalStateException("Repository trả về request không còn active.");
                }

                long requestSequence = repository.nextRequestAuditSequence(request.getId());
                repository.appendRequestStateLog(NhatKyTrangThaiYeuCau.routeCancelledByDriver(
                        request,
                        route.getTaiXe(),
                        cancelledAt,
                        previous,
                        requestSequence));
                repository.persistNotification(ThongBao.routeCancelledByDriver(request));
                passengersNotified++;
            }

            TrangThaiLoTrinh previousRouteStatus = route.getTrangThaiLoTrinh();
            route.cancelByDriver(cancelledAt, normalizedReason);
            long routeSequence = repository.nextRouteAuditSequence(route.getId());
            repository.appendRouteStateLog(NhatKyTrangThaiLoTrinh.driverCancelled(
                    route, route.getTaiXe(), cancelledAt, routeSequence));
            repository.flush();

            SharedRouteCancellationResult result = new SharedRouteCancellationResult(
                    route.getId(),
                    previousRouteStatus,
                    route.getTrangThaiLoTrinh(),
                    cancelledAt,
                    route.getLyDoHuy(),
                    pendingCancelled,
                    acceptedCancelled,
                    seatsRestored,
                    passengersNotified);
            return mapper.toResponse(result);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "SHARED_ROUTE_CANCELLATION_INVARIANT_VIOLATION",
                    exception.getMessage());
        }
    }

    private static void validateInput(Long driverId, Long routeId, String reason) {
        AuthenticatedPrincipalValidator.requireUserId(driverId);
        if (routeId == null || routeId <= 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SHARED_ROUTE_CANCELLATION_PATH",
                    "routeId phải là số dương.");
        }
        if (reason == null || reason.isBlank() || reason.trim().length() > 2000) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CANCELLATION_REASON",
                    "Lý do hủy phải có từ 1 đến 2000 ký tự.");
        }
    }

    private static void requireOpenRoute(LoTrinhChiaSe route) {
        if (route.getTrangThaiLoTrinh() != TrangThaiLoTrinh.OPEN) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_SHARED_ROUTE_STATE",
                    "Chỉ lộ trình OPEN mới có thể được hủy.");
        }
    }

    private static BusinessException routeNotFound() {
        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "SHARED_ROUTE_NOT_FOUND",
                "Không tìm thấy lộ trình chia sẻ.");
    }
}
