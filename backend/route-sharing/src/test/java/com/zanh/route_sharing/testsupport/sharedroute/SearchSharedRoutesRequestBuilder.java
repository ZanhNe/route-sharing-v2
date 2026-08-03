package com.zanh.route_sharing.testsupport.sharedroute;

import com.zanh.route_sharing.dto.sharedroute.search.SearchPointRequest;
import com.zanh.route_sharing.dto.sharedroute.search.SearchSharedRoutesRequest;

import java.time.Instant;

import static com.zanh.route_sharing.testsupport.sharedroute.SearchPointRequestBuilder.aSearchPoint;

public final class SearchSharedRoutesRequestBuilder {

    private Long schoolId = 3L;
    private SearchPointRequest pickup = aSearchPoint().build();
    private SearchPointRequest destination = aSearchPoint()
            .withLatitude("10.800000")
            .withLongitude("106.720000")
            .withAddress("Đích hành khách")
            .build();
    private Instant desiredDepartureTime = Instant.parse("2026-08-03T04:00:00Z");

    private SearchSharedRoutesRequestBuilder() {
    }

    public static SearchSharedRoutesRequestBuilder aSearchRequest() {
        return new SearchSharedRoutesRequestBuilder();
    }

    public SearchSharedRoutesRequestBuilder withSchoolId(Long schoolId) {
        this.schoolId = schoolId;
        return this;
    }

    public SearchSharedRoutesRequestBuilder withPickup(SearchPointRequest pickup) {
        this.pickup = pickup;
        return this;
    }

    public SearchSharedRoutesRequestBuilder withDestination(SearchPointRequest destination) {
        this.destination = destination;
        return this;
    }

    public SearchSharedRoutesRequestBuilder withDesiredDepartureTime(Instant desiredDepartureTime) {
        this.desiredDepartureTime = desiredDepartureTime;
        return this;
    }

    public SearchSharedRoutesRequest build() {
        return new SearchSharedRoutesRequest(
                schoolId,
                pickup,
                destination,
                desiredDepartureTime);
    }
}
