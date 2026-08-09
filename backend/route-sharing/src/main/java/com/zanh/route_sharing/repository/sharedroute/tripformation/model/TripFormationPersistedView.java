package com.zanh.route_sharing.repository.sharedroute.tripformation.model;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import org.locationtech.jts.geom.LineString;

import java.time.Instant;
import java.util.List;

public record TripFormationPersistedView(
        Long routeId,
        TrangThaiLoTrinh routeStatus,
        Instant lockedAt,
        Integer remainingSeats,
        Instant expectedDepartureTime,
        Long tripId,
        TrangThaiVanHanhChuyenDi tripStatus,
        Instant formedAt,
        Integer plannedPassengerCount,
        Integer actualPassengerCount,
        LineString operationalRoute,
        List<TripFormationStopView> stops) {

    public TripFormationPersistedView {
        stops = stops == null ? List.of() : List.copyOf(stops);
    }
}
