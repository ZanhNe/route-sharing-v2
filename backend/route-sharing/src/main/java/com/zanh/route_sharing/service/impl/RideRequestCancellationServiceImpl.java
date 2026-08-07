package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.dto.riderequest.cancellation.RideRequestCancellationResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.cancellation.RideRequestCancellationRepository;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.RideRequestCancellationService;
import com.zanh.route_sharing.service.riderequest.cancellation.RideRequestCancellationResponseMapper;
import com.zanh.route_sharing.service.riderequest.cancellation.model.RideRequestCancellationResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class RideRequestCancellationServiceImpl implements RideRequestCancellationService {
    private final RideRequestCancellationRepository repository;
    private final Clock clock;
    private final RideRequestCancellationResponseMapper responseMapper;

    public RideRequestCancellationServiceImpl(
            RideRequestCancellationRepository repository,
            Clock clock,
            RideRequestCancellationResponseMapper responseMapper) {
        this.repository = repository;
        this.clock = clock;
        this.responseMapper = responseMapper;
    }

    @Override
    @Transactional
    public RideRequestCancellationResponse cancelByPassenger(Long actorId, Long rideRequestId, String reason) {
        validatePassengerInput(actorId, rideRequestId, reason);
        Long routeId = repository.findPassengerRequestRouteId(actorId, rideRequestId)
                .orElseThrow(RideRequestCancellationServiceImpl::requestNotFound);
        LoTrinhChiaSe route = repository.lockRoute(routeId)
                .orElseThrow(RideRequestCancellationServiceImpl::requestNotFound);
        YeuCauDiChung request = repository.lockPassengerRequest(actorId, routeId, rideRequestId)
                .orElseThrow(RideRequestCancellationServiceImpl::requestNotFound);
        Instant cancelledAt = clock.instant();
        requireRouteOpen(route);
        requireNotAssignedToTrip(request);
        TrangThaiYeuCau previous = request.getTrangThaiYeuCau();
        if (previous != TrangThaiYeuCau.PENDING && previous != TrangThaiYeuCau.ACCEPTED) {
            throw invalidState("Hành khách chỉ có thể hủy yêu cầu PENDING hoặc ACCEPTED.");
        }
        request.cancelByPassenger(cancelledAt, reason);
        if (previous == TrangThaiYeuCau.ACCEPTED) {
            route.releaseOneSeat();
        }
        repository.appendStateLog(NhatKyTrangThaiYeuCau.cancelledByPassenger(
                request, request.getHanhKhach(), cancelledAt, previous));
        repository.persistNotification(ThongBao.bookingCancelledByPassenger(request));
        repository.flush();
        return responseMapper.toResponse(result(route, request, previous, cancelledAt));
    }

    private static void requireRouteOpen(LoTrinhChiaSe route) {
        if (route.getTrangThaiLoTrinh() != TrangThaiLoTrinh.OPEN) {
            throw new BusinessException(HttpStatus.CONFLICT, "SHARED_ROUTE_NOT_OPEN",
                    "Lộ trình không còn mở để hủy yêu cầu đi chung.");
        }
    }

    private static void requireNotAssignedToTrip(YeuCauDiChung request) {
        if (request.getChuyenDi() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "RIDE_REQUEST_ALREADY_ASSIGNED_TO_TRIP",
                    "Yêu cầu đã thuộc một chuyến đi thực tế.");
        }
    }

    private static RideRequestCancellationResult result(
            LoTrinhChiaSe route, YeuCauDiChung request,
            TrangThaiYeuCau previous, Instant cancelledAt) {
        return new RideRequestCancellationResult(
                route.getId(), request.getId(), previous, request.getTrangThaiYeuCau(),
                cancelledAt, route.getSoGheConLai(), request.getLyDoHuy());
    }

    private static void validatePassengerInput(Long actorId, Long rideRequestId, String reason) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (rideRequestId == null || rideRequestId <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_RIDE_REQUEST_CANCELLATION_PATH",
                    "rideRequestId phải là số dương.");
        }
        requireReason(reason);
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 2000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CANCELLATION_REASON",
                    "Lý do hủy phải có từ 1 đến 2000 ký tự.");
        }
    }


    private static BusinessException requestNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "RIDE_REQUEST_NOT_FOUND",
                "Không tìm thấy yêu cầu đi chung.");
    }

    private static BusinessException invalidState(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_RIDE_REQUEST_STATE", message);
    }
}
