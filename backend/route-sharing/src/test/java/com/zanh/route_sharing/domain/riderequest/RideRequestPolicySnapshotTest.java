package com.zanh.route_sharing.domain.riderequest;

import com.zanh.route_sharing.testsupport.riderequest.RideRequestMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideRequestPolicySnapshotTest {

    @Test
    void givenValidBoundaryPolicy_whenCreating_thenImmutablePolicyIsAccepted() {
        RideRequestPolicySnapshot result = policy(
                1L, 0L, bd("0.01"), bd("0.01"), BigDecimal.ZERO,
                0L, bd("100"), Duration.ZERO, Duration.ZERO);

        assertThat(result.minimumConvenienceRatioPercent()).isEqualByComparingTo("100");
        assertThat(result.maxPickupDeviationMeters()).isZero();
        assertThat(result.bookingCutoff()).isZero();
        assertThat(result.rejectionCooldown()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPolicies")
    void givenInvalidPolicyField_whenCreating_thenInvariantIsRejected(
            String description,
            RideRequestPolicyFactory factory) {
        assertThatThrownBy(factory::create).isInstanceOfAny(
                IllegalArgumentException.class,
                NullPointerException.class);
    }

    @Test
    void motherPolicy_containsOnlyBookingCutoffAndRejectionCooldownTimeRules() {
        assertThat(RideRequestMother.policy().bookingCutoff()).isEqualTo(Duration.ofMinutes(15));
        assertThat(RideRequestMother.policy().rejectionCooldown()).isEqualTo(Duration.ofHours(1));
    }

    private static Stream<Arguments> invalidPolicies() {
        return Stream.of(
                Arguments.of("configuration id zero", factory(0L, 0L, bd("1"), bd("1"),
                        bd("0"), 0, bd("0"), sec(0), sec(0))),
                Arguments.of("configuration version negative", factory(1L, -1L, bd("1"), bd("1"),
                        bd("0"), 0, bd("0"), sec(0), sec(0))),
                Arguments.of("same destination radius zero", factory(1L, 0L, bd("0"), bd("1"),
                        bd("0"), 0, bd("0"), sec(0), sec(0))),
                Arguments.of("near route radius negative", factory(1L, 0L, bd("1"), bd("-1"),
                        bd("0"), 0, bd("0"), sec(0), sec(0))),
                Arguments.of("pickup deviation negative", factory(1L, 0L, bd("1"), bd("1"),
                        bd("-1"), 0, bd("0"), sec(0), sec(0))),
                Arguments.of("pickup seconds negative", factory(1L, 0L, bd("1"), bd("1"),
                        bd("0"), -1, bd("0"), sec(0), sec(0))),
                Arguments.of("ratio below zero", factory(1L, 0L, bd("1"), bd("1"),
                        bd("0"), 0, bd("-0.01"), sec(0), sec(0))),
                Arguments.of("ratio above one hundred", factory(1L, 0L, bd("1"), bd("1"),
                        bd("0"), 0, bd("100.01"), sec(0), sec(0))),
                Arguments.of("cutoff negative", factory(1L, 0L, bd("1"), bd("1"),
                        bd("0"), 0, bd("0"), sec(-1), sec(0))),
                Arguments.of("cooldown negative", factory(1L, 0L, bd("1"), bd("1"),
                        bd("0"), 0, bd("0"), sec(0), sec(-1))));
    }

    private static RideRequestPolicyFactory factory(
            Long id,
            Long version,
            BigDecimal same,
            BigDecimal near,
            BigDecimal maxDeviation,
            long maxSeconds,
            BigDecimal ratio,
            Duration cutoff,
            Duration cooldown) {
        return () -> policy(id, version, same, near, maxDeviation, maxSeconds,
                ratio, cutoff, cooldown);
    }

    private static RideRequestPolicySnapshot policy(
            Long id,
            Long version,
            BigDecimal same,
            BigDecimal near,
            BigDecimal maxDeviation,
            long maxSeconds,
            BigDecimal ratio,
            Duration cutoff,
            Duration cooldown) {
        return new RideRequestPolicySnapshot(
                id, version, same, near, maxDeviation, maxSeconds,
                ratio, cutoff, cooldown);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static Duration sec(long value) {
        return Duration.ofSeconds(value);
    }

    @FunctionalInterface
    private interface RideRequestPolicyFactory {
        RideRequestPolicySnapshot create();
    }
}
