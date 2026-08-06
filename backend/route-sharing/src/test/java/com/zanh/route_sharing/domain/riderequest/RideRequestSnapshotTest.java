package com.zanh.route_sharing.domain.riderequest;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.testsupport.riderequest.RideRequestMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideRequestSnapshotTest {

        private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

        @Test
        void givenValidSegmentSnapshot_whenCreating_thenPendingSnapshotIsAccepted() {
                RideRequestSnapshot result = SnapshotValues.segment().create();

                assertThat(result.matchType()).isEqualTo(LoaiGhepTuyen.TRUNG_DOAN_TUYEN);
                assertThat(result.dropoffType()).isEqualTo(LoaiDiemTha.DIEM_THA_TRUNG_GIAN);
                assertThat(result.servedDistanceMeters().add(result.remainingDistanceMeters()))
                                .isEqualByComparingTo(result.passengerDesiredDistanceMeters());
                assertThat(result.agreedSupportAmount()).isNull();
        }

        @Test
        void givenValidSameDestinationSnapshot_whenCreating_thenCollapsedRemainingLegIsAccepted() {
                SnapshotValues values = SnapshotValues.segment();
                values.matchType = LoaiGhepTuyen.CUNG_DIEM_DEN;
                values.dropoffType = LoaiDiemTha.DIEM_DICH_CUOI_CUNG;
                values.total = new BigDecimal("3900.00");
                values.served = new BigDecimal("3900.00");
                values.remaining = BigDecimal.ZERO;
                values.ratio = new BigDecimal("100.00");

                RideRequestSnapshot result = values.create();

                assertThat(result.remainingDistanceMeters()).isZero();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidSnapshots")
        void givenBrokenPendingSnapshotInvariant_whenCreating_thenInvariantIsRejected(
                        String description,
                        Consumer<SnapshotValues> mutation) {
                SnapshotValues values = SnapshotValues.segment();
                mutation.accept(values);

                assertThatThrownBy(values::create)
                                .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
        }

        private static Stream<Arguments> invalidSnapshots() {
                return Stream.of(
                                Arguments.of("route version negative", mutation(v -> v.routeVersion = -1L)),
                                Arguments.of("driver id zero", mutation(v -> v.driverId = 0L)),
                                Arguments.of("pickup deviation negative",
                                                mutation(v -> v.pickupDeviationMeters = new BigDecimal("-0.01"))),
                                Arguments.of("pickup deviation seconds negative",
                                                mutation(v -> v.pickupDeviationSeconds = -1L)),
                                Arguments.of("total distance zero", mutation(v -> v.total = BigDecimal.ZERO)),
                                Arguments.of("served distance negative",
                                                mutation(v -> v.served = new BigDecimal("-0.01"))),
                                Arguments.of("remaining distance negative",
                                                mutation(v -> v.remaining = new BigDecimal("-0.01"))),
                                Arguments.of("ratio above one hundred",
                                                mutation(v -> v.ratio = new BigDecimal("100.01"))),
                                Arguments.of("proposed support negative",
                                                mutation(v -> v.proposedSupport = new BigDecimal("-0.01"))),
                                Arguments.of("agreed support present while pending",
                                                mutation(v -> v.agreedSupport = new BigDecimal("25000"))),
                                Arguments.of("leg total mismatch", mutation(v -> v.total = new BigDecimal("4200.01"))),
                                Arguments.of("same destination wrong dropoff", mutation(v -> {
                                        v.matchType = LoaiGhepTuyen.CUNG_DIEM_DEN;
                                        v.dropoffType = LoaiDiemTha.DIEM_THA_TRUNG_GIAN;
                                        v.total = new BigDecimal("3900");
                                        v.served = new BigDecimal("3900");
                                        v.remaining = BigDecimal.ZERO;
                                })),
                                Arguments.of("same destination has remaining leg", mutation(v -> {
                                        v.matchType = LoaiGhepTuyen.CUNG_DIEM_DEN;
                                        v.dropoffType = LoaiDiemTha.DIEM_DICH_CUOI_CUNG;
                                })),
                                Arguments.of("segment match wrong dropoff",
                                                mutation(v -> v.dropoffType = LoaiDiemTha.DIEM_DICH_CUOI_CUNG)),
                                Arguments.of("segment match no remaining leg", mutation(v -> {
                                        v.total = new BigDecimal("3900");
                                        v.served = new BigDecimal("3900");
                                        v.remaining = BigDecimal.ZERO;
                                })),
                                Arguments.of("desired route wrong SRID",
                                                mutation(v -> v.desiredRoute = line(0, 106.7, 10.7, 106.8, 10.8))),
                                Arguments.of("served route collapsed",
                                                mutation(v -> v.servedRoute = line(4326, 106.7, 10.7, 106.7, 10.7))));
        }

        private static Consumer<SnapshotValues> mutation(Consumer<SnapshotValues> mutation) {
                return mutation;
        }

        private static final class SnapshotValues {
                private Long routeVersion = 0L;
                private Long driverId = RideRequestMother.DRIVER_ID;
                private java.time.Instant departure = RideRequestMother.DEPARTURE;
                private LoaiGhepTuyen matchType = LoaiGhepTuyen.TRUNG_DOAN_TUYEN;
                private LoaiDiemTha dropoffType = LoaiDiemTha.DIEM_THA_TRUNG_GIAN;
                private RideRequestPointSnapshot pickup = point(106.700981, 10.776530, "Điểm đón");
                private RideRequestPointSnapshot destination = point(106.712450, 10.782120, "Điểm đến");
                private RideRequestPointSnapshot dropoff = point(106.711900, 10.781800, "Điểm thả");
                private LineString desiredRoute = line(4326, 106.700981, 10.776530,
                                106.711900, 10.781800, 106.712450, 10.782120);
                private LineString servedRoute = line(4326, 106.700981, 10.776530,
                                106.711900, 10.781800);
                private BigDecimal pickupDeviationMeters = new BigDecimal("100.00");
                private long pickupDeviationSeconds = 60L;
                private BigDecimal total = new BigDecimal("4200.00");
                private BigDecimal served = new BigDecimal("3900.00");
                private BigDecimal remaining = new BigDecimal("300.00");
                private BigDecimal ratio = new BigDecimal("92.86");
                private BigDecimal suggestedSupport = new BigDecimal("5000.00");
                private BigDecimal proposedSupport = new BigDecimal("25000.00");
                private BigDecimal agreedSupport;
                private RideRequestPolicySnapshot policy = RideRequestMother.policy();

                static SnapshotValues segment() {
                        return new SnapshotValues();
                }

                RideRequestSnapshot create() {
                        return new RideRequestSnapshot(
                                        routeVersion,
                                        driverId,
                                        departure,
                                        matchType,
                                        dropoffType,
                                        pickup,
                                        destination,
                                        dropoff,
                                        desiredRoute,
                                        servedRoute,
                                        pickupDeviationMeters,
                                        pickupDeviationSeconds,
                                        total,
                                        served,
                                        remaining,
                                        ratio,
                                        suggestedSupport,
                                        proposedSupport,
                                        agreedSupport,
                                        policy);
                }
        }

        private static RideRequestPointSnapshot point(
                        double longitude,
                        double latitude,
                        String address) {
                Point point = WGS84.createPoint(new Coordinate(longitude, latitude));
                point.setSRID(4326);
                return new RideRequestPointSnapshot(point, address);
        }

        private static LineString line(int srid, double... values) {
                Coordinate[] coordinates = new Coordinate[values.length / 2];
                for (int i = 0; i < values.length; i += 2) {
                        coordinates[i / 2] = new Coordinate(values[i], values[i + 1]);
                }
                GeometryFactory factory = new GeometryFactory(new PrecisionModel(), srid);
                LineString result = factory.createLineString(coordinates);
                result.setSRID(srid);
                return result;
        }
}
