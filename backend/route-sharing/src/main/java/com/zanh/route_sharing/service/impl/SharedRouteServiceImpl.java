package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointResponse;
import com.zanh.route_sharing.dto.sharedroute.SharedRouteResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.HoSoTaiXeRepository;
import com.zanh.route_sharing.repository.NguoiDungRepository;
import com.zanh.route_sharing.repository.PhuongTienRepository;
import com.zanh.route_sharing.service.SharedRouteService;
import com.zanh.route_sharing.service.routing.RoutePlanner;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.sharedroute.SharedRoutePublicationCommitter;
import com.zanh.route_sharing.service.sharedroute.SharedRoutePublicationPolicy;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;

@Service
public class SharedRouteServiceImpl implements SharedRouteService {

    private final NguoiDungRepository nguoiDungRepository;
    private final HoSoTaiXeRepository hoSoTaiXeRepository;
    private final PhuongTienRepository phuongTienRepository;
    private final RoutePlanner routePlanner;
    private final SharedRoutePublicationCommitter publicationCommitter;
    private final SharedRoutePublicationPolicy publicationPolicy;
    private final Clock clock;

    public SharedRouteServiceImpl(
            NguoiDungRepository nguoiDungRepository,
            HoSoTaiXeRepository hoSoTaiXeRepository,
            PhuongTienRepository phuongTienRepository,
            RoutePlanner routePlanner,
            SharedRoutePublicationCommitter publicationCommitter,
            SharedRoutePublicationPolicy publicationPolicy,
            Clock clock) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.hoSoTaiXeRepository = hoSoTaiXeRepository;
        this.phuongTienRepository = phuongTienRepository;
        this.routePlanner = routePlanner;
        this.publicationCommitter = publicationCommitter;
        this.publicationPolicy = publicationPolicy;
        this.clock = clock;
    }

    @Override
    public SharedRouteResponse createSharedRoute(Long actorUserId, CreateSharedRouteRequest request) {
        Objects.requireNonNull(actorUserId, "actorUserId không được trống");
        Objects.requireNonNull(request, "request không được trống");

        NguoiDung actor = findActor(actorUserId);
        HoSoTaiXe driverProfile = findDriverProfile(actorUserId);
        PhuongTien vehicle = findVehicle(request.vehicleId());

        publicationPolicy.validate(actor, driverProfile, vehicle, request, TimePolicy.now(clock));

        RoutePlan routePlan = routePlanner.plan(RoutePlanRequest.direct(
                coordinate(request.origin().latitude(), request.origin().longitude()),
                coordinate(request.driverDestination().latitude(), request.driverDestination().longitude()),
                vehicle.getDongXe().getLoaiPhuongTien()));

        LoTrinhChiaSe saved = publicationCommitter.commit(actorUserId, request, routePlan);
        return toResponse(saved);
    }

    private NguoiDung findActor(Long actorUserId) {
        return nguoiDungRepository.findById(actorUserId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "Không tìm thấy tài khoản hiện tại."));
    }

    private HoSoTaiXe findDriverProfile(Long actorUserId) {
        return hoSoTaiXeRepository.findByUserIdForRouteCreation(actorUserId)
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "DRIVER_PROFILE_NOT_FOUND",
                        "Bạn chưa có hồ sơ tài xế."));
    }

    private PhuongTien findVehicle(Long vehicleId) {
        return phuongTienRepository.findByIdForRouteCreation(vehicleId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND",
                        "Không tìm thấy phương tiện."));
    }

    private static GeoCoordinate coordinate(BigDecimal latitude, BigDecimal longitude) {
        return new GeoCoordinate(latitude, longitude);
    }

    private static SharedRouteResponse toResponse(LoTrinhChiaSe entity) {
        return new SharedRouteResponse(
                entity.getId(),
                entity.getTrangThaiLoTrinh(),
                entity.getThoiGianKhoiHanhDuKien(),
                entity.getSoGheCungCap(),
                entity.getSoGheConLai(),
                entity.getKhoangCachDuKienMet(),
                entity.getThoiLuongDuKienGiay(),
                entity.getMucHoTroGoiYMoiKm(),
                new RouteEndpointResponse(
                        BigDecimal.valueOf(entity.getDiemXuatPhat().getY()),
                        BigDecimal.valueOf(entity.getDiemXuatPhat().getX()),
                        entity.getDiaChiXuatPhat()),
                new RouteEndpointResponse(
                        BigDecimal.valueOf(entity.getDiemDichTaiXe().getY()),
                        BigDecimal.valueOf(entity.getDiemDichTaiXe().getX()),
                        entity.getDiaChiDichTaiXe()),
                entity.getTaiXe().getId(),
                entity.getPhuongTien().getId(),
                entity.getCreatedAt());
    }

    private static BusinessException error(HttpStatus status, String code, String message) {
        return new BusinessException(status, code, message);
    }
}
