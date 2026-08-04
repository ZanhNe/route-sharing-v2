package com.zanh.route_sharing.repository.sharedroute;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.repository.SharedRouteSearchContext;
import com.zanh.route_sharing.repository.SharedRouteSearchCriteria;
import com.zanh.route_sharing.repository.SharedRouteSearchPage;
import com.zanh.route_sharing.repository.SharedRouteSearchRepository;
import com.zanh.route_sharing.repository.SharedRouteSearchRow;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture.IneligibleMutation;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture.Scenario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostgisSharedRouteSearchRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-03T03:00:00Z");
    private static final Instant DEPARTURE = Instant.parse("2026-08-03T04:00:00Z");

    @Autowired
    private SharedRouteSearchRepository sut;

    @Autowired
    private EntityManager entityManager;

    private SharedRouteSearchDatabaseFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new SharedRouteSearchDatabaseFixture(entityManager);
    }

    @Test
    void givenActiveApprovedMemberAndConfiguredSchool_whenLoadingContext_thenConfigurationIsReturned() {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);

        // Act
        SharedRouteSearchContext context = sut.findSearchContext(
                        scenario.actorId(),
                        scenario.schoolId(),
                        scenario.travelDate())
                .orElseThrow();

        // Assert
        assertThat(context.sameDestinationRadiusMeters())
                .isEqualByComparingTo("200.00");
        assertThat(context.destinationNearRouteRadiusMeters())
                .isEqualByComparingTo("150.00");
        assertThat(context.maxPickupDeviationMeters())
                .isEqualByComparingTo("150.00");
        assertThat(context.departureToleranceMinutes()).isEqualTo(30);
    }

    @Test
    void givenUnknownActorAndSchool_whenLoadingSearchContext_thenEmptyIsReturned() {
        // Act
        var context = sut.findSearchContext(
                Long.MAX_VALUE,
                Long.MAX_VALUE,
                LocalDate.of(2026, 8, 3));

        // Assert
        assertThat(context).isEmpty();
    }

    @Test
    void givenDestinationInsideSameDestinationRadius_whenSearching_thenSameDestinationMatchIsReturned() {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
        SharedRouteSearchCriteria criteria = criteria(
                scenario,
                "10.770100",
                "106.690000",
                "10.770100",
                "106.720100",
                0,
                10);

        // Act
        SharedRouteSearchPage page = sut.search(criteria);

        // Assert
        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(page.rows()).hasSize(1);

        SharedRouteSearchRow row = page.rows().get(0);
        assertThat(row.sharedRouteId()).isEqualTo(scenario.routeId());
        assertThat(row.matchType()).isEqualTo(LoaiGhepTuyen.CUNG_DIEM_DEN);
        assertThat(row.dropoffType()).isEqualTo(LoaiDiemTha.DIEM_DICH_CUOI_CUNG);
        assertThat(row.proposedDropoffLatitude())
                .isCloseTo(new BigDecimal("10.770100"), within(new BigDecimal("0.000001")));
        assertThat(row.proposedDropoffLongitude())
                .isCloseTo(new BigDecimal("106.720100"), within(new BigDecimal("0.000001")));
        assertThat(row.destinationDeviationMeters()).isBetween(
                BigDecimal.ZERO,
                new BigDecimal("200.00"));
        assertThat(row.sharedSegmentMeters()).isPositive();
        assertThat(row.routeGeoJson()).contains("LineString");
    }

    @Test
    void givenDestinationNearRouteAndAfterPickup_whenSearching_thenRouteSegmentMatchIsReturned() {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
        SharedRouteSearchCriteria criteria = criteria(
                scenario,
                "10.770100",
                "106.690000",
                "10.770100",
                "106.705000",
                0,
                10);

        // Act
        SharedRouteSearchPage page = sut.search(criteria);

        // Assert
        assertThat(page.totalElements()).isEqualTo(1L);
        SharedRouteSearchRow row = page.rows().get(0);
        assertThat(row.sharedRouteId()).isEqualTo(scenario.routeId());
        assertThat(row.matchType()).isEqualTo(LoaiGhepTuyen.TRUNG_DOAN_TUYEN);
        assertThat(row.dropoffType()).isEqualTo(LoaiDiemTha.DIEM_THA_TRUNG_GIAN);
        assertThat(row.proposedDropoffLatitude())
                .isCloseTo(new BigDecimal("10.770000"), within(new BigDecimal("0.000001")));
        assertThat(row.proposedDropoffLongitude())
                .isCloseTo(new BigDecimal("106.705000"), within(new BigDecimal("0.000001")));
        assertThat(row.pickupDeviationMeters()).isNotNegative();
        assertThat(row.destinationDeviationMeters()).isBetween(
                BigDecimal.ZERO,
                new BigDecimal("150.00"));
        assertThat(row.sharedSegmentMeters()).isPositive();
    }

    @Test
    void givenRouteMatchingBothTiers_whenSearching_thenSameDestinationTierIsReturnedOnlyOnce() {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
        SharedRouteSearchCriteria criteria = criteria(
                scenario,
                "10.770100",
                "106.690000",
                "10.770100",
                "106.720100",
                0,
                10);

        // Act
        SharedRouteSearchPage page = sut.search(criteria);

        // Assert
        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(page.rows()).singleElement()
                .extracting(SharedRouteSearchRow::matchType)
                .isEqualTo(LoaiGhepTuyen.CUNG_DIEM_DEN);
    }

    @Test
    void givenDestinationBeforePickupAlongRoute_whenSearching_thenRouteIsExcluded() {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
        SharedRouteSearchCriteria criteria = criteria(
                scenario,
                "10.770100",
                "106.705000",
                "10.770100",
                "106.690000",
                0,
                10);

        // Act
        SharedRouteSearchPage page = sut.search(criteria);

        // Assert
        assertThat(page.rows()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void givenPickupAtDriverDestination_whenSearching_thenZeroLengthCandidateIsExcluded() {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
        SharedRouteSearchCriteria criteria = criteria(
                scenario,
                "10.770000",
                "106.720000",
                "10.770100",
                "106.720100",
                0,
                10);

        // Act
        SharedRouteSearchPage page = sut.search(criteria);

        // Assert
        assertThat(page.rows()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void givenOwnRoute_whenSearching_thenRouteIsExcluded() {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
        fixture.makeRouteOwnedByActor(scenario, NOW);
        SharedRouteSearchCriteria criteria = criteria(
                scenario,
                "10.770100",
                "106.690000",
                "10.770100",
                "106.720100",
                0,
                10);

        // Act
        SharedRouteSearchPage page = sut.search(criteria);

        // Assert
        assertThat(page.rows()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @ParameterizedTest(name = "mutation={0}")
    @EnumSource(IneligibleMutation.class)
    void givenIneligibleCandidateState_whenSearching_thenRouteIsExcluded(
            IneligibleMutation mutation) {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
        fixture.applyIneligibleMutation(scenario, mutation, NOW);
        SharedRouteSearchCriteria criteria = criteria(
                scenario,
                "10.770100",
                "106.690000",
                "10.770100",
                "106.720100",
                0,
                10);

        // Act
        SharedRouteSearchPage page = sut.search(criteria);

        // Assert
        assertThat(page.rows()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void givenPickupOutsideConfiguredRadius_whenSearching_thenRouteIsExcluded() {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
        SharedRouteSearchCriteria criteria = criteria(
                scenario,
                "10.780000",
                "106.690000",
                "10.770100",
                "106.720100",
                0,
                10);

        // Act
        SharedRouteSearchPage page = sut.search(criteria);

        // Assert
        assertThat(page.rows()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void givenDestinationFarFromDriverDestinationAndRoute_whenSearching_thenRouteIsExcluded() {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
        SharedRouteSearchCriteria criteria = criteria(
                scenario,
                "10.770100",
                "106.690000",
                "10.790000",
                "106.705000",
                0,
                10);

        // Act
        SharedRouteSearchPage page = sut.search(criteria);

        // Assert
        assertThat(page.rows()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void givenTimeWindowCrossingMidnightAndActorExpiredBeforeRouteDate_whenSearching_thenRouteIsExcluded() {
        // Arrange
        Instant currentTime = Instant.parse("2026-08-03T15:30:00Z");
        Instant desiredDeparture = Instant.parse("2026-08-03T16:50:00Z");
        Instant routeDeparture = Instant.parse("2026-08-03T17:10:00Z");
        Scenario scenario = fixture.createStandardScenario(currentTime, routeDeparture);
        fixture.expireActorBeforeRouteDate(scenario);

        LocalDate requestedTravelDate = LocalDate.of(2026, 8, 3);
        SharedRouteSearchContext context = sut.findSearchContext(
                        scenario.actorId(),
                        scenario.schoolId(),
                        requestedTravelDate)
                .orElseThrow();

        SharedRouteSearchCriteria criteria = new SharedRouteSearchCriteria(
                scenario.actorId(),
                scenario.schoolId(),
                new BigDecimal("10.770100"),
                new BigDecimal("106.690000"),
                new BigDecimal("10.770100"),
                new BigDecimal("106.720100"),
                currentTime,
                requestedTravelDate,
                desiredDeparture.minusSeconds(1800),
                desiredDeparture.plusSeconds(1800),
                context,
                0,
                10);

        // Act
        SharedRouteSearchPage page = sut.search(criteria);

        // Assert
        assertThat(page.rows()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void givenThreeEquivalentRoutes_whenReadingTwoPages_thenOrderIsStableAndRoutesAreNotDuplicated() {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
        Long secondRouteId = fixture.createAdditionalEquivalentRoute(scenario, DEPARTURE);
        Long thirdRouteId = fixture.createAdditionalEquivalentRoute(scenario, DEPARTURE);

        SharedRouteSearchCriteria firstPageCriteria = criteria(
                scenario,
                "10.770100",
                "106.690000",
                "10.770100",
                "106.720100",
                0,
                2);
        SharedRouteSearchCriteria secondPageCriteria = criteria(
                scenario,
                "10.770100",
                "106.690000",
                "10.770100",
                "106.720100",
                1,
                2);

        // Act
        SharedRouteSearchPage firstPage = sut.search(firstPageCriteria);
        SharedRouteSearchPage secondPage = sut.search(secondPageCriteria);

        // Assert
        assertThat(firstPage.totalElements()).isEqualTo(3L);
        assertThat(secondPage.totalElements()).isEqualTo(3L);
        assertThat(firstPage.rows()).hasSize(2);
        assertThat(secondPage.rows()).hasSize(1);

        List<Long> actualIds = new ArrayList<>();
        firstPage.rows().forEach(row -> actualIds.add(row.sharedRouteId()));
        secondPage.rows().forEach(row -> actualIds.add(row.sharedRouteId()));

        List<Long> expectedIds = new ArrayList<>(List.of(
                scenario.routeId(),
                secondRouteId,
                thirdRouteId));
        expectedIds.sort(Comparator.naturalOrder());

        assertThat(actualIds).containsExactlyElementsOf(expectedIds);
        assertThat(actualIds).doesNotHaveDuplicates();
    }

    @Test
    void givenSearchOperation_whenCompleted_thenSeatsAndRideRequestsRemainUnchanged() {
        // Arrange
        Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
        int seatsBefore = fixture.remainingSeats(scenario.routeId());
        long rideRequestsBefore = fixture.countRideRequests();
        SharedRouteSearchCriteria criteria = criteria(
                scenario,
                "10.770100",
                "106.690000",
                "10.770100",
                "106.720100",
                0,
                10);

        // Act
        SharedRouteSearchPage page = sut.search(criteria);

        // Assert
        assertThat(page.rows()).hasSize(1);
        assertThat(fixture.remainingSeats(scenario.routeId()))
                .isEqualTo(seatsBefore);
        assertThat(fixture.countRideRequests())
                .isEqualTo(rideRequestsBefore);
    }

    private SharedRouteSearchCriteria criteria(
            Scenario scenario,
            String pickupLatitude,
            String pickupLongitude,
            String destinationLatitude,
            String destinationLongitude,
            int page,
            int size) {
        SharedRouteSearchContext context = sut.findSearchContext(
                        scenario.actorId(),
                        scenario.schoolId(),
                        scenario.travelDate())
                .orElseThrow();

        return new SharedRouteSearchCriteria(
                scenario.actorId(),
                scenario.schoolId(),
                new BigDecimal(pickupLatitude),
                new BigDecimal(pickupLongitude),
                new BigDecimal(destinationLatitude),
                new BigDecimal(destinationLongitude),
                NOW,
                scenario.travelDate(),
                DEPARTURE.minusSeconds(1800),
                DEPARTURE.plusSeconds(1800),
                context,
                page,
                size);
    }
}
