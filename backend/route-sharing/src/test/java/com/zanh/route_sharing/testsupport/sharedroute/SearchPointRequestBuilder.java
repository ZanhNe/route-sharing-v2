package com.zanh.route_sharing.testsupport.sharedroute;

import com.zanh.route_sharing.dto.sharedroute.search.SearchPointRequest;

import java.math.BigDecimal;

public final class SearchPointRequestBuilder {

    private BigDecimal latitude = new BigDecimal("10.770000");
    private BigDecimal longitude = new BigDecimal("106.690000");
    private String address = "Điểm đón";

    private SearchPointRequestBuilder() {
    }

    public static SearchPointRequestBuilder aSearchPoint() {
        return new SearchPointRequestBuilder();
    }

    public SearchPointRequestBuilder withLatitude(String latitude) {
        this.latitude = latitude == null ? null : new BigDecimal(latitude);
        return this;
    }

    public SearchPointRequestBuilder withLongitude(String longitude) {
        this.longitude = longitude == null ? null : new BigDecimal(longitude);
        return this;
    }

    public SearchPointRequestBuilder withAddress(String address) {
        this.address = address;
        return this;
    }

    public SearchPointRequest build() {
        return new SearchPointRequest(latitude, longitude, address);
    }
}
