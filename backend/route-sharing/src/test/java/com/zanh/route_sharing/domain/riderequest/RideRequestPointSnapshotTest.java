package com.zanh.route_sharing.domain.riderequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideRequestPointSnapshotTest {

    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    void givenValidWgs84PointAndAddress_whenCreating_thenAddressIsTrimmed() {
        RideRequestPointSnapshot result = new RideRequestPointSnapshot(
                point(106.700981, 10.776530, 4326),
                "  Điểm đón  ");

        assertThat(result.address()).isEqualTo("Điểm đón");
        assertThat(result.point().getX()).isEqualTo(106.700981);
        assertThat(result.point().getY()).isEqualTo(10.776530);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPoints")
    void givenInvalidSpatialPoint_whenCreating_thenInvariantIsRejected(
            String description,
            Point point) {
        assertThatThrownBy(() -> new RideRequestPointSnapshot(point, "Địa chỉ"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidAddresses")
    void givenInvalidAddress_whenCreating_thenInvariantIsRejected(String address) {
        assertThatThrownBy(() -> new RideRequestPointSnapshot(
                point(106.7, 10.7, 4326),
                address))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNullPoint_whenCreating_thenNullIsRejected() {
        assertThatThrownBy(() -> new RideRequestPointSnapshot(null, "Địa chỉ"))
                .isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> invalidPoints() {
        Point empty = WGS84.createPoint((Coordinate) null);
        return Stream.of(
                Arguments.of("wrong SRID", point(106.7, 10.7, 0)),
                Arguments.of("longitude below range", point(-180.1, 10.7, 4326)),
                Arguments.of("longitude above range", point(180.1, 10.7, 4326)),
                Arguments.of("latitude below range", point(106.7, -90.1, 4326)),
                Arguments.of("latitude above range", point(106.7, 90.1, 4326)),
                Arguments.of("empty point", empty));
    }

    private static Stream<String> invalidAddresses() {
        return Stream.of(null, "", "   ", "A".repeat(501));
    }

    private static Point point(double longitude, double latitude, int srid) {
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), srid);
        Point point = factory.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(srid);
        return point;
    }
}
