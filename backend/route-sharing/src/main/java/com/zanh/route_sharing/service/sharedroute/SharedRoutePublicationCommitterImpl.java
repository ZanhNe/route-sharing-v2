package com.zanh.route_sharing.service.sharedroute;

import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.HoSoTaiXeRepository;
import com.zanh.route_sharing.repository.LoTrinhChiaSeRepository;
import com.zanh.route_sharing.repository.NguoiDungRepository;
import com.zanh.route_sharing.repository.PhuongTienRepository;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.utils.SpatialValidator;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;

@Service
public class SharedRoutePublicationCommitterImpl implements SharedRoutePublicationCommitter {

        private final NguoiDungRepository nguoiDungRepository;
        private final HoSoTaiXeRepository hoSoTaiXeRepository;
        private final PhuongTienRepository phuongTienRepository;
        private final LoTrinhChiaSeRepository loTrinhChiaSeRepository;
        private final SharedRoutePublicationPolicy publicationPolicy;
        private final GeometryFactory geometryFactory;
        private final Clock clock;

        public SharedRoutePublicationCommitterImpl(
                        NguoiDungRepository nguoiDungRepository,
                        HoSoTaiXeRepository hoSoTaiXeRepository,
                        PhuongTienRepository phuongTienRepository,
                        LoTrinhChiaSeRepository loTrinhChiaSeRepository,
                        SharedRoutePublicationPolicy publicationPolicy,
                        GeometryFactory geometryFactory,
                        Clock clock) {
                this.nguoiDungRepository = nguoiDungRepository;
                this.hoSoTaiXeRepository = hoSoTaiXeRepository;
                this.phuongTienRepository = phuongTienRepository;
                this.loTrinhChiaSeRepository = loTrinhChiaSeRepository;
                this.publicationPolicy = publicationPolicy;
                this.geometryFactory = geometryFactory;
                this.clock = clock;
        }

        @Override
        @Transactional
        public LoTrinhChiaSe commit(
                        Long actorUserId,
                        CreateSharedRouteRequest request,
                        RoutePlan routePlan) {
                Objects.requireNonNull(actorUserId, "actorUserId không được trống");
                Objects.requireNonNull(request, "request không được trống");
                Objects.requireNonNull(routePlan, "routePlan không được trống");

                NguoiDung actor = nguoiDungRepository.findById(actorUserId)
                                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                                                "Không tìm thấy tài khoản hiện tại."));
                HoSoTaiXe driverProfile = hoSoTaiXeRepository.findByUserIdForRouteCreation(actorUserId)
                                .orElseThrow(() -> error(HttpStatus.CONFLICT, "DRIVER_PROFILE_NOT_FOUND",
                                                "Bạn chưa có hồ sơ tài xế."));
                PhuongTien vehicle = phuongTienRepository.findByIdForRouteCreation(request.vehicleId())
                                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND",
                                                "Không tìm thấy phương tiện."));

                publicationPolicy.validate(actor, driverProfile, vehicle, request, TimePolicy.now(clock));

                Point originPoint = point(new GeoCoordinate(request.origin().latitude(), request.origin().longitude()),
                                "điểm xuất phát");
                Point destinationPoint = point(
                                new GeoCoordinate(request.driverDestination().latitude(),
                                                request.driverDestination().longitude()),
                                "điểm đích");
                LineString routeLine = copyRoute(routePlan.geometry());

                LoTrinhChiaSe sharedRoute = LoTrinhChiaSe.open(
                                actor,
                                vehicle,
                                originPoint,
                                request.origin().address(),
                                destinationPoint,
                                request.driverDestination().address(),
                                routeLine,
                                routePlan.distanceMeters(),
                                routePlan.durationSeconds(),
                                request.expectedDepartureTime(),
                                request.offeredSeats(),
                                request.suggestedSupportPerKm());

                return loTrinhChiaSeRepository.saveAndFlush(sharedRoute);
        }

        private Point point(GeoCoordinate coordinate, String name) {
                Point point = geometryFactory.createPoint(new Coordinate(
                                coordinate.longitude().doubleValue(),
                                coordinate.latitude().doubleValue()));
                point.setSRID(Wgs84Coordinates.SRID);
                SpatialValidator.validateWgs84Point(point, name);
                return point;
        }

        private static LineString copyRoute(LineString source) {
                LineString copy = (LineString) source.copy();
                copy.setSRID(Wgs84Coordinates.SRID);
                try {
                        SpatialValidator.validateWgs84LineString(copy, "tuyến đường gốc");
                } catch (BusinessException exception) {
                        throw error(HttpStatus.BAD_GATEWAY, "MAP_PROVIDER_INVALID_RESPONSE",
                                        "Dịch vụ bản đồ trả về geometry tuyến đường không hợp lệ.");
                }
                return copy;
        }

        private static BusinessException error(HttpStatus status, String code, String message) {
                return new BusinessException(status, code, message);
        }
}
