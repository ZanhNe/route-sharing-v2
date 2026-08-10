package com.zanh.route_sharing.integration.goong;

import com.zanh.route_sharing.config.properties.GoongProperties;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.service.routing.RoutePlanValidator;
import com.zanh.route_sharing.service.routing.RoutePlanner;
import com.zanh.route_sharing.service.routing.RoutePlanningPolicy;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RouteBounds;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanLeg;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.utils.GeoDistanceUtils;
import com.zanh.route_sharing.utils.PolylineUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GoongRoutePlanner implements RoutePlanner {

    private static final Logger log = LoggerFactory.getLogger(GoongRoutePlanner.class);

    private final GoongApiGateway goongApiGateway;
    private final GoongProperties properties;
    private final RoutePlanningPolicy routingPolicy;
    private final RoutePlanValidator routePlanValidator;

    public GoongRoutePlanner(
            GoongApiGateway goongApiGateway,
            GoongProperties properties,
            RoutePlanningPolicy routingPolicy,
            RoutePlanValidator routePlanValidator) {
        this.goongApiGateway = Objects.requireNonNull(goongApiGateway);
        this.properties = Objects.requireNonNull(properties);
        this.routingPolicy = Objects.requireNonNull(routingPolicy);
        this.routePlanValidator = Objects.requireNonNull(routePlanValidator);
    }

    @Override
    public RoutePlan plan(RoutePlanRequest request) {
        if (request == null) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ROUTE_PLAN_REQUEST",
                    "Yêu cầu tính tuyến đường không hợp lệ.");
        }

        NormalizedWaypoints normalized = normalizeWaypoints(request.waypoints());
        if (normalized.physicalWaypoints().size() < 2) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ROUTE_WAYPOINTS",
                    "Tuyến đường phải có ít nhất hai điểm vật lý khác nhau.");
        }

        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("origin", toGoongParameter(normalized.physicalWaypoints().get(0)));
        query.add(
                "destination",
                normalized.physicalWaypoints().stream()
                        .skip(1)
                        .map(GoongRoutePlanner::toGoongParameter)
                        .collect(Collectors.joining(";")));
        query.add("vehicle", toGoongVehicle(request.vehicleType()));
        query.add("alternatives", Boolean.toString(request.alternatives()));

        GoongDirectionsResponse response = goongApiGateway.get(
                properties.getDirectionsPath(),
                query,
                GoongDirectionsResponse.class);

        GoongDirectionsResponse.RouteDto route = selectRoute(response);
        List<PhysicalLeg> physicalLegs = mapPhysicalLegs(
                route.legs(),
                normalized.physicalWaypoints().size() - 1);
        LineString geometry = decodeGeometry(route);
        List<RoutePlanLeg> semanticLegs = mapSemanticLegs(
                request.waypoints(),
                normalized.semanticToPhysical(),
                physicalLegs);

        BigDecimal distanceMeters = physicalLegs.stream()
                .map(PhysicalLeg::distanceMeters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long durationSeconds = sumDurations(physicalLegs);
        if (distanceMeters.signum() <= 0 || durationSeconds <= 0) {
            throw invalidProviderResponse("Khoảng cách hoặc thời lượng của tuyến không hợp lệ.");
        }

        RoutePlan plan = new RoutePlan(
                geometry,
                distanceMeters,
                durationSeconds,
                semanticLegs,
                route.warnings() == null ? List.of() : route.warnings(),
                boundsOf(geometry));
        routePlanValidator.validate(request, plan);

        if (response.routes().size() > 1 && !request.alternatives()) {
            log.debug("Goong returned {} routes although alternatives=false", response.routes().size());
        }
        return plan;
    }

    private NormalizedWaypoints normalizeWaypoints(List<RouteWaypoint> semanticWaypoints) {
        List<GeoCoordinate> physical = new ArrayList<>();
        int[] semanticToPhysical = new int[semanticWaypoints.size()];
        BigDecimal duplicateTolerance = routingPolicy.duplicateWaypointToleranceMeters();

        for (int index = 0; index < semanticWaypoints.size(); index++) {
            GeoCoordinate coordinate = semanticWaypoints.get(index).coordinate();
            if (physical.isEmpty()) {
                physical.add(coordinate);
                semanticToPhysical[index] = 0;
                continue;
            }

            GeoCoordinate previous = physical.get(physical.size() - 1);
            if (GeoDistanceUtils.distanceMeters(previous, coordinate)
                    .compareTo(duplicateTolerance) <= 0) {
                semanticToPhysical[index] = physical.size() - 1;
            } else {
                physical.add(coordinate);
                semanticToPhysical[index] = physical.size() - 1;
            }
        }

        return new NormalizedWaypoints(List.copyOf(physical), semanticToPhysical);
    }

    private static GoongDirectionsResponse.RouteDto selectRoute(
            GoongDirectionsResponse response) {
        if (response == null || response.routes() == null) {
            throw invalidProviderResponse("Dịch vụ bản đồ trả về dữ liệu không hợp lệ.");
        }
        if (response.routes().isEmpty()) {
            throw error(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "ROUTE_NOT_FOUND",
                    "Không tìm thấy tuyến đường phù hợp.");
        }
        GoongDirectionsResponse.RouteDto route = response.routes().get(0);
        if (route == null) {
            throw invalidProviderResponse("Dịch vụ bản đồ trả về tuyến đường không hợp lệ.");
        }
        return route;
    }

    private static List<PhysicalLeg> mapPhysicalLegs(
            List<GoongDirectionsResponse.LegDto> legs,
            int expectedLegCount) {
        if (legs == null || legs.isEmpty() || legs.size() != expectedLegCount) {
            throw invalidProviderResponse("Số chặng Goong trả về không khớp các điểm dừng.");
        }

        List<PhysicalLeg> mapped = new ArrayList<>(legs.size());
        for (GoongDirectionsResponse.LegDto leg : legs) {
            if (leg == null
                    || leg.distance() == null
                    || leg.distance().value() == null
                    || leg.duration() == null
                    || leg.duration().value() == null
                    || leg.distance().value() < 0
                    || leg.duration().value() < 0) {
                throw invalidProviderResponse("Dịch vụ bản đồ trả về dữ liệu chặng đường không hợp lệ.");
            }
            mapped.add(new PhysicalLeg(
                    BigDecimal.valueOf(leg.distance().value()),
                    leg.duration().value()));
        }
        return List.copyOf(mapped);
    }

    private static List<RoutePlanLeg> mapSemanticLegs(
            List<RouteWaypoint> semanticWaypoints,
            int[] semanticToPhysical,
            List<PhysicalLeg> physicalLegs) {
        List<RoutePlanLeg> result = new ArrayList<>(semanticWaypoints.size() - 1);

        for (int index = 0; index < semanticWaypoints.size() - 1; index++) {
            RouteWaypoint from = semanticWaypoints.get(index);
            RouteWaypoint to = semanticWaypoints.get(index + 1);
            int fromPhysical = semanticToPhysical[index];
            int toPhysical = semanticToPhysical[index + 1];

            if (fromPhysical == toPhysical) {
                result.add(new RoutePlanLeg(
                        index + 1,
                        from.role(),
                        to.role(),
                        BigDecimal.ZERO,
                        0L,
                        true));
                continue;
            }
            if (toPhysical != fromPhysical + 1 || fromPhysical >= physicalLegs.size()) {
                throw invalidProviderResponse("Không thể ánh xạ chặng vật lý sang chặng nghiệp vụ.");
            }

            PhysicalLeg physicalLeg = physicalLegs.get(fromPhysical);
            result.add(new RoutePlanLeg(
                    index + 1,
                    from.role(),
                    to.role(),
                    physicalLeg.distanceMeters(),
                    physicalLeg.durationSeconds(),
                    false));
        }
        return List.copyOf(result);
    }

    private static LineString decodeGeometry(
            GoongDirectionsResponse.RouteDto route) {
        String encodedPolyline = route.overviewPolyline() == null
                ? null
                : route.overviewPolyline().points();
        if (encodedPolyline == null || encodedPolyline.isBlank()) {
            throw invalidProviderResponse("Dịch vụ bản đồ không trả về đường biểu diễn tuyến.");
        }

        try {
            return PolylineUtils.decodeToLineString(encodedPolyline);
        } catch (RuntimeException exception) {
            throw invalidProviderResponse("Dịch vụ bản đồ trả về overview_polyline không hợp lệ.");
        }
    }

    private static long sumDurations(List<PhysicalLeg> legs) {
        long total = 0L;
        for (PhysicalLeg leg : legs) {
            try {
                total = Math.addExact(total, leg.durationSeconds());
            } catch (ArithmeticException exception) {
                throw invalidProviderResponse("Thời lượng tuyến vượt giới hạn xử lý.");
            }
        }
        return total;
    }

    private static RouteBounds boundsOf(LineString geometry) {
        Envelope envelope = geometry.getEnvelopeInternal();
        return new RouteBounds(
                BigDecimal.valueOf(envelope.getMinX()),
                BigDecimal.valueOf(envelope.getMinY()),
                BigDecimal.valueOf(envelope.getMaxX()),
                BigDecimal.valueOf(envelope.getMaxY()));
    }

    private static String toGoongParameter(GeoCoordinate coordinate) {
        return coordinate.latitude().toPlainString()
                + ","
                + coordinate.longitude().toPlainString();
    }

    private static String toGoongVehicle(LoaiPhuongTien vehicleType) {
        if (vehicleType == null) {
            throw error(
                    HttpStatus.CONFLICT,
                    "VEHICLE_TYPE_NOT_CONFIGURED",
                    "Phương tiện chưa được cấu hình loại phương tiện.");
        }
        return switch (vehicleType) {
            case XE_MAY -> "bike";
            case O_TO -> "car";
        };
    }

    private static BusinessException invalidProviderResponse(String message) {
        return error(HttpStatus.BAD_GATEWAY, "MAP_PROVIDER_INVALID_RESPONSE", message);
    }

    private static BusinessException error(
            HttpStatus status,
            String code,
            String message) {
        return new BusinessException(status, code, message);
    }

    private record NormalizedWaypoints(
            List<GeoCoordinate> physicalWaypoints,
            int[] semanticToPhysical) {
        private NormalizedWaypoints {
            physicalWaypoints = List.copyOf(physicalWaypoints);
            semanticToPhysical = semanticToPhysical.clone();
        }

        @Override
        public int[] semanticToPhysical() {
            return semanticToPhysical.clone();
        }
    }

    private record PhysicalLeg(
            BigDecimal distanceMeters,
            long durationSeconds) {
    }
}
