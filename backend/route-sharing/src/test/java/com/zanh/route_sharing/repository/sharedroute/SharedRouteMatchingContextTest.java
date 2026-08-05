package com.zanh.route_sharing.repository.sharedroute;

import com.zanh.route_sharing.repository.sharedroute.common.model.SharedRouteMatchingContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedRouteMatchingContextTest {

    @Test
    void givenValidConfiguration_whenCreated_thenValuesArePreserved() {
        // Arrange & Act
        SharedRouteMatchingContext context = new SharedRouteMatchingContext(
                new BigDecimal("500"),
                new BigDecimal("300"),
                new BigDecimal("0"),
                30);

        // Assert
        assertThat(context.sameDestinationRadiusMeters()).isEqualByComparingTo("500");
        assertThat(context.destinationNearRouteRadiusMeters()).isEqualByComparingTo("300");
        assertThat(context.maxPickupDeviationMeters()).isZero();
        assertThat(context.departureToleranceMinutes()).isEqualTo(30);
    }

    @ParameterizedTest(name = "same={0}, nearRoute={1}")
    @MethodSource("invalidPositiveRadii")
    void givenNonPositiveRequiredRadius_whenCreated_thenIllegalArgumentIsThrown(
            BigDecimal sameDestinationRadius,
            BigDecimal destinationNearRouteRadius) {
        // Act & Assert
        assertThatThrownBy(() -> new SharedRouteMatchingContext(
                sameDestinationRadius,
                destinationNearRouteRadius,
                BigDecimal.ZERO,
                0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNegativePickupDeviation_whenCreated_thenIllegalArgumentIsThrown() {
        // Act & Assert
        assertThatThrownBy(() -> new SharedRouteMatchingContext(
                BigDecimal.ONE,
                BigDecimal.ONE,
                new BigDecimal("-0.01"),
                0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNegativeDepartureTolerance_whenCreated_thenIllegalArgumentIsThrown() {
        // Act & Assert
        assertThatThrownBy(() -> new SharedRouteMatchingContext(
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> invalidPositiveRadii() {
        return Stream.of(
                Arguments.of(null, BigDecimal.ONE),
                Arguments.of(BigDecimal.ZERO, BigDecimal.ONE),
                Arguments.of(new BigDecimal("-1"), BigDecimal.ONE),
                Arguments.of(BigDecimal.ONE, null),
                Arguments.of(BigDecimal.ONE, BigDecimal.ZERO),
                Arguments.of(BigDecimal.ONE, new BigDecimal("-1")));
    }
}
