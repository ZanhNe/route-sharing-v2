package com.zanh.route_sharing.dto.sharedroute.preview;

import java.math.BigDecimal;
import java.util.List;

public record PreviewRouteResponse(
        GeoJsonLineStringResponse geometry,
        RouteBoundsResponse bounds,
        BigDecimal distanceMeters,
        long durationSeconds,
        List<PreviewRouteLegResponse> legs,
        List<String> warnings) {

    public PreviewRouteResponse {
        legs = List.copyOf(legs);
        warnings = List.copyOf(warnings);
    }
}
