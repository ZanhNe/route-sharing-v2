package com.zanh.route_sharing.dto.sharedroute.preview;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record GeoJsonLineStringResponse(
        String type,
        List<List<BigDecimal>> coordinates) {

    public GeoJsonLineStringResponse {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(coordinates, "coordinates must not be null");
        coordinates = coordinates.stream()
                .map(List::copyOf)
                .toList();
    }
}
