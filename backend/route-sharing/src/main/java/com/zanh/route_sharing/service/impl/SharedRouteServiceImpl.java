package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiXe;
import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointResponse;
import com.zanh.route_sharing.dto.sharedroute.SharedRouteResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.integration.goong.RouteCalculation;
import com.zanh.route_sharing.integration.goong.RouteCoordinate;
import com.zanh.route_sharing.repository.HoSoTaiXeRepository;
import com.zanh.route_sharing.repository.LoTrinhChiaSeRepository;
import com.zanh.route_sharing.repository.NguoiDungRepository;
import com.zanh.route_sharing.repository.PhuongTienRepository;
import com.zanh.route_sharing.service.GoongRouteService;
import com.zanh.route_sharing.service.SharedRouteService;
import com.zanh.route_sharing.utils.SpatialValidator;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SharedRouteServiceImpl implements SharedRouteService {
        private static final int WGS84_SRID = 4326;

        private final NguoiDungRepository nguoiDungRepository;
        private final HoSoTaiXeRepository hoSoTaiXeRepository;
        private final PhuongTienRepository phuongTienRepository;
        private final LoTrinhChiaSeRepository loTrinhChiaSeRepository;
        private final GoongRouteService goongRouteService;
        private final GeometryFactory geometryFactory;
        private final Clock clock;
        private final EntityManager entityManager;

        @Transactional
        public SharedRouteResponse createSharedRoute(
                        Long actorUserId,
                        CreateSharedRouteRequest request) {
                Objects.requireNonNull(actorUserId, "actorUserId không được trống");
                Objects.requireNonNull(request, "request không được trống");

                NguoiDung actor = findActor(actorUserId);
                HoSoTaiXe driverProfile = findDriverProfile(actorUserId);
                PhuongTien vehicle = findVehicle(request.vehicleId());

                validateAccount(actor);
                validateDriverProfile(actor, driverProfile);
                validateVehicle(actor, vehicle);
                validateSeatCount(vehicle, request.offeredSeats());
                validateDepartureTime(request.expectedDepartureTime(), clock.instant());
                validateSupportAmount(request.suggestedSupportPerKm());
                validateDistinctEndpoints(request.origin(), request.driverDestination());

                RouteCoordinate originCoordinate = toRouteCoordinate(request.origin());
                RouteCoordinate destinationCoordinate = toRouteCoordinate(request.driverDestination());

                RouteCalculation calculation = goongRouteService.calculate(
                                originCoordinate,
                                destinationCoordinate,
                                vehicle.getDongXe().getLoaiPhuongTien());

                refreshAndRevalidate(actor, driverProfile, vehicle, request);

                Point originPoint = createPoint(originCoordinate, "điểm xuất phát");
                Point destinationPoint = createPoint(destinationCoordinate, "điểm đích");
                LineString routeLine = createLineString(calculation.path());

                LoTrinhChiaSe sharedRoute = LoTrinhChiaSe.open(
                                actor,
                                vehicle,
                                originPoint,
                                request.origin().address(),
                                destinationPoint,
                                request.driverDestination().address(),
                                routeLine,
                                calculation.distanceMeters(),
                                calculation.durationSeconds(),
                                request.expectedDepartureTime(),
                                request.offeredSeats(),
                                request.suggestedSupportPerKm());

                LoTrinhChiaSe saved = loTrinhChiaSeRepository.saveAndFlush(sharedRoute);
                return toResponse(saved);
        }

        private void refreshAndRevalidate(
                        NguoiDung actor,
                        HoSoTaiXe driverProfile,
                        PhuongTien vehicle,
                        CreateSharedRouteRequest request) {
                entityManager.refresh(actor);
                entityManager.refresh(driverProfile);
                entityManager.refresh(vehicle);

                validateAccount(actor);
                validateDriverProfile(actor, driverProfile);
                validateVehicle(actor, vehicle);
                validateSeatCount(vehicle, request.offeredSeats());
                validateDepartureTime(request.expectedDepartureTime(), clock.instant());
        }

        private NguoiDung findActor(Long actorUserId) {
                return nguoiDungRepository.findById(actorUserId)
                                .orElseThrow(() -> error(
                                                HttpStatus.NOT_FOUND,
                                                "ACCOUNT_NOT_FOUND",
                                                "Không tìm thấy tài khoản hiện tại."));
        }

        private HoSoTaiXe findDriverProfile(Long actorUserId) {
                return hoSoTaiXeRepository.findByUserIdForRouteCreation(actorUserId)
                                .orElseThrow(() -> error(
                                                HttpStatus.CONFLICT,
                                                "DRIVER_PROFILE_NOT_FOUND",
                                                "Bạn chưa có hồ sơ tài xế."));
        }

        private PhuongTien findVehicle(Long vehicleId) {
                return phuongTienRepository.findByIdForRouteCreation(vehicleId)
                                .orElseThrow(() -> error(
                                                HttpStatus.NOT_FOUND,
                                                "VEHICLE_NOT_FOUND",
                                                "Không tìm thấy phương tiện."));
        }

        private static void validateAccount(NguoiDung actor) {
                if (actor.getTrangThaiTaiKhoan() != TrangThaiTaiKhoan.ACTIVE) {
                        throw error(
                                        HttpStatus.FORBIDDEN,
                                        "ACCOUNT_NOT_ACTIVE",
                                        "Tài khoản chưa ở trạng thái hoạt động.");
                }
        }

        private static void validateDriverProfile(
                        NguoiDung actor,
                        HoSoTaiXe driverProfile) {
                if (driverProfile.getNguoiDung() == null
                                || !Objects.equals(driverProfile.getNguoiDung().getId(), actor.getId())) {
                        throw error(
                                        HttpStatus.CONFLICT,
                                        "DRIVER_PROFILE_MISMATCH",
                                        "Hồ sơ tài xế không thuộc tài khoản hiện tại.");
                }

                if (driverProfile.getTrangThaiTaiXe() != TrangThaiTaiXe.ACTIVE) {
                        throw error(
                                        HttpStatus.CONFLICT,
                                        "DRIVER_NOT_ACTIVE",
                                        "Hồ sơ tài xế chưa ở trạng thái hoạt động.");
                }
        }

        private static void validateVehicle(
                        NguoiDung actor,
                        PhuongTien vehicle) {
                if (vehicle.getTrangThaiPhuongTien() != TrangThaiPhuongTien.ACTIVE) {
                        throw error(
                                        HttpStatus.CONFLICT,
                                        "VEHICLE_NOT_ACTIVE",
                                        "Phương tiện chưa ở trạng thái hoạt động.");
                }

                if (vehicle.getNguoiDangKySuDung() == null
                                || !Objects.equals(vehicle.getNguoiDangKySuDung().getId(), actor.getId())) {
                        throw error(
                                        HttpStatus.FORBIDDEN,
                                        "VEHICLE_NOT_AUTHORIZED",
                                        "Bạn không có quyền sử dụng phương tiện này để đăng lộ trình.");
                }

                if (vehicle.getDongXe() == null
                                || vehicle.getDongXe().getLoaiPhuongTien() == null) {
                        throw error(
                                        HttpStatus.CONFLICT,
                                        "VEHICLE_TYPE_NOT_CONFIGURED",
                                        "Phương tiện chưa được cấu hình loại phương tiện.");
                }

                if (vehicle.getSoChoHanhKhachDuocDuyet() == null
                                || vehicle.getSoChoHanhKhachDuocDuyet() < 1) {
                        throw error(
                                        HttpStatus.CONFLICT,
                                        "VEHICLE_CAPACITY_NOT_CONFIGURED",
                                        "Số chỗ hành khách của phương tiện chưa hợp lệ.");
                }
        }

        private static void validateSeatCount(
                        PhuongTien vehicle,
                        Integer offeredSeats) {
                if (offeredSeats == null || offeredSeats < 1) {
                        throw error(
                                        HttpStatus.BAD_REQUEST,
                                        "INVALID_SEAT_COUNT",
                                        "Số ghế cung cấp phải lớn hơn hoặc bằng 1.");
                }

                if (offeredSeats > vehicle.getSoChoHanhKhachDuocDuyet()) {
                        throw error(
                                        HttpStatus.CONFLICT,
                                        "SEAT_COUNT_EXCEEDS_VEHICLE_CAPACITY",
                                        "Số ghế cung cấp vượt quá số chỗ hành khách đã được duyệt của phương tiện.");
                }
        }

        private static void validateDepartureTime(
                        Instant expectedDepartureTime,
                        Instant now) {
                if (expectedDepartureTime == null || !expectedDepartureTime.isAfter(now)) {
                        throw error(
                                        HttpStatus.BAD_REQUEST,
                                        "DEPARTURE_TIME_MUST_BE_FUTURE",
                                        "Thời gian khởi hành dự kiến phải nằm trong tương lai.");
                }
        }

        private static void validateSupportAmount(BigDecimal amount) {
                if (amount != null && amount.signum() < 0) {
                        throw error(
                                        HttpStatus.BAD_REQUEST,
                                        "INVALID_SUPPORT_AMOUNT",
                                        "Mức hỗ trợ gợi ý không được âm.");
                }
        }

        private static void validateDistinctEndpoints(
                        RouteEndpointRequest origin,
                        RouteEndpointRequest destination) {
                boolean sameLatitude = origin.latitude().compareTo(destination.latitude()) == 0;
                boolean sameLongitude = origin.longitude().compareTo(destination.longitude()) == 0;

                if (sameLatitude && sameLongitude) {
                        throw error(
                                        HttpStatus.BAD_REQUEST,
                                        "ROUTE_ENDPOINTS_MUST_BE_DIFFERENT",
                                        "Điểm xuất phát và điểm đích phải khác nhau.");
                }
        }

        private static RouteCoordinate toRouteCoordinate(RouteEndpointRequest endpoint) {
                return new RouteCoordinate(endpoint.latitude(), endpoint.longitude());
        }

        private Point createPoint(RouteCoordinate coordinate, String name) {
                Point point = geometryFactory.createPoint(new Coordinate(
                                coordinate.longitude().doubleValue(),
                                coordinate.latitude().doubleValue()));
                point.setSRID(WGS84_SRID);
                SpatialValidator.validateWgs84Point(point, name);
                return point;
        }

        private LineString createLineString(List<RouteCoordinate> path) {
                if (path == null || path.size() < 2) {
                        throw error(
                                        HttpStatus.BAD_GATEWAY,
                                        "MAP_PROVIDER_INVALID_RESPONSE",
                                        "Dịch vụ bản đồ trả về tuyến đường không hợp lệ.");
                }

                Coordinate[] coordinates = path.stream()
                                .map(item -> new Coordinate(
                                                item.longitude().doubleValue(),
                                                item.latitude().doubleValue()))
                                .toArray(Coordinate[]::new);

                LineString lineString = geometryFactory.createLineString(coordinates);
                lineString.setSRID(WGS84_SRID);
                try {
                        SpatialValidator.validateWgs84LineString(lineString, "tuyến đường gốc");
                } catch (BusinessException exception) {
                        throw error(
                                        HttpStatus.BAD_GATEWAY,
                                        "MAP_PROVIDER_INVALID_RESPONSE",
                                        "Dịch vụ bản đồ trả về geometry tuyến đường không hợp lệ.");
                }
                return lineString;
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

        private static BusinessException error(
                        HttpStatus status,
                        String code,
                        String message) {
                return new BusinessException(status, code, message);
        }
}
