package com.zanh.route_sharing.integration.goong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoongDirectionsResponse(
        @JsonProperty("geocoded_waypoints") List<GeocodedWaypointDto> geocodedWaypoints,
        List<RouteDto> routes) {

    public GoongDirectionsResponse(List<RouteDto> routes) {
        this(List.of(), routes);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RouteDto(
            BoundsDto bounds,
            List<LegDto> legs,
            @JsonProperty("overview_polyline") OverviewPolylineDto overviewPolyline,
            List<String> warnings,
            @JsonProperty("waypoint_order") List<Integer> waypointOrder) {

        public RouteDto(
                List<LegDto> legs,
                OverviewPolylineDto overviewPolyline) {
            this(null, legs, overviewPolyline, List.of(), List.of());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LegDto(
            ValueDto distance,
            ValueDto duration,
            List<StepDto> steps) {

        public LegDto(ValueDto distance, ValueDto duration) {
            this(distance, duration, List.of());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValueDto(String text, Long value) {
        public ValueDto(Long value) {
            this(null, value);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OverviewPolylineDto(String points) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BoundsDto(CoordinateDto northeast, CoordinateDto southwest) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CoordinateDto(Double lat, Double lng) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodedWaypointDto(
            @JsonProperty("geocoder_status") String geocoderStatus,
            @JsonProperty("place_id") String placeId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StepDto(
            ValueDto distance,
            ValueDto duration,
            @JsonProperty("html_instructions") String htmlInstructions) {
    }
}
