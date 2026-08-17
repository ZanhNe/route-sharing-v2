package com.zanh.route_sharing.repository.sharedroute.tripcompletion.model;

import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;

import java.time.Instant;

public record TripCompletionCommitResult(
                Long tripId,
                Long routeId,
                TrangThaiVanHanhChuyenDi tripStatus,
                Instant endedAt,
                Integer actualPassengerCount,
                Long driverEndStopId,
                Integer driverEndStopOrder,
                TrangThaiDiemDung driverEndStatus,
                Instant driverEndCompletedAt) {
}
