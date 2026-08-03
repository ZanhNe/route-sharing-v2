package com.zanh.route_sharing.repository.sharedroute;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.zanh.route_sharing.repository.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static com.zanh.route_sharing.testsupport.sharedroute.SharedRouteSearchContextMother.standardConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostgisSharedRouteSearchRepositoryIntegrationTest {

    @Autowired
    private SharedRouteSearchRepository sut;

    @Test
    void givenUnknownActorAndSchool_whenLoadingSearchContext_thenEmptyIsReturned() {
        // Arrange
        Long unknownActorId = Long.MAX_VALUE;
        Long unknownSchoolId = Long.MAX_VALUE;

        // Act
        var context = sut.findSearchContext(
                unknownActorId,
                unknownSchoolId,
                LocalDate.of(2026, 8, 3));

        // Assert
        assertThat(context).isEmpty();
    }

    @Test
    void givenNoEligibleRoutesInSelectedScope_whenSearching_thenEmptyPageIsReturned() {
        // Arrange
        Instant now = Instant.parse("2026-08-03T03:00:00Z");
        SharedRouteSearchCriteria criteria = new SharedRouteSearchCriteria(
                Long.MAX_VALUE,
                Long.MAX_VALUE,
                new BigDecimal("10.77"),
                new BigDecimal("106.69"),
                new BigDecimal("10.80"),
                new BigDecimal("106.72"),
                now,
                LocalDate.of(2026, 8, 3),
                now,
                now.plusSeconds(3600),
                standardConfiguration(),
                0,
                10);

        // Act
        SharedRouteSearchPage result = sut.search(criteria);

        // Assert
        assertThat(result.rows()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }
}
