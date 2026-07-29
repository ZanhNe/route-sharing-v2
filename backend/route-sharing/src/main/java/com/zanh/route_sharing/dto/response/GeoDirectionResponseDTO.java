package com.zanh.route_sharing.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoDirectionResponseDTO(
        @JsonProperty("routes") Route[] routes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(
            @JsonProperty("legs") Leg[] legs,
            @JsonProperty("bounds") Bounds bounds,
            @JsonAlias("overview_polyline") Polyline polyline) {

        public long getTotalDistanceMeters() {
            if (legs == null)
                return 0;
            return Arrays.stream(legs).mapToLong(leg -> leg.distance().value()).sum();
        }

        public long getTotalDurationSeconds() {
            if (legs == null)
                return 0;
            return Arrays.stream(legs).mapToLong(leg -> leg.duration().value()).sum();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Leg(
            @JsonProperty("distance") Measure distance,
            @JsonProperty("duration") Measure duration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Measure(
            @JsonProperty("value") Long value,
            @JsonProperty("text") String text) {
    }

    public record Polyline(
            @JsonProperty("points") String points) {
    }

    public record Bounds(
            @JsonProperty("northeast") Location northeast,
            @JsonProperty("southwest") Location southwest) {
    }

    public record Location(
            @JsonAlias("lat") Double latitude,
            @JsonAlias("lng") Double longitude) {
    }
}