package com.zanh.route_sharing.dto.sharedroute;

import com.zanh.route_sharing.testfixture.CreateSharedRouteRequestTestBuilder;
import com.zanh.route_sharing.testfixture.SharedRouteMother;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateSharedRouteRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation
                .byDefaultProvider()
                .configure()
                .clockProvider(() -> Clock.fixed(
                        SharedRouteMother.NOW,
                        ZoneOffset.UTC
                ))
                .buildValidatorFactory();

        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void givenValidRequest_whenValidated_thenHasNoViolations() {
        // Arrange
        CreateSharedRouteRequest request =
                CreateSharedRouteRequestTestBuilder
                        .aValidRequest()
                        .build();

        // Act
        Set<ConstraintViolation<CreateSharedRouteRequest>> violations =
                validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void givenDepartureTimeNotInFuture_whenValidated_thenReportsExpectedDepartureTimeError() {
        // Arrange
        CreateSharedRouteRequest request =
                CreateSharedRouteRequestTestBuilder
                        .aValidRequest()
                        .withDepartureTime(SharedRouteMother.NOW)
                        .build();

        // Act
        Set<ConstraintViolation<CreateSharedRouteRequest>> violations =
                validator.validate(request);

        // Assert
        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .contains("expectedDepartureTime");
    }

    @Test
    void givenInvalidOriginCoordinates_whenValidated_thenReportsNestedOriginErrors() {
        // Arrange
        RouteEndpointRequest invalidOrigin =
                SharedRouteMother.endpoint(
                        "91",
                        "181",
                        "Điểm lỗi"
                );

        CreateSharedRouteRequest request =
                CreateSharedRouteRequestTestBuilder
                        .aValidRequest()
                        .withOrigin(invalidOrigin)
                        .build();

        // Act
        Set<ConstraintViolation<CreateSharedRouteRequest>> violations =
                validator.validate(request);

        // Assert
        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .contains(
                        "origin.latitude",
                        "origin.longitude"
                );
    }
}
