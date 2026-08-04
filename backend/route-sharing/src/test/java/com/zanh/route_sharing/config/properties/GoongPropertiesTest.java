package com.zanh.route_sharing.config.properties;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class GoongPropertiesTest {
    @Test
    void exposesBackwardCompatibleDirectionsDefaults() {
        GoongProperties properties = new GoongProperties();

        assertThat(properties.getDirectionsPath()).isEqualTo("/v2/direction");
        assertThat(properties.getWaypointSnapToleranceMeters())
                .isEqualByComparingTo("100.00");
        assertThat(properties.getDuplicateWaypointToleranceMeters())
                .isEqualByComparingTo("2.00");
        assertThat(properties.isDuplicateToleranceWithinSnapTolerance()).isTrue();
    }

    @Test
    void rejectsDuplicateToleranceGreaterThanSnapTolerance() {
        GoongProperties properties = new GoongProperties();
        properties.setWaypointSnapToleranceMeters(new BigDecimal("10"));
        properties.setDuplicateWaypointToleranceMeters(new BigDecimal("11"));

        assertThat(properties.isDuplicateToleranceWithinSnapTolerance()).isFalse();
    }

    @Test
    void acceptsOnlyAnHttpsOrigin() {
        GoongProperties properties = new GoongProperties();
        properties.setBaseUrl(URI.create("https://rsapi.goong.io"));
        assertThat(properties.isSecureBaseUrl()).isTrue();

        properties.setBaseUrl(URI.create("http://rsapi.goong.io"));
        assertThat(properties.isSecureBaseUrl()).isFalse();

        properties.setBaseUrl(URI.create("https://user@rsapi.goong.io"));
        assertThat(properties.isSecureBaseUrl()).isFalse();

        properties.setBaseUrl(URI.create("https://rsapi.goong.io/custom/path"));
        assertThat(properties.isSecureBaseUrl()).isFalse();
    }
}
