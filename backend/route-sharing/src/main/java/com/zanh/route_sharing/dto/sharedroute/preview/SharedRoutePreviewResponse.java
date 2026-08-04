package com.zanh.route_sharing.dto.sharedroute.preview;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;

import java.math.BigDecimal;
import java.time.Instant;

public record SharedRoutePreviewResponse(
                Long routeId,
                Instant calculatedAt,
                boolean canProceed,
                TrangThaiLoTrinh routeStatus,
                LoaiGhepTuyen matchType,
                LoaiDiemTha dropoffType,
                PreviewDriverResponse driver,
                PreviewVehicleResponse vehicle,
                Instant expectedDepartureTime,
                Integer remainingSeats,
                BigDecimal suggestedSupportPerKm,
                BigDecimal pickupDeviationMeters,
                BigDecimal destinationDeviationMeters,
                BigDecimal sharedSegmentMeters,
                PreviewPointsResponse points,
                OriginalRouteResponse originalRoute,
                PreviewRouteResponse previewRoute) {
}
