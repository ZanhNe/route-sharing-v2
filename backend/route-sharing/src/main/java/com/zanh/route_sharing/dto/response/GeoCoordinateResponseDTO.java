package com.zanh.route_sharing.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoCoordinateResponseDTO(
        @JsonProperty("status") String status,
        @JsonProperty("results") Result[] results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            @JsonProperty("geometry") Geometry geometry) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Geometry(
                @JsonProperty("location") Location location) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Location(
                    @JsonAlias("lat") double latitude,
                    @JsonAlias("lng") double longitude) {
            }
        }
    }
}