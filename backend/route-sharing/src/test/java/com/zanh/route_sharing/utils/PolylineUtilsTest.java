package com.zanh.route_sharing.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolylineUtilsTest {
    @Test
    void decodesStandardGooglePolyline() {
        var line = PolylineUtils.decodeToLineString("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
        assertThat(line.getSRID()).isEqualTo(4326);
        assertThat(line.getNumPoints()).isEqualTo(3);
        assertThat(line.getCoordinateN(0).x).isCloseTo(-120.2, within(0.00001));
        assertThat(line.getCoordinateN(0).y).isCloseTo(38.5, within(0.00001));
    }

    @Test
    void rejectsTruncatedPolyline() {
        assertThatThrownBy(() -> PolylineUtils.decodeToLineString("_p~iF~ps|U_"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
