package com.zanh.route_sharing.dto.sharedroute;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;

import java.math.BigDecimal;
import java.time.Instant;

public record SharedRouteResponse(
                Long id,
                TrangThaiLoTrinh status,
                Instant expectedDepartureTime,
                Integer offeredSeats,
                Integer remainingSeats,
                BigDecimal estimatedDistanceMeters,
                Long estimatedDurationSeconds,
                BigDecimal suggestedSupportPerKm,
                RouteEndpointResponse origin,
                RouteEndpointResponse driverDestination,
                Long driverId,
                Long vehicleId,
                Instant createdAt) {
}
