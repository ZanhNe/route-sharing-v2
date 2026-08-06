package com.zanh.route_sharing.service.riderequest;

import com.zanh.route_sharing.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideRequestExpiryPolicyTest {

    private static final Instant SENT_AT = Instant.parse("2026-08-05T12:00:00Z");
    private final RideRequestExpiryPolicy sut = new RideRequestExpiryPolicy();

    @Test
    void givenTtlEarlierThanCutoff_whenCalculating_thenTtlBoundaryIsUsed() {
        Instant result = sut.calculate(
                SENT_AT,
                SENT_AT.plusSeconds(3600),
                Duration.ofMinutes(15),
                Duration.ofMinutes(10));

        assertThat(result).isEqualTo(SENT_AT.plusSeconds(900));
    }

    @Test
    void givenCutoffEarlierThanTtl_whenCalculating_thenCutoffBoundaryIsUsed() {
        Instant result = sut.calculate(
                SENT_AT,
                SENT_AT.plusSeconds(1200),
                Duration.ofMinutes(30),
                Duration.ofMinutes(10));

        assertThat(result).isEqualTo(SENT_AT.plusSeconds(600));
    }

    @Test
    void givenEqualBoundaries_whenCalculating_thenSharedBoundaryIsUsed() {
        Instant result = sut.calculate(
                SENT_AT,
                SENT_AT.plusSeconds(1500),
                Duration.ofMinutes(15),
                Duration.ofMinutes(10));

        assertThat(result).isEqualTo(SENT_AT.plusSeconds(900));
    }

    @Test
    void givenZeroBookingCutoff_whenCalculating_thenDepartureCanBeTheExpiryBoundary() {
        Instant result = sut.calculate(
                SENT_AT,
                SENT_AT.plusSeconds(300),
                Duration.ofHours(1),
                Duration.ZERO);

        assertThat(result).isEqualTo(SENT_AT.plusSeconds(300));
    }

    @ParameterizedTest
    @MethodSource("closedWindows")
    void givenClosedBookingWindow_whenCalculating_thenStableConflictIsReturned(
            Instant departure,
            Duration ttl,
            Duration cutoff) {
        assertThatThrownBy(() -> sut.calculate(SENT_AT, departure, ttl, cutoff))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(409);
                    assertThat(exception.getCode())
                            .isEqualTo("SHARED_ROUTE_BOOKING_CUTOFF_REACHED");
                });
    }

    @ParameterizedTest
    @MethodSource("invalidDurations")
    void givenInvalidDurationConfiguration_whenCalculating_thenProgrammingErrorIsRejected(
            Duration ttl,
            Duration cutoff) {
        assertThatThrownBy(() -> sut.calculate(
                SENT_AT,
                SENT_AT.plusSeconds(3600),
                ttl,
                cutoff))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenTemporalOverflow_whenCalculating_thenBookingWindowConflictIsReturned() {
        assertThatThrownBy(() -> sut.calculate(
                Instant.MAX.minusSeconds(1),
                Instant.MAX,
                Duration.ofDays(1),
                Duration.ZERO))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("SHARED_ROUTE_BOOKING_CUTOFF_REACHED"));
    }

    @ParameterizedTest
    @MethodSource("nullArguments")
    void givenMissingRequiredArgument_whenCalculating_thenNullIsRejected(
            Instant sentAt,
            Instant departure,
            Duration ttl,
            Duration cutoff) {
        assertThatThrownBy(() -> sut.calculate(sentAt, departure, ttl, cutoff))
                .isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> closedWindows() {
        return Stream.of(
                Arguments.of(SENT_AT.plusSeconds(600), Duration.ofMinutes(15), Duration.ofMinutes(10)),
                Arguments.of(SENT_AT.plusSeconds(599), Duration.ofMinutes(15), Duration.ofMinutes(10)),
                Arguments.of(SENT_AT.minusSeconds(1), Duration.ofMinutes(15), Duration.ZERO));
    }

    private static Stream<Arguments> invalidDurations() {
        return Stream.of(
                Arguments.of(Duration.ZERO, Duration.ZERO),
                Arguments.of(Duration.ofSeconds(-1), Duration.ZERO),
                Arguments.of(Duration.ofMinutes(15), Duration.ofSeconds(-1)));
    }

    private static Stream<Arguments> nullArguments() {
        return Stream.of(
                Arguments.of(null, SENT_AT.plusSeconds(3600), Duration.ofMinutes(15), Duration.ZERO),
                Arguments.of(SENT_AT, null, Duration.ofMinutes(15), Duration.ZERO),
                Arguments.of(SENT_AT, SENT_AT.plusSeconds(3600), null, Duration.ZERO),
                Arguments.of(SENT_AT, SENT_AT.plusSeconds(3600), Duration.ofMinutes(15), null));
    }
}
