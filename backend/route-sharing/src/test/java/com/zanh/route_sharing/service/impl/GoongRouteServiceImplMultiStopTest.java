package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.config.properties.GoongProperties;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.integration.goong.GoongApiGateway;
import com.zanh.route_sharing.integration.goong.GoongDirectionsResponse;
import com.zanh.route_sharing.service.routing.RoutePlanValidator;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypoint;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoongRouteServiceImplMultiStopTest {

        private GoongApiGateway gateway;
        private GoongRouteServiceImpl sut;

        @BeforeEach
        void setUp() {
                gateway = mock(GoongApiGateway.class);
                GoongProperties properties = new GoongProperties();
                properties.setWaypointSnapToleranceMeters(new BigDecimal("20"));
                properties.setDuplicateWaypointToleranceMeters(new BigDecimal("2"));
                sut = new GoongRouteServiceImpl(
                                gateway,
                                properties,
                                new RoutePlanValidator(properties));
        }

        @SuppressWarnings("unchecked")
        @Test
        void givenFourSemanticWaypoints_whenPlanning_thenGoongReceivesOrderedMultiStopDestination() {
                List<GeoCoordinate> points = List.of(
                                coordinate("10.7700", "106.6800"),
                                coordinate("10.7701", "106.6900"),
                                coordinate("10.7700", "106.7050"),
                                coordinate("10.7700", "106.7200"));
                when(gateway.get(
                                eq("/v2/direction"),
                                any(),
                                eq(GoongDirectionsResponse.class)))
                                .thenReturn(response(points, List.of(
                                                leg(100L, 10L),
                                                leg(200L, 20L),
                                                leg(300L, 30L))));
                ArgumentCaptor<MultiValueMap<String, String>> queryCaptor = ArgumentCaptor
                                .forClass(MultiValueMap.class);

                var result = sut.plan(request(points));

                org.mockito.Mockito.verify(gateway).get(
                                eq("/v2/direction"),
                                queryCaptor.capture(),
                                eq(GoongDirectionsResponse.class));
                assertThat(queryCaptor.getValue().getFirst("origin"))
                                .isEqualTo("10.7700,106.6800");
                assertThat(queryCaptor.getValue().getFirst("destination"))
                                .isEqualTo("10.7701,106.6900;10.7700,106.7050;10.7700,106.7200");
                assertThat(queryCaptor.getValue().getFirst("alternatives")).isEqualTo("false");
                assertThat(result.distanceMeters()).isEqualByComparingTo("600");
                assertThat(result.durationSeconds()).isEqualTo(60L);
                assertThat(result.legs()).hasSize(3);
                assertThat(result.geometry().getSRID()).isEqualTo(4326);
        }

        @Test
        void givenDropoffAndDriverDestinationAreSame_whenPlanning_thenFinalSemanticLegIsCollapsed() {
                List<GeoCoordinate> points = List.of(
                                coordinate("10.7700", "106.6800"),
                                coordinate("10.7701", "106.6900"),
                                coordinate("10.7700", "106.7200"),
                                coordinate("10.7700", "106.7200"));
                when(gateway.get(
                                eq("/v2/direction"),
                                any(),
                                eq(GoongDirectionsResponse.class)))
                                .thenReturn(response(
                                                points.subList(0, 3),
                                                List.of(leg(100L, 10L), leg(500L, 50L))));

                var result = sut.plan(request(points));

                assertThat(result.legs()).hasSize(3);
                assertThat(result.legs().get(2).collapsed()).isTrue();
                assertThat(result.legs().get(2).distanceMeters()).isEqualByComparingTo("0");
                assertThat(result.legs().get(2).durationSeconds()).isZero();
                assertThat(result.distanceMeters()).isEqualByComparingTo("600");
        }

        @Test
        void givenPhysicalLegCountDoesNotMatchWaypoints_whenPlanning_thenProviderResponseIsRejected() {
                List<GeoCoordinate> points = List.of(
                                coordinate("10.7700", "106.6800"),
                                coordinate("10.7701", "106.6900"),
                                coordinate("10.7700", "106.7050"),
                                coordinate("10.7700", "106.7200"));
                when(gateway.get(
                                eq("/v2/direction"),
                                any(),
                                eq(GoongDirectionsResponse.class)))
                                .thenReturn(response(points, List.of(leg(600L, 60L))));

                assertThatThrownBy(() -> sut.plan(request(points)))
                                .isInstanceOf(BusinessException.class)
                                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                                                .isEqualTo("MAP_PROVIDER_INVALID_RESPONSE"));
        }

        @Test
        void givenProviderPolylineSkipsPickup_whenPlanning_thenWaypointValidationRejectsRoute() {
                List<GeoCoordinate> requested = List.of(
                                coordinate("10.7700", "106.6800"),
                                coordinate("10.8000", "106.6900"),
                                coordinate("10.7700", "106.7050"),
                                coordinate("10.7700", "106.7200"));
                List<GeoCoordinate> providerPath = List.of(
                                requested.get(0),
                                requested.get(2),
                                requested.get(3));
                when(gateway.get(
                                eq("/v2/direction"),
                                any(),
                                eq(GoongDirectionsResponse.class)))
                                .thenReturn(response(providerPath, List.of(
                                                leg(100L, 10L),
                                                leg(200L, 20L),
                                                leg(300L, 30L))));

                assertThatThrownBy(() -> sut.plan(request(requested)))
                                .isInstanceOf(BusinessException.class)
                                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                                                .isEqualTo("MAP_PROVIDER_INVALID_RESPONSE"));
        }

        private static RoutePlanRequest request(List<GeoCoordinate> points) {
                return new RoutePlanRequest(
                                List.of(
                                                new RouteWaypoint(RouteWaypointRole.DRIVER_ORIGIN, points.get(0)),
                                                new RouteWaypoint(RouteWaypointRole.PASSENGER_PICKUP, points.get(1)),
                                                new RouteWaypoint(RouteWaypointRole.PROPOSED_DROPOFF, points.get(2)),
                                                new RouteWaypoint(RouteWaypointRole.DRIVER_DESTINATION, points.get(3))),
                                LoaiPhuongTien.XE_MAY,
                                false);
        }

        private static GoongDirectionsResponse response(
                        List<GeoCoordinate> path,
                        List<GoongDirectionsResponse.LegDto> legs) {
                return new GoongDirectionsResponse(List.of(
                                new GoongDirectionsResponse.RouteDto(
                                                null,
                                                legs,
                                                new GoongDirectionsResponse.OverviewPolylineDto(encode(path)),
                                                List.of(),
                                                List.of())));
        }

        private static GoongDirectionsResponse.LegDto leg(long distance, long duration) {
                return new GoongDirectionsResponse.LegDto(
                                new GoongDirectionsResponse.ValueDto(distance),
                                new GoongDirectionsResponse.ValueDto(duration));
        }

        private static GeoCoordinate coordinate(String latitude, String longitude) {
                return new GeoCoordinate(new BigDecimal(latitude), new BigDecimal(longitude));
        }

        private static String encode(List<GeoCoordinate> coordinates) {
                StringBuilder encoded = new StringBuilder();
                long previousLatitude = 0L;
                long previousLongitude = 0L;
                for (GeoCoordinate coordinate : coordinates) {
                        long latitude = Math.round(coordinate.latitude().doubleValue() * 100_000d);
                        long longitude = Math.round(coordinate.longitude().doubleValue() * 100_000d);
                        encodeDelta(latitude - previousLatitude, encoded);
                        encodeDelta(longitude - previousLongitude, encoded);
                        previousLatitude = latitude;
                        previousLongitude = longitude;
                }
                return encoded.toString();
        }

        private static void encodeDelta(long delta, StringBuilder target) {
                long value = delta < 0 ? ~(delta << 1) : delta << 1;
                while (value >= 0x20) {
                        target.append((char) ((0x20 | (value & 0x1f)) + 63));
                        value >>= 5;
                }
                target.append((char) (value + 63));
        }
}
