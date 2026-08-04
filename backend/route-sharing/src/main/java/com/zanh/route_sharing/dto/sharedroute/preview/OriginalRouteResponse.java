package com.zanh.route_sharing.dto.sharedroute.preview;

import java.math.BigDecimal;

public record OriginalRouteResponse(
                GeoJsonLineStringResponse geometry,
                BigDecimal distanceMeters,
                long durationSeconds) {
}
