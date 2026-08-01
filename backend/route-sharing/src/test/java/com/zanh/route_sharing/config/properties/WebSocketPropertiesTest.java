package com.zanh.route_sharing.config.properties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketPropertiesTest {
    @Test
    void acceptsExactHttpOriginsWithoutPaths() {
        assertThat(WebSocketProperties.areValidOrigins(List.of(
                "https://app.example.edu.vn",
                "https://internal.example.edu.vn:8443"
        ))).isTrue();
    }

    @Test
    void rejectsWildcardsCredentialsPathsAndEquivalentDefaultPorts() {
        assertThat(WebSocketProperties.areValidOrigins(List.of("*"))).isFalse();
        assertThat(WebSocketProperties.areValidOrigins(List.of("https://user@example.edu.vn"))).isFalse();
        assertThat(WebSocketProperties.areValidOrigins(List.of("https://example.edu.vn/path"))).isFalse();
        assertThat(WebSocketProperties.areValidOrigins(List.of(
                "http://example.edu.vn",
                "http://example.edu.vn:80"
        ))).isFalse();
    }
}
