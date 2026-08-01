package com.zanh.route_sharing.config.properties;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncPropertiesTest {
    @Test
    void acceptsAConsistentPoolConfiguration() {
        AsyncProperties properties = new AsyncProperties();
        properties.setCorePoolSize(5);
        properties.setMaxPoolSize(10);
        properties.setAwaitTermination(Duration.ofSeconds(30));

        assertThat(properties.isValidConfiguration()).isTrue();
    }

    @Test
    void rejectsMaxPoolBelowCorePoolAndNegativeShutdownTimeout() {
        AsyncProperties properties = new AsyncProperties();
        properties.setCorePoolSize(10);
        properties.setMaxPoolSize(5);
        assertThat(properties.isValidConfiguration()).isFalse();

        properties.setMaxPoolSize(10);
        properties.setAwaitTermination(Duration.ofSeconds(-1));
        assertThat(properties.isValidConfiguration()).isFalse();
    }
}
