package com.zanh.route_sharing.dto.trip.cancellation;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;

import java.time.Instant;

public record CancelTripBeforeStartResponse(
        Long tripId,
        Long routeId,
        TrangThaiVanHanhChuyenDi tripStatus,
        TrangThaiLoTrinh routeStatus,
        Instant cancelledAt,
        String reason,
        int affectedBookingCount) {
}
