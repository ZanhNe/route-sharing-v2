package com.zanh.route_sharing.integration.goong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoongGeocodingResponse(
        String status,
        List<ResultDto> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResultDto(
            @JsonProperty("formatted_address") String formattedAddress) {
    }
}
