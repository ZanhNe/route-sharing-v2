package com.zanh.route_sharing.testfixture;

import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointRequest;

import java.math.BigDecimal;
import java.time.Instant;

public final class CreateSharedRouteRequestTestBuilder {

    private RouteEndpointRequest origin = new RouteEndpointRequest(
            new BigDecimal("10.762622"),
            new BigDecimal("106.660172"),
            "Điểm A");

    private RouteEndpointRequest destination = new RouteEndpointRequest(
            new BigDecimal("10.823099"),
            new BigDecimal("106.629664"),
            "Điểm B");

    private Instant expectedDepartureTime = Instant.parse("2026-08-01T09:00:00Z");

    private Long vehicleId = 20L;
    private Integer offeredSeats = 1;
    private BigDecimal suggestedSupportPerKm = new BigDecimal("3000");

    private CreateSharedRouteRequestTestBuilder() {
    }

    public static CreateSharedRouteRequestTestBuilder aValidRequest() {
        return new CreateSharedRouteRequestTestBuilder();
    }

    public CreateSharedRouteRequestTestBuilder withOrigin(
            RouteEndpointRequest origin) {
        this.origin = origin;
        return this;
    }

    public CreateSharedRouteRequestTestBuilder withDestination(
            RouteEndpointRequest destination) {
        this.destination = destination;
        return this;
    }

    public CreateSharedRouteRequestTestBuilder withDepartureTime(
            Instant expectedDepartureTime) {
        this.expectedDepartureTime = expectedDepartureTime;
        return this;
    }

    public CreateSharedRouteRequestTestBuilder withVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
        return this;
    }

    public CreateSharedRouteRequestTestBuilder withOfferedSeats(
            Integer offeredSeats) {
        this.offeredSeats = offeredSeats;
        return this;
    }

    public CreateSharedRouteRequestTestBuilder withSuggestedSupportPerKm(
            BigDecimal suggestedSupportPerKm) {
        this.suggestedSupportPerKm = suggestedSupportPerKm;
        return this;
    }

    public CreateSharedRouteRequest build() {
        return new CreateSharedRouteRequest(
                origin,
                destination,
                expectedDepartureTime,
                vehicleId,
                offeredSeats,
                suggestedSupportPerKm);
    }
}
