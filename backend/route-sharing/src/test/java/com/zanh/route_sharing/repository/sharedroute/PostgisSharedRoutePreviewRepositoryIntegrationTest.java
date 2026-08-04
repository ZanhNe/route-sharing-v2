package com.zanh.route_sharing.repository.sharedroute;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.repository.sharedroute.preview.SharedRoutePreviewRepository;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewEvaluation;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewEvaluationStatus;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewCriteria;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewPreparation;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture.IneligibleMutation;
import com.zanh.route_sharing.testsupport.sharedroute.integration.SharedRouteSearchDatabaseFixture.Scenario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostgisSharedRoutePreviewRepositoryIntegrationTest {

        private static final Instant NOW = Instant.parse("2026-08-03T03:00:00Z");
        private static final Instant DEPARTURE = Instant.parse("2026-08-03T04:00:00Z");

        @Autowired
        private SharedRoutePreviewRepository sut;

        @Autowired
        private EntityManager entityManager;

        private SharedRouteSearchDatabaseFixture fixture;

        @BeforeEach
        void setUp() {
                fixture = new SharedRouteSearchDatabaseFixture(entityManager);
        }

        @Test
        void givenSelectedRouteAndSameDestination_whenEvaluating_thenCurrentPreparationIsReturned() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);

                // Act
                PreviewEvaluation evaluation = sut.evaluate(sameDestinationCriteria(scenario));

                // Assert
                assertThat(evaluation.status()).isEqualTo(PreviewEvaluationStatus.ELIGIBLE);
                SharedRoutePreviewPreparation preparation = evaluation.requirePreparation();
                assertThat(preparation.route().routeId()).isEqualTo(scenario.routeId());
                assertThat(preparation.route().routeVersion()).isNotNull();
                assertThat(preparation.route().status()).isEqualTo(TrangThaiLoTrinh.OPEN);
                assertThat(preparation.route().remainingSeats()).isEqualTo(2);
                assertThat(preparation.route().originalRouteGeoJson()).contains("LineString");
                assertThat(preparation.match().matchType()).isEqualTo(LoaiGhepTuyen.CUNG_DIEM_DEN);
                assertThat(preparation.match().dropoffType()).isEqualTo(LoaiDiemTha.DIEM_DICH_CUOI_CUNG);
                assertThat(preparation.match().proposedDropoff().latitude())
                                .isCloseTo(new BigDecimal("10.770100"), within(new BigDecimal("0.000001")));
                assertThat(preparation.match().proposedDropoff().longitude())
                                .isCloseTo(new BigDecimal("106.720100"), within(new BigDecimal("0.000001")));
                assertThat(preparation.consistencyToken().schoolId()).isEqualTo(scenario.schoolId());
                assertThat(preparation.consistencyToken().sameDestinationRadiusMeters())
                                .isEqualByComparingTo("200.00");
        }

        @Test
        void givenSelectedRouteAndDestinationNearRoute_whenEvaluating_thenSegmentPreparationIsReturned() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);

                // Act
                PreviewEvaluation evaluation = sut.evaluate(segmentCriteria(scenario));

                // Assert
                assertThat(evaluation.status()).isEqualTo(PreviewEvaluationStatus.ELIGIBLE);
                SharedRoutePreviewPreparation preparation = evaluation.requirePreparation();
                assertThat(preparation.match().matchType()).isEqualTo(LoaiGhepTuyen.TRUNG_DOAN_TUYEN);
                assertThat(preparation.match().dropoffType()).isEqualTo(LoaiDiemTha.DIEM_THA_TRUNG_GIAN);
                assertThat(preparation.match().proposedDropoff().latitude())
                                .isCloseTo(new BigDecimal("10.770000"), within(new BigDecimal("0.000001")));
                assertThat(preparation.match().proposedDropoff().longitude())
                                .isCloseTo(new BigDecimal("106.705000"), within(new BigDecimal("0.000001")));
                assertThat(preparation.match().sharedSegmentMeters()).isPositive();
        }

        @Test
        void givenUnknownRoute_whenEvaluating_thenNotFoundOrInaccessibleIsReturned() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
                SharedRoutePreviewCriteria criteria = new SharedRoutePreviewCriteria(
                                scenario.actorId(),
                                scenario.schoolId(),
                                Long.MAX_VALUE,
                                new BigDecimal("10.770100"),
                                new BigDecimal("106.690000"),
                                new BigDecimal("10.770100"),
                                new BigDecimal("106.720100"),
                                NOW);

                // Act
                PreviewEvaluation evaluation = sut.evaluate(criteria);

                // Assert
                assertThat(evaluation.status())
                                .isEqualTo(PreviewEvaluationStatus.NOT_FOUND_OR_INACCESSIBLE);
                assertThat(evaluation.optionalPreparation()).isEmpty();
        }

        @Test
        void givenActorOwnsSelectedRoute_whenEvaluating_thenSelfRouteIsReturned() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
                fixture.makeRouteOwnedByActor(scenario, NOW);

                // Act
                PreviewEvaluation evaluation = sut.evaluate(sameDestinationCriteria(scenario));

                // Assert
                assertThat(evaluation.status()).isEqualTo(PreviewEvaluationStatus.SELF_ROUTE);
        }

        @Test
        void givenSelectedRouteHasNoRemainingSeat_whenEvaluating_thenUnavailableIsReturned() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
                fixture.applyIneligibleMutation(scenario, IneligibleMutation.NO_REMAINING_SEATS, NOW);

                // Act
                PreviewEvaluation evaluation = sut.evaluate(sameDestinationCriteria(scenario));

                // Assert
                assertThat(evaluation.status()).isEqualTo(PreviewEvaluationStatus.ROUTE_UNAVAILABLE);
        }

        @Test
        void givenDriverNoLongerBelongsToRequestedSchool_whenEvaluating_thenRouteIsHidden() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
                fixture.applyIneligibleMutation(
                                scenario,
                                IneligibleMutation.DRIVER_MEMBERSHIP_EXPIRED,
                                NOW);

                // Act
                PreviewEvaluation evaluation = sut.evaluate(sameDestinationCriteria(scenario));

                // Assert
                assertThat(evaluation.status())
                                .isEqualTo(PreviewEvaluationStatus.NOT_FOUND_OR_INACCESSIBLE);
        }

        @Test
        void givenDriverAccountBecomesInactive_whenEvaluating_thenUnavailableIsReturned() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
                fixture.applyIneligibleMutation(scenario, IneligibleMutation.DRIVER_INACTIVE, NOW);

                // Act
                PreviewEvaluation evaluation = sut.evaluate(sameDestinationCriteria(scenario));

                // Assert
                assertThat(evaluation.status())
                                .isEqualTo(PreviewEvaluationStatus.DRIVER_OR_VEHICLE_INELIGIBLE);
        }

        @Test
        void givenPickupOutsideConfiguredRadius_whenEvaluating_thenNoLongerMatchesIsReturned() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
                SharedRoutePreviewCriteria criteria = new SharedRoutePreviewCriteria(
                                scenario.actorId(),
                                scenario.schoolId(),
                                scenario.routeId(),
                                new BigDecimal("10.780000"),
                                new BigDecimal("106.690000"),
                                new BigDecimal("10.770100"),
                                new BigDecimal("106.720100"),
                                NOW);

                // Act
                PreviewEvaluation evaluation = sut.evaluate(criteria);

                // Assert
                assertThat(evaluation.status()).isEqualTo(PreviewEvaluationStatus.NO_LONGER_MATCHES);
        }

        @Test
        void givenUnchangedSnapshot_whenCheckingFreshness_thenTrueIsReturned() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
                SharedRoutePreviewPreparation preparation = sut.evaluate(sameDestinationCriteria(scenario))
                                .requirePreparation();

                // Act
                boolean current = sut.remainsCurrent(
                                preparation.consistencyToken(),
                                NOW.plusSeconds(5));

                // Assert
                assertThat(current).isTrue();
        }

        @Test
        void givenMatchingThresholdChangesWithoutVersionBump_whenCheckingFreshness_thenFalseIsReturned() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
                SharedRoutePreviewPreparation preparation = sut.evaluate(sameDestinationCriteria(scenario))
                                .requirePreparation();
                entityManager.createNativeQuery(
                                "UPDATE cau_hinh_nghiep_vu "
                                                + "SET ban_kinh_cung_diem_den_met = 201.00 "
                                                + "WHERE id = :configurationId")
                                .setParameter("configurationId", scenario.configurationId())
                                .executeUpdate();
                fixture.flushAndClear();

                // Act
                boolean current = sut.remainsCurrent(
                                preparation.consistencyToken(),
                                NOW.plusSeconds(5));

                // Assert
                assertThat(current).isFalse();
        }

        @Test
        void givenSeatChangedAfterPreparation_whenCheckingFreshness_thenFalseIsReturned() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
                SharedRoutePreviewPreparation preparation = sut.evaluate(sameDestinationCriteria(scenario))
                                .requirePreparation();
                fixture.applyIneligibleMutation(scenario, IneligibleMutation.NO_REMAINING_SEATS, NOW);

                // Act
                boolean current = sut.remainsCurrent(
                                preparation.consistencyToken(),
                                NOW.plusSeconds(5));

                // Assert
                assertThat(current).isFalse();
        }

        @Test
        void givenPreviewEvaluation_whenCompleted_thenSeatsAndRideRequestsAreUnchanged() {
                // Arrange
                Scenario scenario = fixture.createStandardScenario(NOW, DEPARTURE);
                int seatsBefore = fixture.remainingSeats(scenario.routeId());
                long requestsBefore = fixture.countRideRequests();

                // Act
                PreviewEvaluation first = sut.evaluate(sameDestinationCriteria(scenario));
                PreviewEvaluation second = sut.evaluate(segmentCriteria(scenario));

                // Assert
                assertThat(first.status()).isEqualTo(PreviewEvaluationStatus.ELIGIBLE);
                assertThat(second.status()).isEqualTo(PreviewEvaluationStatus.ELIGIBLE);
                assertThat(fixture.remainingSeats(scenario.routeId())).isEqualTo(seatsBefore);
                assertThat(fixture.countRideRequests()).isEqualTo(requestsBefore);
        }

        private static SharedRoutePreviewCriteria sameDestinationCriteria(Scenario scenario) {
                return criteria(scenario, "10.770100", "106.720100");
        }

        private static SharedRoutePreviewCriteria segmentCriteria(Scenario scenario) {
                return criteria(scenario, "10.770100", "106.705000");
        }

        private static SharedRoutePreviewCriteria criteria(
                        Scenario scenario,
                        String destinationLatitude,
                        String destinationLongitude) {
                return new SharedRoutePreviewCriteria(
                                scenario.actorId(),
                                scenario.schoolId(),
                                scenario.routeId(),
                                new BigDecimal("10.770100"),
                                new BigDecimal("106.690000"),
                                new BigDecimal(destinationLatitude),
                                new BigDecimal(destinationLongitude),
                                NOW);
        }
}
