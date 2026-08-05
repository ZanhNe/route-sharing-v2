package com.zanh.route_sharing.dto.riderequest;

import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CreateRideRequestRequestValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void givenValidPayload_whenValidating_thenNoViolationIsReported() {
        CreateRideRequestRequest request = request(new BigDecimal("25000.00"), "Ghi chú");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void givenNegativeSupport_whenValidating_thenProposedSupportFieldIsRejected() {
        CreateRideRequestRequest request = request(new BigDecimal("-0.01"), null);

        assertThat(validator.validate(request))
                .anySatisfy(violation ->
                        assertThat(violation.getPropertyPath().toString())
                                .isEqualTo("proposedSupportAmount"));
    }

    @Test
    void givenBlankNote_whenConstructing_thenNoteIsNormalizedToNull() {
        CreateRideRequestRequest request = request(new BigDecimal("0"), "   ");

        assertThat(request.note()).isNull();
        assertThat(request.pickup().address()).isEqualTo("Điểm đón");
    }

    private static CreateRideRequestRequest request(BigDecimal support, String note) {
        return new CreateRideRequestRequest(
                1L,
                new RouteEndpointRequest(
                        new BigDecimal("10.776530"),
                        new BigDecimal("106.700981"),
                        "  Điểm đón  "),
                new RouteEndpointRequest(
                        new BigDecimal("10.782120"),
                        new BigDecimal("106.712450"),
                        "Điểm đến"),
                support,
                note);
    }
}
