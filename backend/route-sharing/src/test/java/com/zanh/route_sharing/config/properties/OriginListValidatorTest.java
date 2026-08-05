package com.zanh.route_sharing.config.properties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OriginListValidatorTest {

    @Test
    void givenDistinctHttpOrigins_whenValidating_thenAccepted() {
        assertThat(OriginListValidator.isValid(List.of(
                "https://app.example.edu.vn",
                "http://localhost:5173")))
                .isTrue();
    }

    @Test
    void givenEquivalentDefaultPorts_whenValidating_thenRejectedAsDuplicates() {
        assertThat(OriginListValidator.isValid(List.of(
                "https://example.edu.vn",
                "https://example.edu.vn:443")))
                .isFalse();
    }

    @Test
    void givenWildcardOrNonOriginUrl_whenValidating_thenRejected() {
        assertThat(OriginListValidator.isValid(List.of("*"))).isFalse();
        assertThat(OriginListValidator.isValid(List.of("https://example.edu.vn/path"))).isFalse();
        assertThat(OriginListValidator.isValid(List.of("ftp://example.edu.vn"))).isFalse();
    }
}
