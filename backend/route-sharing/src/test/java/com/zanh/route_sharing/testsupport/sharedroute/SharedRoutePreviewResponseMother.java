package com.zanh.route_sharing.testsupport.sharedroute;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.dto.sharedroute.preview.GeoJsonLineStringResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.OriginalRouteResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewDriverResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewPointResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewPointsResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewRouteLegResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewRouteResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewVehicleResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.RouteBoundsResponse;
import com.zanh.route_sharing.dto.sharedroute.preview.SharedRoutePreviewResponse;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class SharedRoutePreviewResponseMother {

        public static SharedRoutePreviewResponse validPreview() {
                GeoJsonLineStringResponse geometry = new GeoJsonLineStringResponse(
                                "LineString",
                                List.of(
                                                List.of(new BigDecimal("106.6800"), new BigDecimal("10.7700")),
                                                List.of(new BigDecimal("106.7200"), new BigDecimal("10.7700"))));
                PreviewPointResponse origin = point("10.7700", "106.6800", "Điểm đầu tài xế");
                PreviewPointResponse pickup = point("10.7701", "106.6900", "Điểm đón");
                PreviewPointResponse destination = point("10.7701", "106.7201", "Điểm đến");
                PreviewPointResponse driverDestination = point(
                                "10.7700",
                                "106.7200",
                                "Điểm cuối tài xế");

                return new SharedRoutePreviewResponse(
                                2L,
                                Instant.parse("2026-08-04T02:00:00Z"),
                                true,
                                TrangThaiLoTrinh.OPEN,
                                LoaiGhepTuyen.CUNG_DIEM_DEN,
                                LoaiDiemTha.DIEM_DICH_CUOI_CUNG,
                                new PreviewDriverResponse(3L, "Tài xế", null),
                                new PreviewVehicleResponse(
                                                4L,
                                                "59A1-TEST",
                                                "Đen",
                                                "Honda",
                                                "Wave",
                                                LoaiPhuongTien.XE_MAY),
                                Instant.parse("2026-08-04T04:00:00Z"),
                                2,
                                new BigDecimal("3000.00"),
                                new BigDecimal("12.50"),
                                new BigDecimal("20.00"),
                                new BigDecimal("4500.00"),
                                new PreviewPointsResponse(
                                                origin,
                                                pickup,
                                                destination,
                                                destination,
                                                driverDestination),
                                new OriginalRouteResponse(geometry, new BigDecimal("4500.00"), 900L),
                                new PreviewRouteResponse(
                                                geometry,
                                                new RouteBoundsResponse(
                                                                List.of(new BigDecimal("106.6800"),
                                                                                new BigDecimal("10.7700")),
                                                                List.of(new BigDecimal("106.7201"),
                                                                                new BigDecimal("10.7701"))),
                                                new BigDecimal("4700.00"),
                                                960L,
                                                List.of(
                                                                new PreviewRouteLegResponse(
                                                                                1,
                                                                                RouteWaypointRole.DRIVER_ORIGIN,
                                                                                RouteWaypointRole.PASSENGER_PICKUP,
                                                                                new BigDecimal("1200.00"),
                                                                                240L,
                                                                                false),
                                                                new PreviewRouteLegResponse(
                                                                                2,
                                                                                RouteWaypointRole.PASSENGER_PICKUP,
                                                                                RouteWaypointRole.PROPOSED_DROPOFF,
                                                                                new BigDecimal("3500.00"),
                                                                                720L,
                                                                                false),
                                                                new PreviewRouteLegResponse(
                                                                                3,
                                                                                RouteWaypointRole.PROPOSED_DROPOFF,
                                                                                RouteWaypointRole.DRIVER_DESTINATION,
                                                                                BigDecimal.ZERO,
                                                                                0L,
                                                                                true)),
                                                List.of()));
        }

        private static PreviewPointResponse point(
                        String latitude,
                        String longitude,
                        String address) {
                return new PreviewPointResponse(
                                new BigDecimal(latitude),
                                new BigDecimal(longitude),
                                address);
        }

        private SharedRoutePreviewResponseMother() {
        }
}
