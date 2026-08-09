package com.zanh.route_sharing.repository.sharedroute.tripformation.model;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.repository.sharedroute.riderequest.decision.model.CurrentAcceptEligibility;
import com.zanh.route_sharing.service.tripformation.model.TripFormationBookingSnapshot;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TripFormationPreparation(
        Long routeId,
        Long routeVersion,
        TrangThaiLoTrinh routeStatus,
        Integer remainingSeats,
        Instant expectedDepartureTime,
        Long driverId,
        Long vehicleId,
        Long vehicleVersion,
        LoaiPhuongTien vehicleType,
        Point origin,
        String originAddress,
        Point driverDestination,
        String driverDestinationAddress,
        LineString originalRoute,
        Long schoolId,
        Long configurationId,
        Long configurationVersion,
        BigDecimal arrivalRadiusMeters,
        CurrentAcceptEligibility eligibility,
        List<TripFormationBookingSnapshot> activeRequests,
        TripFormationPersistedView existingFormation) {

    public TripFormationPreparation {
        activeRequests = activeRequests == null ? List.of() : List.copyOf(activeRequests);
    }
}
