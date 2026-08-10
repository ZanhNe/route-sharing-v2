package com.zanh.route_sharing.service.riderequest;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.riderequest.RideRequestPointSnapshot;
import com.zanh.route_sharing.domain.riderequest.RideRequestSnapshot;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPreparation;
import com.zanh.route_sharing.service.riderequest.model.PickupDeviation;
import com.zanh.route_sharing.service.routing.RoutePlanSegmentExtractor;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanLeg;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Component
public class RideRequestSnapshotCalculator {

    private final RoutePlanSegmentExtractor segmentExtractor;
    private final GeometryFactory geometryFactory;

    public RideRequestSnapshotCalculator(
            RoutePlanSegmentExtractor segmentExtractor,
            GeometryFactory geometryFactory) {
        this.segmentExtractor = segmentExtractor;
        this.geometryFactory = geometryFactory;
    }

    public RideRequestSnapshot calculate(
            RideRequestPreparation preparation,
            RouteEndpointRequest pickup,
            RouteEndpointRequest passengerDestination,
            RoutePlanRequest passengerPlanRequest,
            RoutePlan passengerPlan,
            PickupDeviation pickupDeviation,
            String proposedDropoffAddress,
            BigDecimal proposedSupportAmount) {
        Objects.requireNonNull(preparation, "preparation không được trống");
        Objects.requireNonNull(pickup, "pickup không được trống");
        Objects.requireNonNull(passengerDestination, "passengerDestination không được trống");
        Objects.requireNonNull(passengerPlanRequest, "passengerPlanRequest không được trống");
        Objects.requireNonNull(passengerPlan, "passengerPlan không được trống");
        Objects.requireNonNull(pickupDeviation, "pickupDeviation không được trống");
        Objects.requireNonNull(proposedSupportAmount, "proposedSupportAmount không được trống");

        RoutePlanLeg servedLeg = leg(
                passengerPlan,
                RouteWaypointRole.PASSENGER_PICKUP,
                RouteWaypointRole.PROPOSED_DROPOFF);
        RoutePlanLeg remainingLeg = leg(
                passengerPlan,
                RouteWaypointRole.PROPOSED_DROPOFF,
                RouteWaypointRole.PASSENGER_DESTINATION);

        BigDecimal total = passengerPlan.distanceMeters();
        BigDecimal served = servedLeg.distanceMeters();
        BigDecimal remaining = remainingLeg.distanceMeters();
        if (total.signum() <= 0 || served.add(remaining).compareTo(total) != 0) {
            throw notComputable("Tổng khoảng cách tuyến hành khách không khớp các chặng.");
        }

        BigDecimal ratio = served.multiply(new BigDecimal("100"))
                .divide(total, 2, RoundingMode.HALF_UP);
        if (ratio.compareTo(preparation.policy().minimumConvenienceRatioPercent()) < 0) {
            throw noLongerMatches("Tỷ lệ tiện đường không còn đạt cấu hình của nhà trường.");
        }
        if (pickupDeviation.distanceMeters()
                .compareTo(preparation.policy().maxPickupDeviationMeters()) > 0
                || pickupDeviation.durationSeconds() > preparation.policy().maxPickupDeviationSeconds()) {
            throw noLongerMatches("Độ lệch để đón hành khách vượt giới hạn của nhà trường.");
        }
        if (preparation.matchType() == LoaiGhepTuyen.CUNG_DIEM_DEN
                && remaining.signum() != 0) {
            throw notComputable("Ghép cùng điểm đến phải có chặng cuối co lại bằng 0.");
        }
        if (preparation.matchType() == LoaiGhepTuyen.TRUNG_DOAN_TUYEN
                && remaining.signum() <= 0) {
            throw notComputable("Ghép trùng đoạn tuyến phải còn chặng từ điểm thả tới đích hành khách.");
        }

        LineString fullRoute = copy(passengerPlan.geometry());
        LineString servedSegment;
        try {
            servedSegment = segmentExtractor.extract(
                    passengerPlanRequest,
                    passengerPlan,
                    RouteWaypointRole.PASSENGER_PICKUP,
                    RouteWaypointRole.PROPOSED_DROPOFF);
        } catch (IllegalArgumentException exception) {
            throw notComputable("Không thể xác định đoạn tuyến được phục vụ.");
        }

        BigDecimal servedRounded = scaleMetric(served);
        BigDecimal remainingRounded = scaleMetric(remaining);
        BigDecimal totalRounded = servedRounded.add(remainingRounded);

        return new RideRequestSnapshot(
                preparation.routeVersion(),
                preparation.driverId(),
                preparation.expectedDepartureTime(),
                preparation.matchType(),
                preparation.dropoffType(),
                point(pickup.latitude(), pickup.longitude(), pickup.address()),
                point(
                        passengerDestination.latitude(),
                        passengerDestination.longitude(),
                        passengerDestination.address()),
                point(
                        preparation.proposedDropoff().latitude(),
                        preparation.proposedDropoff().longitude(),
                        proposedDropoffAddress),
                fullRoute,
                servedSegment,
                scaleMetric(pickupDeviation.distanceMeters()),
                pickupDeviation.durationSeconds(),
                totalRounded,
                servedRounded,
                remainingRounded,
                ratio,
                preparation.suggestedSupportPerKm(),
                proposedSupportAmount,
                null,
                preparation.policy());
    }

    private RideRequestPointSnapshot point(
            BigDecimal latitude,
            BigDecimal longitude,
            String address) {
        Point point = geometryFactory.createPoint(
                new Coordinate(longitude.doubleValue(), latitude.doubleValue()));
        point.setSRID(Wgs84Coordinates.SRID);
        return new RideRequestPointSnapshot(point, address);
    }

    private static RoutePlanLeg leg(
            RoutePlan plan,
            RouteWaypointRole from,
            RouteWaypointRole to) {
        java.util.List<RoutePlanLeg> matches = plan.legs().stream()
                .filter(candidate -> candidate.fromRole() == from && candidate.toRole() == to)
                .limit(2)
                .toList();
        if (matches.size() != 1) {
            throw notComputable(matches.isEmpty()
                    ? "Dịch vụ bản đồ không trả đủ chặng nghiệp vụ."
                    : "Chặng nghiệp vụ bị mơ hồ do role xuất hiện nhiều lần.");
        }
        return matches.get(0);
    }

    private static LineString copy(LineString source) {
        LineString result = (LineString) source.copy();
        result.setSRID(Wgs84Coordinates.SRID);
        return result;
    }

    private static BigDecimal scaleMetric(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BusinessException noLongerMatches(String message) {
        return new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "SHARED_ROUTE_NO_LONGER_MATCHES",
                message);
    }

    private static BusinessException notComputable(String message) {
        return new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "RIDE_REQUEST_ROUTE_NOT_COMPUTABLE",
                message);
    }
}
