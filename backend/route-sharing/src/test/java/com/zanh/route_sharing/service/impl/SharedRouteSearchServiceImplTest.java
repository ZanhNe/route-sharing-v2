package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.dto.sharedroute.search.SearchPointRequest;
import com.zanh.route_sharing.dto.sharedroute.search.SearchSharedRoutesRequest;
import com.zanh.route_sharing.dto.sharedroute.search.SharedRouteSearchResult;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchCriteria;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchPage;
import com.zanh.route_sharing.testsupport.sharedroute.RecordingSharedRouteSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static com.zanh.route_sharing.testsupport.sharedroute.SearchPointRequestBuilder.aSearchPoint;
import static com.zanh.route_sharing.testsupport.sharedroute.SearchSharedRoutesRequestBuilder.aSearchRequest;
import static com.zanh.route_sharing.testsupport.sharedroute.SharedRouteSearchItemAssert.assertThatSearchItem;
import static com.zanh.route_sharing.testsupport.sharedroute.SharedRouteSearchRowBuilder.aSearchRow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedRouteSearchServiceImplTest {

        private static final Instant NOW = Instant.parse("2026-08-03T03:00:00Z");
        private static final Long ACTOR_ID = 7L;

        @Test
        void givenEligibleActorAndSegmentMatch_whenSearching_thenCompleteCriteriaAndResponseAreProduced() {
                // Arrange
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository()
                                .withPage(new SharedRouteSearchPage(
                                                List.of(aSearchRow().build()),
                                                1L));
                SharedRouteSearchServiceImpl sut = service(repository, NOW);
                SearchSharedRoutesRequest request = aSearchRequest().build();

                // Act
                SharedRouteSearchResult result = sut.search(ACTOR_ID, request, 0, 10);

                // Assert
                SharedRouteSearchCriteria criteria = repository.lastCriteria();
                assertThat(repository.contextQueryCount()).isEqualTo(1);
                assertThat(repository.searchQueryCount()).isEqualTo(1);
                assertThat(repository.lastActorUserId()).isEqualTo(ACTOR_ID);
                assertThat(repository.lastSchoolId()).isEqualTo(request.schoolId());
                assertThat(repository.lastRequestedTravelDate())
                                .isEqualTo(LocalDate.of(2026, 8, 3));

                assertThat(criteria.actorUserId()).isEqualTo(ACTOR_ID);
                assertThat(criteria.schoolId()).isEqualTo(request.schoolId());
                assertThat(criteria.pickupLatitude())
                                .isEqualByComparingTo(request.pickup().latitude());
                assertThat(criteria.pickupLongitude())
                                .isEqualByComparingTo(request.pickup().longitude());
                assertThat(criteria.destinationLatitude())
                                .isEqualByComparingTo(request.destination().latitude());
                assertThat(criteria.destinationLongitude())
                                .isEqualByComparingTo(request.destination().longitude());
                assertThat(criteria.now()).isEqualTo(NOW);
                assertThat(criteria.requestedTravelDate())
                                .isEqualTo(LocalDate.of(2026, 8, 3));
                assertThat(criteria.departureFrom())
                                .isEqualTo(Instant.parse("2026-08-03T03:30:00Z"));
                assertThat(criteria.departureTo())
                                .isEqualTo(Instant.parse("2026-08-03T04:30:00Z"));
                assertThat(criteria.page()).isZero();
                assertThat(criteria.size()).isEqualTo(10);

                assertThat(result.items()).hasSize(1);
                assertThatSearchItem(result.items().get(0))
                                .hasMatchType(LoaiGhepTuyen.TRUNG_DOAN_TUYEN)
                                .hasDropoffType(LoaiDiemTha.DIEM_THA_TRUNG_GIAN)
                                .hasProposedDropoffWithoutAddress()
                                .hasPickupDeviation("25.50")
                                .hasDestinationDeviation("80.00")
                                .hasSharedSegment("5000.00");
                assertThat(result.meta().totalElements()).isEqualTo(1L);
        }

        @Test
        void givenSameDestinationMatch_whenSearching_thenPassengerDestinationAddressIsProposed() {
                // Arrange
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository()
                                .withPage(new SharedRouteSearchPage(
                                                List.of(aSearchRow()
                                                                .withMatchType(LoaiGhepTuyen.CUNG_DIEM_DEN)
                                                                .withDropoffType(LoaiDiemTha.DIEM_DICH_CUOI_CUNG)
                                                                .build()),
                                                1L));
                SharedRouteSearchServiceImpl sut = service(repository, NOW);
                SearchSharedRoutesRequest request = aSearchRequest().build();

                // Act
                var item = sut.search(ACTOR_ID, request, 0, 10).items().get(0);

                // Assert
                assertThatSearchItem(item)
                                .hasMatchType(LoaiGhepTuyen.CUNG_DIEM_DEN)
                                .hasDropoffType(LoaiDiemTha.DIEM_DICH_CUOI_CUNG)
                                .hasProposedDropoffAddress("Đích hành khách")
                                .hasDestinationDeviation("80.00");
        }

        @Test
        void givenDepartureWindowOverlappingNow_whenSearching_thenLowerBoundIsClampedToNow() {
                // Arrange
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository();
                SharedRouteSearchServiceImpl sut = service(repository, NOW);
                SearchSharedRoutesRequest request = aSearchRequest()
                                .withDesiredDepartureTime(Instant.parse("2026-08-03T03:10:00Z"))
                                .build();

                // Act
                sut.search(ACTOR_ID, request, 0, 10);

                // Assert
                assertThat(repository.lastCriteria().departureFrom()).isEqualTo(NOW);
                assertThat(repository.lastCriteria().departureTo())
                                .isEqualTo(Instant.parse("2026-08-03T03:40:00Z"));
        }

        @Test
        void givenDesiredDepartureOnNextVietnamDate_whenSearching_thenTravelDateUsesDesiredDeparture() {
                // Arrange
                Instant currentTime = Instant.parse("2026-08-03T15:30:00Z");
                Instant desiredDeparture = Instant.parse("2026-08-03T18:30:00Z");
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository();
                SharedRouteSearchServiceImpl sut = service(repository, currentTime);
                SearchSharedRoutesRequest request = aSearchRequest()
                                .withDesiredDepartureTime(desiredDeparture)
                                .build();

                // Act
                sut.search(ACTOR_ID, request, 0, 10);

                // Assert
                assertThat(repository.lastRequestedTravelDate())
                                .isEqualTo(LocalDate.of(2026, 8, 4));
                assertThat(repository.lastCriteria().requestedTravelDate())
                                .isEqualTo(LocalDate.of(2026, 8, 4));
        }

        @Test
        void givenDesiredDepartureNotAfterNow_whenSearching_thenDepartureErrorIsReturnedWithoutRepositoryCall() {
                // Arrange
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository();
                SharedRouteSearchServiceImpl sut = service(repository, NOW);
                SearchSharedRoutesRequest request = aSearchRequest()
                                .withDesiredDepartureTime(NOW)
                                .build();

                // Act & Assert
                assertBusinessCode(
                                () -> sut.search(ACTOR_ID, request, 0, 10),
                                "DESIRED_DEPARTURE_NOT_IN_FUTURE");
                assertThat(repository.contextQueryCount()).isZero();
                assertThat(repository.searchQueryCount()).isZero();
        }

        @Test
        void givenIdenticalPickupAndDestination_whenSearching_thenEndpointErrorIsReturnedWithoutRepositoryCall() {
                // Arrange
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository();
                SharedRouteSearchServiceImpl sut = service(repository, NOW);
                SearchPointRequest samePoint = aSearchPoint()
                                .withAddress("Cùng điểm")
                                .build();
                SearchSharedRoutesRequest request = aSearchRequest()
                                .withPickup(samePoint)
                                .withDestination(samePoint)
                                .build();

                // Act & Assert
                assertBusinessCode(
                                () -> sut.search(ACTOR_ID, request, 0, 10),
                                "INVALID_SEARCH_ENDPOINTS");
                assertThat(repository.contextQueryCount()).isZero();
                assertThat(repository.searchQueryCount()).isZero();
        }

        @Test
        void givenActorWithoutEligibleMembership_whenSearching_thenEligibilityErrorIsReturned() {
                // Arrange
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository()
                                .withoutEligibleContext();
                SharedRouteSearchServiceImpl sut = service(repository, NOW);

                // Act & Assert
                assertBusinessCode(
                                () -> sut.search(ACTOR_ID, aSearchRequest().build(), 0, 10),
                                "SHARED_ROUTE_SEARCH_NOT_ELIGIBLE");
                assertThat(repository.contextQueryCount()).isEqualTo(1);
                assertThat(repository.searchQueryCount()).isZero();
        }

        @Test
        void givenEmptyRepositoryPage_whenSearching_thenEmptyItemsAndCorrectMetaAreReturned() {
                // Arrange
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository()
                                .withPage(new SharedRouteSearchPage(List.of(), 0L));
                SharedRouteSearchServiceImpl sut = service(repository, NOW);

                // Act
                SharedRouteSearchResult result = sut.search(
                                ACTOR_ID,
                                aSearchRequest().build(),
                                2,
                                10);

                // Assert
                assertThat(result.items()).isEmpty();
                assertThat(result.meta().page()).isEqualTo(2);
                assertThat(result.meta().size()).isEqualTo(10);
                assertThat(result.meta().totalElements()).isZero();
        }

        @ParameterizedTest(name = "actorUserId={0}")
        @MethodSource("invalidActorIds")
        void givenInvalidActorId_whenSearching_thenAuthenticatedUserErrorIsReturned(
                        Long actorUserId) {
                // Arrange
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository();
                SharedRouteSearchServiceImpl sut = service(repository, NOW);

                // Act & Assert
                assertBusinessCode(
                                () -> sut.search(actorUserId, aSearchRequest().build(), 0, 10),
                                "AUTHENTICATED_USER_REQUIRED");
                assertThat(repository.contextQueryCount()).isZero();
        }

        @ParameterizedTest(name = "schoolId={0}")
        @MethodSource("invalidSchoolIds")
        void givenNonPositiveSchoolId_whenSearching_thenInvalidCriteriaIsReturnedWithoutRepositoryCall(
                        Long schoolId) {
                // Arrange
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository();
                SharedRouteSearchServiceImpl sut = service(repository, NOW);
                SearchSharedRoutesRequest request = aSearchRequest()
                                .withSchoolId(schoolId)
                                .build();

                // Act & Assert
                assertBusinessCode(
                                () -> sut.search(ACTOR_ID, request, 0, 10),
                                "INVALID_SEARCH_CRITERIA");
                assertThat(repository.contextQueryCount()).isZero();
                assertThat(repository.searchQueryCount()).isZero();
        }

        @ParameterizedTest(name = "page={0}, size={1}, code={2}")
        @MethodSource("invalidPagingCases")
        void givenInvalidPaging_whenSearching_thenExpectedPagingErrorIsReturned(
                        int page,
                        int size,
                        String expectedCode) {
                // Arrange
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository();
                SharedRouteSearchServiceImpl sut = service(repository, NOW);

                // Act & Assert
                assertBusinessCode(
                                () -> sut.search(ACTOR_ID, aSearchRequest().build(), page, size),
                                expectedCode);
                assertThat(repository.contextQueryCount()).isZero();
        }

        @ParameterizedTest(name = "invalid request #{index}")
        @MethodSource("incompleteRequests")
        void givenIncompleteCriteria_whenSearching_thenInvalidCriteriaErrorIsReturned(
                        SearchSharedRoutesRequest request) {
                // Arrange
                RecordingSharedRouteSearchRepository repository = new RecordingSharedRouteSearchRepository();
                SharedRouteSearchServiceImpl sut = service(repository, NOW);

                // Act & Assert
                assertBusinessCode(
                                () -> sut.search(ACTOR_ID, request, 0, 10),
                                "INVALID_SEARCH_CRITERIA");
                assertThat(repository.contextQueryCount()).isZero();
        }

        private static SharedRouteSearchServiceImpl service(
                        RecordingSharedRouteSearchRepository repository,
                        Instant now) {
                return new SharedRouteSearchServiceImpl(
                                repository,
                                Clock.fixed(now, ZoneOffset.UTC));
        }

        private static void assertBusinessCode(
                        Runnable operation,
                        String expectedCode) {
                assertThatThrownBy(operation::run)
                                .isInstanceOf(BusinessException.class)
                                .satisfies(exception -> assertThat(
                                                ((BusinessException) exception).getCode())
                                                .isEqualTo(expectedCode));
        }

        private static Stream<Long> invalidActorIds() {
                return Stream.of(null, 0L, -1L);
        }

        private static Stream<Long> invalidSchoolIds() {
                return Stream.of(0L, -1L);
        }

        private static Stream<Arguments> invalidPagingCases() {
                return Stream.of(
                                Arguments.of(-1, 10, "INVALID_PAGE"),
                                Arguments.of(0, 0, "INVALID_PAGE_SIZE"),
                                Arguments.of(0, 51, "INVALID_PAGE_SIZE"));
        }

        private static Stream<SearchSharedRoutesRequest> incompleteRequests() {
                return Stream.of(
                                null,
                                aSearchRequest().withSchoolId(null).build(),
                                aSearchRequest().withPickup(null).build(),
                                aSearchRequest().withDestination(null).build(),
                                aSearchRequest().withDesiredDepartureTime(null).build());
        }
}
