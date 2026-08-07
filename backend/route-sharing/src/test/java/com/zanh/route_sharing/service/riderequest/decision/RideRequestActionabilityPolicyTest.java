package com.zanh.route_sharing.service.riderequest.decision;

import com.zanh.route_sharing.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideRequestActionabilityPolicyTest {

    private final RideRequestActionabilityPolicy sut = new RideRequestActionabilityPolicy();
    private final Instant now = Instant.parse("2026-08-06T06:30:00Z");

    @Test
    void givenTimeBeforeCurrentCutoff_whenCheckingAcceptWindow_thenAccepted() {
        assertThatCode(() -> sut.requireAcceptWindowOpen(
                now,
                now.plusSeconds(901),
                900L)).doesNotThrowAnyException();
    }

    @Test
    void givenTimeAtCutoff_whenCheckingAcceptWindow_thenCutoffConflict() {
        assertThatThrownBy(() -> sut.requireAcceptWindowOpen(
                now,
                now.plusSeconds(900),
                900L)).isInstanceOfSatisfying(BusinessException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getCode())
                                .isEqualTo("SHARED_ROUTE_BOOKING_CUTOFF_REACHED"));
    }

    @Test
    void givenTimeAfterCutoff_whenCheckingAcceptWindow_thenCutoffConflict() {
        assertThatThrownBy(() -> sut.requireAcceptWindowOpen(
                now.plusSeconds(1),
                now.plusSeconds(900),
                900L)).isInstanceOf(BusinessException.class);
    }
}
