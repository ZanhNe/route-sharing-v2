package com.zanh.route_sharing.integration.goong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoongDirectionsResponse(List<RouteDto> routes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RouteDto(
            List<LegDto> legs,
            @JsonProperty("overview_polyline") OverviewPolylineDto overviewPolyline) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LegDto(ValueDto distance, ValueDto duration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValueDto(Long value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OverviewPolylineDto(String points) {
    }
}
