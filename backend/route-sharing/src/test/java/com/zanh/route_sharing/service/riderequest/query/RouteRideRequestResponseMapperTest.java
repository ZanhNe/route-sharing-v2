package com.zanh.route_sharing.service.riderequest.query;

import com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestDetailResponse.StoredRouteMeaning;
import com.zanh.route_sharing.testsupport.riderequest.query.RouteRideRequestQueryMother;
import com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RouteRideRequestResponseMapperTest {

    private RouteRideRequestResponseMapper sut;

    @BeforeEach
    void setUp() {
        sut = new RouteRideRequestResponseMapper(
                new RouteGeoJsonWriter(JsonMapper.builder().build()));
    }

    @Test
    void givenPendingSnapshot_whenMappingPage_thenStatusAndBusinessDataArePreservedWithoutExpiryField() {
        var result = sut.toPage(
                RouteRideRequestQueryMother.page(),
                RouteRideRequestQueryMother.READ_AT);

        assertThat(result.data().items()).hasSize(1);
        assertThat(result.data().items().get(0).status().name()).isEqualTo("PENDING");
        assertThat(result.data().items().get(0).proposedSupportAmount())
                .isEqualByComparingTo("25000.00");
    }

    @Test
    void givenStoredSnapshot_whenMappingDetail_thenThreeRouteMeaningsAndLongitudeLatitudeAxesArePreserved() {
        var result = sut.toDetail(
                RouteRideRequestQueryMother.detailLookup(),
                Instant.parse("2026-08-06T00:10:00Z"));

        assertThat(result.map().originalDriverRoute().meaning())
                .isEqualTo(StoredRouteMeaning.DRIVER_ORIGINAL_ROUTE);
        assertThat(result.map().passengerDesiredRoute().meaning())
                .isEqualTo(StoredRouteMeaning.PASSENGER_DESIRED_ROUTE_VIA_DROPOFF);
        assertThat(result.map().servedSegment().meaning())
                .isEqualTo(StoredRouteMeaning.PASSENGER_SERVED_SEGMENT);
        assertThat(result.map().servedSegment().geoJson().coordinates().get(0))
                .containsExactly(
                        new java.math.BigDecimal("106.700981"),
                        new java.math.BigDecimal("10.776530"));
        assertThat(result.request().pickup().latitude())
                .isEqualByComparingTo("10.776530");
        assertThat(result.request().pickup().longitude())
                .isEqualByComparingTo("106.700981");
    }

    @Test
    void givenSameDestinationSnapshot_whenMappingDetail_thenDropoffEqualsDestinationAndRemainingDistanceIsZero() {
        var result = sut.toDetail(
                RouteRideRequestQueryMother.sameDestinationDetailLookup(),
                RouteRideRequestQueryMother.READ_AT);

        assertThat(result.request().matchType().name()).isEqualTo("CUNG_DIEM_DEN");
        assertThat(result.request().proposedDropoff())
                .isEqualTo(result.request().passengerDestination());
        assertThat(result.request().remainingDistanceMeters())
                .isEqualByComparingTo("0.00");
        assertThat(result.map().servedSegment().geoJson())
                .isEqualTo(result.map().passengerDesiredRoute().geoJson());
    }

}
