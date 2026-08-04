package com.zanh.route_sharing.repository.sharedroute;

import com.zanh.route_sharing.repository.SharedRouteSearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Stream;

import static com.zanh.route_sharing.testsupport.sharedroute.SharedRouteSearchContextMother.standardConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedRouteSearchCriteriaTest {

    private static final Instant NOW = Instant.parse("2026-08-03T03:00:00Z");
    private static final LocalDate TRAVEL_DATE = LocalDate.of(2026, 8, 3);

    @Test
    void givenPageAndSize_whenCalculatingOffset_thenLongOffsetIsReturned() {
        // Arrange
        SharedRouteSearchCriteria criteria = validCriteria(3, 25);

        // Act
        long offset = criteria.offset();

        // Assert
        assertThat(offset).isEqualTo(75L);
    }

    @Test
    void givenRequestedTravelDate_whenCreated_thenDateIsPreserved() {
        // Act
        SharedRouteSearchCriteria criteria = validCriteria(0, 10);

        // Assert
        assertThat(criteria.requestedTravelDate()).isEqualTo(TRAVEL_DATE);
        assertThat(criteria.membershipDate()).isEqualTo(TRAVEL_DATE);
    }

    @ParameterizedTest(name = "actor={0}, school={1}")
    @MethodSource("invalidActorOrSchool")
    void givenInvalidActorOrSchool_whenCreated_thenIllegalArgumentIsThrown(
            Long actorUserId,
            Long schoolId) {
        // Act & Assert
        assertThatThrownBy(() -> new SharedRouteSearchCriteria(
                actorUserId,
                schoolId,
                new BigDecimal("10.77"),
                new BigDecimal("106.69"),
                new BigDecimal("10.80"),
                new BigDecimal("106.72"),
                NOW,
                TRAVEL_DATE,
                NOW,
                NOW.plusSeconds(3600),
                standardConfiguration(),
                0,
                10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenMissingRequestedTravelDate_whenCreated_thenIllegalArgumentIsThrown() {
        // Act & Assert
        assertThatThrownBy(() -> new SharedRouteSearchCriteria(
                7L,
                3L,
                new BigDecimal("10.77"),
                new BigDecimal("106.69"),
                new BigDecimal("10.80"),
                new BigDecimal("106.72"),
                NOW,
                null,
                NOW,
                NOW.plusSeconds(3600),
                standardConfiguration(),
                0,
                10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenDepartureEndBeforeStart_whenCreated_thenIllegalArgumentIsThrown() {
        // Act & Assert
        assertThatThrownBy(() -> new SharedRouteSearchCriteria(
                7L,
                3L,
                new BigDecimal("10.77"),
                new BigDecimal("106.69"),
                new BigDecimal("10.80"),
                new BigDecimal("106.72"),
                NOW,
                TRAVEL_DATE,
                NOW.plusSeconds(60),
                NOW,
                standardConfiguration(),
                0,
                10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "page={0}, size={1}")
    @MethodSource("invalidPaging")
    void givenInvalidPaging_whenCreated_thenIllegalArgumentIsThrown(
            int page,
            int size) {
        // Act & Assert
        assertThatThrownBy(() -> validCriteria(page, size))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SharedRouteSearchCriteria validCriteria(int page, int size) {
        return new SharedRouteSearchCriteria(
                7L,
                3L,
                new BigDecimal("10.77"),
                new BigDecimal("106.69"),
                new BigDecimal("10.80"),
                new BigDecimal("106.72"),
                NOW,
                TRAVEL_DATE,
                NOW,
                NOW.plusSeconds(3600),
                standardConfiguration(),
                page,
                size);
    }

    private static Stream<Arguments> invalidActorOrSchool() {
        return Stream.of(
                Arguments.of(null, 3L),
                Arguments.of(0L, 3L),
                Arguments.of(-1L, 3L),
                Arguments.of(7L, null),
                Arguments.of(7L, 0L),
                Arguments.of(7L, -1L));
    }

    private static Stream<Arguments> invalidPaging() {
        return Stream.of(
                Arguments.of(-1, 10),
                Arguments.of(0, 0),
                Arguments.of(0, 51));
    }
}
