package com.zanh.route_sharing.dto.sharedroute.search;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

import static com.zanh.route_sharing.testsupport.sharedroute.SearchPointRequestBuilder.aSearchPoint;
import static com.zanh.route_sharing.testsupport.sharedroute.SearchSharedRoutesRequestBuilder.aSearchRequest;
import static org.assertj.core.api.Assertions.assertThat;

class SearchSharedRoutesRequestValidationTest {

    private static final Instant FIXED_DEPARTURE =
            Instant.parse("2026-08-03T04:00:00Z");

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
    void givenValidSearchRequest_whenValidated_thenNoViolationIsReturned() {
        // Arrange
        SearchSharedRoutesRequest request = aSearchRequest()
                .withDesiredDepartureTime(FIXED_DEPARTURE)
                .build();

        // Act
        Set<ConstraintViolation<SearchSharedRoutesRequest>> violations =
                validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "schoolId={0}")
    @MethodSource("invalidSchoolIds")
    void givenInvalidSchoolId_whenValidated_thenSchoolIdIsRejected(Long schoolId) {
        // Arrange
        SearchSharedRoutesRequest request = aSearchRequest()
                .withSchoolId(schoolId)
                .withDesiredDepartureTime(FIXED_DEPARTURE)
                .build();

        // Act
        Set<ConstraintViolation<SearchSharedRoutesRequest>> violations =
                validator.validate(request);

        // Assert
        assertThat(propertyPaths(violations)).contains("schoolId");
    }

    @ParameterizedTest(name = "invalid path={0}")
    @MethodSource("invalidCoordinates")
    void givenCoordinateOutsideWgs84Range_whenValidated_thenCoordinateIsRejected(
            String expectedPath,
            SearchSharedRoutesRequest request) {
        // Act
        Set<ConstraintViolation<SearchSharedRoutesRequest>> violations =
                validator.validate(request);

        // Assert
        assertThat(propertyPaths(violations)).contains(expectedPath);
    }

    @ParameterizedTest(name = "address={0}")
    @MethodSource("blankAddresses")
    void givenBlankPickupAddress_whenValidated_thenPickupAddressIsRejected(String address) {
        // Arrange
        SearchSharedRoutesRequest request = aSearchRequest()
                .withPickup(aSearchPoint().withAddress(address).build())
                .withDesiredDepartureTime(FIXED_DEPARTURE)
                .build();

        // Act
        Set<ConstraintViolation<SearchSharedRoutesRequest>> violations =
                validator.validate(request);

        // Assert
        assertThat(propertyPaths(violations)).contains("pickup.address");
    }

    @Test
    void givenAddressWithSurroundingWhitespace_whenPointIsCreated_thenAddressIsTrimmed() {
        // Arrange & Act
        SearchPointRequest point = aSearchPoint()
                .withAddress("  Đại học A  ")
                .build();

        // Assert
        assertThat(point.address()).isEqualTo("Đại học A");
    }

    @Test
    void givenMissingDesiredDepartureTime_whenValidated_thenDepartureTimeIsRejected() {
        // Arrange
        SearchSharedRoutesRequest request = aSearchRequest()
                .withDesiredDepartureTime(null)
                .build();

        // Act
        Set<ConstraintViolation<SearchSharedRoutesRequest>> violations =
                validator.validate(request);

        // Assert
        assertThat(propertyPaths(violations)).contains("desiredDepartureTime");
    }

    private static Stream<Long> invalidSchoolIds() {
        return Stream.of(null, 0L, -1L);
    }

    private static Stream<Arguments> invalidCoordinates() {
        return Stream.of(
                Arguments.of(
                        "pickup.latitude",
                        aSearchRequest()
                                .withPickup(aSearchPoint().withLatitude("91").build())
                                .withDesiredDepartureTime(FIXED_DEPARTURE)
                                .build()),
                Arguments.of(
                        "pickup.latitude",
                        aSearchRequest()
                                .withPickup(aSearchPoint().withLatitude("-91").build())
                                .withDesiredDepartureTime(FIXED_DEPARTURE)
                                .build()),
                Arguments.of(
                        "destination.longitude",
                        aSearchRequest()
                                .withDestination(aSearchPoint().withLongitude("181").build())
                                .withDesiredDepartureTime(FIXED_DEPARTURE)
                                .build()),
                Arguments.of(
                        "destination.longitude",
                        aSearchRequest()
                                .withDestination(aSearchPoint().withLongitude("-181").build())
                                .withDesiredDepartureTime(FIXED_DEPARTURE)
                                .build()));
    }

    private static Stream<String> blankAddresses() {
        return Stream.of(null, "", "   ");
    }

    private static Set<String> propertyPaths(
            Set<ConstraintViolation<SearchSharedRoutesRequest>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
