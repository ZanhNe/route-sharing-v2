package com.zanh.route_sharing.dto.riderequest;

import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;
import com.zanh.route_sharing.testsupport.riderequest.CreateRideRequestRequestBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

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
        void givenValidBoundaryPayload_whenValidating_thenNoViolationIsReported() {
                CreateRideRequestRequest request = new CreateRideRequestRequest(
                                1L,
                                point("-90", "-180", "A"),
                                point("90", "180", "B".repeat(500)),
                                new BigDecimal("9999999999999.99"),
                                "N".repeat(1000));

                assertThat(validator.validate(request)).isEmpty();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidTopLevelFields")
        void givenInvalidTopLevelField_whenValidating_thenExpectedPropertyIsRejected(
                        String description,
                        CreateRideRequestRequest request,
                        String expectedProperty) {
                assertThat(propertyPaths(validator.validate(request))).contains(expectedProperty);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidPoints")
        void givenInvalidNestedPoint_whenValidating_thenNestedPropertyPathIsReported(
                        String description,
                        RouteEndpointRequest invalidPoint,
                        boolean pickup,
                        String nestedProperty) {
                CreateRideRequestRequestBuilder builder = new CreateRideRequestRequestBuilder();
                CreateRideRequestRequest request = pickup
                                ? builder.withPickup(invalidPoint).build()
                                : builder.withDestination(invalidPoint).build();

                String prefix = pickup ? "pickup." : "passengerDestination.";
                assertThat(propertyPaths(validator.validate(request)))
                                .contains(prefix + nestedProperty);
        }

        @Test
        void givenWhitespaceAroundText_whenConstructing_thenAddressesAndNoteAreNormalized() {
                CreateRideRequestRequest request = new CreateRideRequestRequest(
                                1L,
                                point("10.1", "106.1", "  Điểm đón  "),
                                point("10.2", "106.2", "  Điểm đến  "),
                                BigDecimal.ZERO,
                                "  Ghi chú  ");

                assertThat(request.pickup().address()).isEqualTo("Điểm đón");
                assertThat(request.passengerDestination().address()).isEqualTo("Điểm đến");
                assertThat(request.note()).isEqualTo("Ghi chú");
                assertThat(validator.validate(request)).isEmpty();
        }

        @Test
        void givenBlankNote_whenConstructing_thenNoteIsNormalizedToNull() {
                CreateRideRequestRequest request = new CreateRideRequestRequestBuilder()
                                .withNote("   \t\n")
                                .build();

                assertThat(request.note()).isNull();
                assertThat(validator.validate(request)).isEmpty();
        }

        private static Stream<Arguments> invalidTopLevelFields() {
                return Stream.of(
                                Arguments.of("school null", new CreateRideRequestRequestBuilder()
                                                .withSchoolId(null).build(), "schoolId"),
                                Arguments.of("school zero", new CreateRideRequestRequestBuilder()
                                                .withSchoolId(0L).build(), "schoolId"),
                                Arguments.of("pickup null", new CreateRideRequestRequestBuilder()
                                                .withPickup(null).build(), "pickup"),
                                Arguments.of("destination null", new CreateRideRequestRequestBuilder()
                                                .withDestination(null).build(), "passengerDestination"),
                                Arguments.of("support null", new CreateRideRequestRequestBuilder()
                                                .withProposedSupportAmount(null).build(), "proposedSupportAmount"),
                                Arguments.of("support negative", new CreateRideRequestRequestBuilder()
                                                .withProposedSupportAmount(new BigDecimal("-0.01")).build(),
                                                "proposedSupportAmount"),
                                Arguments.of("support too many integer digits", new CreateRideRequestRequestBuilder()
                                                .withProposedSupportAmount(new BigDecimal("10000000000000.00")).build(),
                                                "proposedSupportAmount"),
                                Arguments.of("support too many fraction digits", new CreateRideRequestRequestBuilder()
                                                .withProposedSupportAmount(new BigDecimal("1.001")).build(),
                                                "proposedSupportAmount"),
                                Arguments.of("note too long", new CreateRideRequestRequestBuilder()
                                                .withNote("N".repeat(1001)).build(), "note"));
        }

        private static Stream<Arguments> invalidPoints() {
                return Stream.of(
                                Arguments.of("latitude null", point(null, "106", "Địa chỉ"), true, "latitude"),
                                Arguments.of("latitude below -90", point("-90.000001", "106", "Địa chỉ"),
                                                true, "latitude"),
                                Arguments.of("latitude above 90", point("90.000001", "106", "Địa chỉ"),
                                                false, "latitude"),
                                Arguments.of("longitude null", point("10", null, "Địa chỉ"), true, "longitude"),
                                Arguments.of("longitude below -180", point("10", "-180.000001", "Địa chỉ"),
                                                false, "longitude"),
                                Arguments.of("longitude above 180", point("10", "180.000001", "Địa chỉ"),
                                                true, "longitude"),
                                Arguments.of("address null", point("10", "106", null), false, "address"),
                                Arguments.of("address blank", point("10", "106", "   "), true, "address"),
                                Arguments.of("address too long", point("10", "106", "A".repeat(501)),
                                                false, "address"));
        }

        private static Set<String> propertyPaths(Set<ConstraintViolation<CreateRideRequestRequest>> violations) {
                return violations.stream()
                                .map(violation -> violation.getPropertyPath().toString())
                                .collect(java.util.stream.Collectors.toSet());
        }

        private static RouteEndpointRequest point(String latitude, String longitude, String address) {
                return new RouteEndpointRequest(
                                latitude == null ? null : new BigDecimal(latitude),
                                longitude == null ? null : new BigDecimal(longitude),
                                address);
        }
}
