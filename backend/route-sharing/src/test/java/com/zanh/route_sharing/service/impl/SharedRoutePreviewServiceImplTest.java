package com.zanh.route_sharing.service.impl;

import tools.jackson.databind.json.JsonMapper;
import com.zanh.route_sharing.config.properties.GoongProperties;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewPointRequest;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewSharedRouteRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.preview.SharedRoutePreviewRepository;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewConsistencyToken;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewDriverSnapshot;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewEvaluation;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewEvaluationStatus;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewGeoPoint;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewMatch;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewRouteSnapshot;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewVehicleSnapshot;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewCriteria;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewPreparation;
import com.zanh.route_sharing.service.routing.RoutePlanValidator;
import com.zanh.route_sharing.service.routing.RoutePlanner;
import com.zanh.route_sharing.service.routing.model.RouteBounds;
import com.zanh.route_sharing.service.routing.model.RoutePlan;
import com.zanh.route_sharing.service.routing.model.RoutePlanLeg;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;
import com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter;
import com.zanh.route_sharing.service.preview.PreviewResponseMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SharedRoutePreviewServiceImplTest {

        private static final Instant NOW = Instant.parse("2026-08-04T02:00:00Z");
        private static final Instant CHECKED_AT = NOW.plusMillis(1);
        private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

        @Test
        void givenEligibleSegmentCandidate_whenPreviewing_thenSelectedRouteIsPlannedAndMapped() {
                SharedRoutePreviewRepository repository = mock(SharedRoutePreviewRepository.class);
                RoutePlanner routePlanner = mock(RoutePlanner.class);
                PreviewSharedRouteRequest request = segmentRequest();
                SharedRoutePreviewPreparation preparation = preparation(
                                LoaiGhepTuyen.TRUNG_DOAN_TUYEN,
                                LoaiDiemTha.DIEM_THA_TRUNG_GIAN,
                                point("10.7700", "106.7050", null));
                RoutePlan plan = segmentPlan();
                when(repository.evaluate(any())).thenReturn(PreviewEvaluation.eligible(preparation));
                when(routePlanner.plan(any())).thenReturn(plan);
                when(repository.remainsCurrent(preparation.consistencyToken(), CHECKED_AT)).thenReturn(true);
                SharedRoutePreviewServiceImpl sut = service(repository, routePlanner);

                var response = sut.preview(7L, 2L, request);

                ArgumentCaptor<SharedRoutePreviewCriteria> criteriaCaptor = ArgumentCaptor
                                .forClass(SharedRoutePreviewCriteria.class);
                verify(repository).evaluate(criteriaCaptor.capture());
                assertThat(criteriaCaptor.getValue().actorUserId()).isEqualTo(7L);
                assertThat(criteriaCaptor.getValue().sharedRouteId()).isEqualTo(2L);
                assertThat(criteriaCaptor.getValue().schoolId()).isEqualTo(1L);
                assertThat(response.routeId()).isEqualTo(2L);
                assertThat(response.routeStatus()).isEqualTo(TrangThaiLoTrinh.OPEN);
                assertThat(response.matchType()).isEqualTo(LoaiGhepTuyen.TRUNG_DOAN_TUYEN);
                assertThat(response.dropoffType()).isEqualTo(LoaiDiemTha.DIEM_THA_TRUNG_GIAN);
                assertThat(response.points().proposedDropoff().address()).isNull();
                assertThat(response.previewRoute().legs()).hasSize(3);
                assertThat(response.previewRoute().geometry().type()).isEqualTo("LineString");
                assertThat(response.calculatedAt()).isEqualTo(CHECKED_AT);
                assertThat(response.canProceed()).isTrue();
        }

        @Test
        void givenSameDestinationCandidate_whenPreviewing_thenPassengerDestinationAddressIsKept() {
                SharedRoutePreviewRepository repository = mock(SharedRoutePreviewRepository.class);
                RoutePlanner routePlanner = mock(RoutePlanner.class);
                PreviewSharedRouteRequest request = sameDestinationRequest();
                SharedRoutePreviewPreparation preparation = preparation(
                                LoaiGhepTuyen.CUNG_DIEM_DEN,
                                LoaiDiemTha.DIEM_DICH_CUOI_CUNG,
                                point("10.7701", "106.7201", null));
                RoutePlan plan = sameDestinationPlan();
                when(repository.evaluate(any())).thenReturn(PreviewEvaluation.eligible(preparation));
                when(routePlanner.plan(any())).thenReturn(plan);
                when(repository.remainsCurrent(preparation.consistencyToken(), CHECKED_AT)).thenReturn(true);
                SharedRoutePreviewServiceImpl sut = service(repository, routePlanner);

                var response = sut.preview(7L, 2L, request);

                assertThat(response.points().proposedDropoff().address())
                                .isEqualTo("Điểm đến hành khách");
                assertThat(response.matchType()).isEqualTo(LoaiGhepTuyen.CUNG_DIEM_DEN);
        }

        @Test
        void givenEligibleCandidate_whenBuildingRouteRequest_thenSemanticOrderIsAPXD() {
                SharedRoutePreviewRepository repository = mock(SharedRoutePreviewRepository.class);
                RoutePlanner routePlanner = mock(RoutePlanner.class);
                SharedRoutePreviewPreparation preparation = preparation(
                                LoaiGhepTuyen.TRUNG_DOAN_TUYEN,
                                LoaiDiemTha.DIEM_THA_TRUNG_GIAN,
                                point("10.7700", "106.7050", null));
                when(repository.evaluate(any())).thenReturn(PreviewEvaluation.eligible(preparation));
                when(routePlanner.plan(any())).thenReturn(segmentPlan());
                when(repository.remainsCurrent(preparation.consistencyToken(), CHECKED_AT)).thenReturn(true);
                SharedRoutePreviewServiceImpl sut = service(repository, routePlanner);

                sut.preview(7L, 2L, segmentRequest());

                ArgumentCaptor<RoutePlanRequest> captor = ArgumentCaptor.forClass(RoutePlanRequest.class);
                verify(routePlanner).plan(captor.capture());
                assertThat(captor.getValue().waypoints())
                                .extracting(waypoint -> waypoint.role())
                                .containsExactly(
                                                RouteWaypointRole.DRIVER_ORIGIN,
                                                RouteWaypointRole.PASSENGER_PICKUP,
                                                RouteWaypointRole.PROPOSED_DROPOFF,
                                                RouteWaypointRole.DRIVER_DESTINATION);
                assertThat(captor.getValue().alternatives()).isFalse();
        }

        @ParameterizedTest
        @MethodSource("ineligibleStatuses")
        void givenIneligibleEvaluation_whenPreviewing_thenExpectedBusinessErrorIsReturned(
                        PreviewEvaluationStatus status,
                        String expectedCode) {
                SharedRoutePreviewRepository repository = mock(SharedRoutePreviewRepository.class);
                RoutePlanner routePlanner = mock(RoutePlanner.class);
                when(repository.evaluate(any())).thenReturn(PreviewEvaluation.ineligible(status));
                SharedRoutePreviewServiceImpl sut = service(repository, routePlanner);

                assertThatThrownBy(() -> sut.preview(7L, 2L, segmentRequest()))
                                .isInstanceOf(BusinessException.class)
                                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                                                .isEqualTo(expectedCode));
                verify(routePlanner, never()).plan(any());
        }

        @Test
        void givenRouteChangedDuringProviderCall_whenPreviewing_thenStaleErrorIsReturned() {
                SharedRoutePreviewRepository repository = mock(SharedRoutePreviewRepository.class);
                RoutePlanner routePlanner = mock(RoutePlanner.class);
                SharedRoutePreviewPreparation preparation = preparation(
                                LoaiGhepTuyen.TRUNG_DOAN_TUYEN,
                                LoaiDiemTha.DIEM_THA_TRUNG_GIAN,
                                point("10.7700", "106.7050", null));
                when(repository.evaluate(any())).thenReturn(PreviewEvaluation.eligible(preparation));
                when(routePlanner.plan(any())).thenReturn(segmentPlan());
                when(repository.remainsCurrent(preparation.consistencyToken(), CHECKED_AT)).thenReturn(false);
                SharedRoutePreviewServiceImpl sut = service(repository, routePlanner);

                assertThatThrownBy(() -> sut.preview(7L, 2L, segmentRequest()))
                                .isInstanceOf(BusinessException.class)
                                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                                                .isEqualTo("SHARED_ROUTE_PREVIEW_STALE"));
        }

        @Test
        void givenIdenticalPickupAndDestination_whenPreviewing_thenRequestIsRejectedBeforeRepositoryCall() {
                SharedRoutePreviewRepository repository = mock(SharedRoutePreviewRepository.class);
                RoutePlanner routePlanner = mock(RoutePlanner.class);
                PreviewPointRequest same = new PreviewPointRequest(
                                new BigDecimal("10.77"),
                                new BigDecimal("106.69"),
                                "Cùng điểm");
                PreviewSharedRouteRequest request = new PreviewSharedRouteRequest(1L, same, same);
                SharedRoutePreviewServiceImpl sut = service(repository, routePlanner);

                assertThatThrownBy(() -> sut.preview(7L, 2L, request))
                                .isInstanceOf(BusinessException.class)
                                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                                                .isEqualTo("INVALID_SHARED_ROUTE_PREVIEW_REQUEST"));
                verify(repository, never()).evaluate(any());
        }

        private static SharedRoutePreviewServiceImpl service(
                        SharedRoutePreviewRepository repository,
                        RoutePlanner routePlanner) {
                GoongProperties properties = new GoongProperties();
                RoutePlanValidator validator = new RoutePlanValidator(properties);
                Clock clock = new SequenceClock(NOW, CHECKED_AT);
                return new SharedRoutePreviewServiceImpl(
                                repository,
                                routePlanner,
                                validator,
                                new PreviewResponseMapper(
                                                new RouteGeoJsonWriter(JsonMapper.builder().build())),
                                clock);
        }

        private static SharedRoutePreviewPreparation preparation(
                        LoaiGhepTuyen matchType,
                        LoaiDiemTha dropoffType,
                        PreviewGeoPoint proposedDropoff) {
                PreviewRouteSnapshot route = new PreviewRouteSnapshot(
                                2L,
                                0L,
                                TrangThaiLoTrinh.OPEN,
                                point("10.7700", "106.6800", "Điểm đầu tài xế"),
                                point("10.7700", "106.7200", "Điểm cuối tài xế"),
                                "{\"type\":\"LineString\",\"coordinates\":[[106.68,10.77],[106.70,10.77],[106.72,10.77]]}",
                                new BigDecimal("4500.00"),
                                900L,
                                NOW.plusSeconds(7200),
                                2,
                                new BigDecimal("3000.00"));
                PreviewDriverSnapshot driver = new PreviewDriverSnapshot(3L, "Tài xế", null);
                PreviewVehicleSnapshot vehicle = new PreviewVehicleSnapshot(
                                4L,
                                "59A1-TEST",
                                "Đen",
                                "Honda",
                                "Wave",
                                LoaiPhuongTien.XE_MAY);
                PreviewMatch match = new PreviewMatch(
                                matchType,
                                dropoffType,
                                proposedDropoff,
                                new BigDecimal("12.50"),
                                new BigDecimal("20.00"),
                                new BigDecimal("1800.00"));
                PreviewConsistencyToken token = token(route, driver, vehicle);
                return new SharedRoutePreviewPreparation(route, driver, vehicle, match, token);
        }

        private static PreviewConsistencyToken token(
                        PreviewRouteSnapshot route,
                        PreviewDriverSnapshot driver,
                        PreviewVehicleSnapshot vehicle) {
                return new PreviewConsistencyToken(
                                route.routeId(), 1L, route.routeVersion(),
                                7L, 0L, 0L,
                                driver.id(), 0L, 0L,
                                30L, 0L,
                                vehicle.id(), 0L,
                                40L, 0L,
                                50L, 0L,
                                60L, 0L,
                                70L, 0L,
                                0L,
                                80L, 0L,
                                new BigDecimal("200.00"),
                                new BigDecimal("150.00"),
                                new BigDecimal("150.00"),
                                route.expectedDepartureTime(),
                                route.remainingSeats());
        }

        private static PreviewSharedRouteRequest segmentRequest() {
                return new PreviewSharedRouteRequest(
                                1L,
                                new PreviewPointRequest(
                                                new BigDecimal("10.7701"),
                                                new BigDecimal("106.6900"),
                                                "Điểm đón"),
                                new PreviewPointRequest(
                                                new BigDecimal("10.7701"),
                                                new BigDecimal("106.7050"),
                                                "Điểm đến hành khách"));
        }

        private static PreviewSharedRouteRequest sameDestinationRequest() {
                return new PreviewSharedRouteRequest(
                                1L,
                                new PreviewPointRequest(
                                                new BigDecimal("10.7701"),
                                                new BigDecimal("106.6900"),
                                                "Điểm đón"),
                                new PreviewPointRequest(
                                                new BigDecimal("10.7701"),
                                                new BigDecimal("106.7201"),
                                                "Điểm đến hành khách"));
        }

        private static RoutePlan segmentPlan() {
                return plan(new Coordinate[] {
                                new Coordinate(106.6800, 10.7700),
                                new Coordinate(106.6900, 10.7701),
                                new Coordinate(106.7050, 10.7700),
                                new Coordinate(106.7200, 10.7700)
                });
        }

        private static RoutePlan sameDestinationPlan() {
                return plan(new Coordinate[] {
                                new Coordinate(106.6800, 10.7700),
                                new Coordinate(106.6900, 10.7701),
                                new Coordinate(106.7201, 10.7701),
                                new Coordinate(106.7200, 10.7700)
                });
        }

        private static RoutePlan plan(Coordinate[] coordinates) {
                LineString geometry = GEOMETRY_FACTORY.createLineString(coordinates);
                geometry.setSRID(4326);
                return new RoutePlan(
                                geometry,
                                new BigDecimal("600"),
                                60L,
                                List.of(
                                                new RoutePlanLeg(1, RouteWaypointRole.DRIVER_ORIGIN,
                                                                RouteWaypointRole.PASSENGER_PICKUP,
                                                                new BigDecimal("100"), 10L, false),
                                                new RoutePlanLeg(2, RouteWaypointRole.PASSENGER_PICKUP,
                                                                RouteWaypointRole.PROPOSED_DROPOFF,
                                                                new BigDecimal("200"), 20L, false),
                                                new RoutePlanLeg(3, RouteWaypointRole.PROPOSED_DROPOFF,
                                                                RouteWaypointRole.DRIVER_DESTINATION,
                                                                new BigDecimal("300"), 30L, false)),
                                List.of(),
                                new RouteBounds(
                                                new BigDecimal("106.68"),
                                                new BigDecimal("10.77"),
                                                new BigDecimal("106.7201"),
                                                new BigDecimal("10.7701")));
        }

        private static PreviewGeoPoint point(String latitude, String longitude, String address) {
                return new PreviewGeoPoint(
                                new BigDecimal(latitude),
                                new BigDecimal(longitude),
                                address);
        }

        private static Stream<Arguments> ineligibleStatuses() {
                return Stream.of(
                                Arguments.of(PreviewEvaluationStatus.NOT_FOUND_OR_INACCESSIBLE,
                                                "SHARED_ROUTE_NOT_FOUND"),
                                Arguments.of(PreviewEvaluationStatus.ROUTE_UNAVAILABLE,
                                                "SHARED_ROUTE_UNAVAILABLE"),
                                Arguments.of(PreviewEvaluationStatus.SELF_ROUTE,
                                                "SHARED_ROUTE_UNAVAILABLE"),
                                Arguments.of(PreviewEvaluationStatus.DRIVER_OR_VEHICLE_INELIGIBLE,
                                                "SHARED_ROUTE_UNAVAILABLE"),
                                Arguments.of(PreviewEvaluationStatus.NO_LONGER_MATCHES,
                                                "SHARED_ROUTE_NO_LONGER_MATCHES"));
        }

        private static final class SequenceClock extends Clock {
                private final List<Instant> instants;
                private int index;

                private SequenceClock(Instant... instants) {
                        this.instants = List.of(instants);
                }

                @Override
                public java.time.ZoneId getZone() {
                        return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(java.time.ZoneId zone) {
                        return this;
                }

                @Override
                public Instant instant() {
                        int current = Math.min(index, instants.size() - 1);
                        index++;
                        return instants.get(current);
                }
        }
}
