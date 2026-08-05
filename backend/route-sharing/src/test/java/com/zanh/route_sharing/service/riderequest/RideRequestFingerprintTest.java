package com.zanh.route_sharing.service.riderequest;

import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import com.zanh.route_sharing.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideRequestFingerprintTest {

    private final RideRequestFingerprint sut = new RideRequestFingerprint();

    @Test
    void givenEquivalentNormalizedPayloads_whenCalculating_thenFingerprintIsStable() {
        CreateRideRequestRequest first = request(
                new BigDecimal("25000.00"),
                "  Tôi đứng tại cổng chính  ");
        CreateRideRequestRequest second = request(
                new BigDecimal("25000"),
                "Tôi đứng tại cổng chính");

        String firstFingerprint = sut.calculate(7L, 22L, first);
        String secondFingerprint = sut.calculate(7L, 22L, second);

        assertThat(firstFingerprint)
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .isEqualTo(secondFingerprint);
    }

    @Test
    void givenDifferentBusinessIntent_whenCalculating_thenFingerprintChanges() {
        String first = sut.calculate(7L, 22L, request(new BigDecimal("25000"), null));
        String second = sut.calculate(7L, 23L, request(new BigDecimal("25000"), null));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void givenMissingKey_whenNormalizing_thenMissingKeyErrorIsReturned() {
        assertThatThrownBy(() -> sut.normalizeKey("  "))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("MISSING_IDEMPOTENCY_KEY");
                    assertThat(exception.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void givenInvalidKeyCharset_whenNormalizing_thenInvalidKeyErrorIsReturned() {
        assertThatThrownBy(() -> sut.normalizeKey("booking key/01"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_IDEMPOTENCY_KEY"));
    }

    private static CreateRideRequestRequest request(BigDecimal support, String note) {
        return new CreateRideRequestRequest(
                1L,
                new RouteEndpointRequest(
                        new BigDecimal("10.776530"),
                        new BigDecimal("106.700981"),
                        "  Điểm đón hành khách  "),
                new RouteEndpointRequest(
                        new BigDecimal("10.782120"),
                        new BigDecimal("106.712450"),
                        "Điểm đến cuối cùng"),
                support,
                note);
    }
}
