package com.zanh.route_sharing.testsupport.riderequest;

import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;

import java.math.BigDecimal;

public final class CreateRideRequestRequestBuilder {

    private Long schoolId = 1L;
    private RouteEndpointRequest pickup = point("10.776530", "106.700981", "Điểm đón");
    private RouteEndpointRequest destination = point("10.782120", "106.712450", "Điểm đến");
    private BigDecimal proposedSupportAmount = new BigDecimal("25000.00");
    private String note = "Tôi đứng tại cổng chính";

    public CreateRideRequestRequestBuilder withSchoolId(Long value) {
        schoolId = value;
        return this;
    }

    public CreateRideRequestRequestBuilder withPickup(RouteEndpointRequest value) {
        pickup = value;
        return this;
    }

    public CreateRideRequestRequestBuilder withDestination(RouteEndpointRequest value) {
        destination = value;
        return this;
    }

    public CreateRideRequestRequestBuilder withProposedSupportAmount(BigDecimal value) {
        proposedSupportAmount = value;
        return this;
    }

    public CreateRideRequestRequestBuilder withNote(String value) {
        note = value;
        return this;
    }

    public CreateRideRequestRequest build() {
        return new CreateRideRequestRequest(
                schoolId,
                pickup,
                destination,
                proposedSupportAmount,
                note);
    }

    public static RouteEndpointRequest point(
            String latitude,
            String longitude,
            String address) {
        return new RouteEndpointRequest(
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                address);
    }
}
