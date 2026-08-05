package com.zanh.route_sharing.utils.spatial;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class Wgs84CoordinatesTest {

    @Test
    void givenBoundaryCoordinates_whenValidating_thenAccepted() {
        assertThat(Wgs84Coordinates.isValid(new BigDecimal("-90"), new BigDecimal("-180")))
                .isTrue();
        assertThat(Wgs84Coordinates.isValid(new BigDecimal("90"), new BigDecimal("180")))
                .isTrue();
    }

    @Test
    void givenCoordinateOutsideWgs84_whenValidating_thenRejected() {
        assertThat(Wgs84Coordinates.isValid(new BigDecimal("90.0001"), BigDecimal.ZERO))
                .isFalse();
        assertThat(Wgs84Coordinates.isValid(BigDecimal.ZERO, new BigDecimal("180.0001")))
                .isFalse();
        assertThat(Wgs84Coordinates.isValidLongitudeLatitude(Double.NaN, 10.0d))
                .isFalse();
    }

    @Test
    void givenEquivalentBigDecimalScales_whenComparing_thenCoordinatesAreSame() {
        assertThat(Wgs84Coordinates.same(
                new BigDecimal("10.0"),
                new BigDecimal("106.00"),
                new BigDecimal("10.000"),
                new BigDecimal("106")))
                .isTrue();
    }
}
