package com.zanh.route_sharing.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.async")
public class AsyncProperties {
    @Min(1)
    private int corePoolSize = 5;
    @Min(1)
    private int maxPoolSize = 10;
    @Min(0)
    private int queueCapacity = 500;
    @NotBlank
    private String threadNamePrefix = "RouteShare-Async-";
    @NotNull
    private Duration awaitTermination = Duration.ofSeconds(30);

    @AssertTrue(message = "app.async requires max-pool-size >= core-pool-size and a non-negative await-termination")
    public boolean isValidConfiguration() {
        if (maxPoolSize < corePoolSize || awaitTermination == null || awaitTermination.isNegative()) {
            return false;
        }
        return awaitTermination.toSeconds() <= Integer.MAX_VALUE;
    }
}
