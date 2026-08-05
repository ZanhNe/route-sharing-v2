package com.zanh.route_sharing.service.riderequest;

import com.zanh.route_sharing.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

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
    void givenCutoffReached_whenCalculating_thenRequestIsRejected() {
        assertThatThrownBy(() -> sut.calculate(
                SENT_AT,
                SENT_AT.plusSeconds(600),
                Duration.ofMinutes(15),
                Duration.ofMinutes(10)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("SHARED_ROUTE_BOOKING_CUTOFF_REACHED"));
    }
}
