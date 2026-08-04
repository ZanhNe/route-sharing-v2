package com.zanh.route_sharing.dto.sharedroute.preview;

import java.math.BigDecimal;

import com.zanh.route_sharing.service.routing.model.RouteWaypointRole;

public record PreviewRouteLegResponse(
                int sequence,
                RouteWaypointRole fromRole,
                RouteWaypointRole toRole,
                BigDecimal distanceMeters,
                long durationSeconds,
                boolean collapsed) {
}
