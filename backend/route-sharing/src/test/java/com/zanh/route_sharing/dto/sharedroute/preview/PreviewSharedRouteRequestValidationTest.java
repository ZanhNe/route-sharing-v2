package com.zanh.route_sharing.dto.sharedroute.preview;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PreviewSharedRouteRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void givenValidRequest_whenValidated_thenNoViolationIsReturned() {
        PreviewSharedRouteRequest request = validRequest();

        Set<ConstraintViolation<PreviewSharedRouteRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void givenNonPositiveSchoolId_whenValidated_thenSchoolIdIsRejected() {
        PreviewSharedRouteRequest request = new PreviewSharedRouteRequest(
                0L,
                validPoint("Điểm đón"),
                validPoint("Điểm đến"));

        Set<ConstraintViolation<PreviewSharedRouteRequest>> violations = validator.validate(request);

        assertThat(paths(violations)).contains("schoolId");
    }

    @Test
    void givenOutOfRangeCoordinate_whenValidated_thenNestedCoordinateIsRejected() {
        PreviewSharedRouteRequest request = new PreviewSharedRouteRequest(
                1L,
                new PreviewPointRequest(new BigDecimal("91"), new BigDecimal("106"), "Điểm đón"),
                validPoint("Điểm đến"));

        Set<ConstraintViolation<PreviewSharedRouteRequest>> violations = validator.validate(request);

        assertThat(paths(violations)).contains("pickup.latitude");
    }

    @Test
    void givenBlankAddress_whenValidated_thenAddressIsRejected() {
        PreviewSharedRouteRequest request = new PreviewSharedRouteRequest(
                1L,
                new PreviewPointRequest(new BigDecimal("10.77"), new BigDecimal("106.69"), "   "),
                validPoint("Điểm đến"));

        Set<ConstraintViolation<PreviewSharedRouteRequest>> violations = validator.validate(request);

        assertThat(paths(violations)).contains("pickup.address");
    }

    @Test
    void givenAddressWithWhitespace_whenPointIsCreated_thenAddressIsTrimmed() {
        PreviewPointRequest point = new PreviewPointRequest(
                new BigDecimal("10.77"),
                new BigDecimal("106.69"),
                "  Điểm đón  ");

        assertThat(point.address()).isEqualTo("Điểm đón");
    }

    private static PreviewSharedRouteRequest validRequest() {
        return new PreviewSharedRouteRequest(
                1L,
                validPoint("Điểm đón"),
                new PreviewPointRequest(
                        new BigDecimal("10.78"),
                        new BigDecimal("106.705"),
                        "Điểm đến"));
    }

    private static PreviewPointRequest validPoint(String address) {
        return new PreviewPointRequest(
                new BigDecimal("10.77"),
                new BigDecimal("106.69"),
                address);
    }

    private static Set<String> paths(Set<ConstraintViolation<PreviewSharedRouteRequest>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
