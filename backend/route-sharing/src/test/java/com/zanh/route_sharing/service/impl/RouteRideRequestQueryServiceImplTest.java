package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.RouteRideRequestQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestDetailLookup;
import com.zanh.route_sharing.service.riderequest.query.RouteRideRequestResponseMapper;
import com.zanh.route_sharing.testsupport.riderequest.query.RouteRideRequestQueryMother;
import com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteRideRequestQueryServiceImplTest {

    @Mock
    private RouteRideRequestQueryRepository repository;

    private RouteRideRequestQueryServiceImpl sut;

    @BeforeEach
    void setUp() {
        RouteRideRequestResponseMapper mapper = new RouteRideRequestResponseMapper(
                new RouteGeoJsonWriter(JsonMapper.builder().build()));
        sut = new RouteRideRequestQueryServiceImpl(
                repository,
                mapper,
                Clock.fixed(RouteRideRequestQueryMother.READ_AT, ZoneOffset.UTC));
    }

    @Test
    void givenOwnedRouteWithPendingRequest_whenListing_thenPendingPageAndStableMetaAreReturned() {
        when(repository.findPendingPage(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                0,
                10)).thenReturn(Optional.of(RouteRideRequestQueryMother.page()));

        var result = sut.listPending(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                0,
                10);

        assertThat(result.data().items()).hasSize(1);
        assertThat(result.data().items().get(0).status().name()).isEqualTo("PENDING");
        assertThat(result.meta().totalElements()).isEqualTo(1L);
        verify(repository).findPendingPage(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                0,
                10);
    }

    @Test
    void givenMissingOrNonOwnedRoute_whenListing_thenSharedRouteNotFoundIsReturned() {
        when(repository.findPendingPage(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                0,
                10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.listPending(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                0,
                10)).isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("SHARED_ROUTE_NOT_FOUND");
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                });
    }

    @Test
    void givenOwnedPendingRequest_whenGettingDetail_thenStoredSnapshotIsReturnedWithoutRecalculation() {
        when(repository.findPendingDetail(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                RouteRideRequestQueryMother.REQUEST_ID))
                .thenReturn(RouteRideRequestQueryMother.detailLookup());

        var result = sut.getPendingDetail(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                RouteRideRequestQueryMother.REQUEST_ID);

        assertThat(result.passenger().passengerId()).isEqualTo(7L);
        assertThat(result.request().status().name()).isEqualTo("PENDING");
        assertThat(result.map().passengerDesiredRoute().geoJson().coordinates()).hasSize(3);
        assertThat(result.map().servedSegment().geoJson().coordinates()).hasSize(2);
        assertThat(result.map().markers()).hasSize(5);
    }

    @Test
    void givenMissingOrNonOwnedRoute_whenGettingDetail_thenSharedRouteNotFoundIsReturned() {
        when(repository.findPendingDetail(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                RouteRideRequestQueryMother.REQUEST_ID))
                .thenReturn(PendingRideRequestDetailLookup.routeNotFound());

        assertThatThrownBy(() -> sut.getPendingDetail(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                RouteRideRequestQueryMother.REQUEST_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("SHARED_ROUTE_NOT_FOUND"));
    }

    @Test
    void givenRequestNoLongerPending_whenGettingDetail_thenRideRequestNotFoundIsReturned() {
        when(repository.findPendingDetail(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                RouteRideRequestQueryMother.REQUEST_ID))
                .thenReturn(PendingRideRequestDetailLookup.requestNotFound(
                        RouteRideRequestQueryMother.route()));

        assertThatThrownBy(() -> sut.getPendingDetail(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                RouteRideRequestQueryMother.REQUEST_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("RIDE_REQUEST_NOT_FOUND"));
    }

    @Test
    void givenInvalidPageOrSize_whenListing_thenInvalidQueryIsReturnedBeforeRepositoryCall() {
        assertThatThrownBy(() -> sut.listPending(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                -1,
                10)).isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_RIDE_REQUEST_QUERY"));

        assertThatThrownBy(() -> sut.listPending(
                RouteRideRequestQueryMother.ACTOR_ID,
                RouteRideRequestQueryMother.ROUTE_ID,
                0,
                51)).isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_RIDE_REQUEST_QUERY"));
        verifyNoInteractions(repository);
    }
}
