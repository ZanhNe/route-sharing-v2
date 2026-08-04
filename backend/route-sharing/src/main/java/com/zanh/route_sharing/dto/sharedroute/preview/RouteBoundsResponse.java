package com.zanh.route_sharing.dto.sharedroute.preview;

import java.math.BigDecimal;
import java.util.List;

public record RouteBoundsResponse(
        List<BigDecimal> southWest,
        List<BigDecimal> northEast) {

    public RouteBoundsResponse {
        southWest = List.copyOf(southWest);
        northEast = List.copyOf(northEast);
    }
}
