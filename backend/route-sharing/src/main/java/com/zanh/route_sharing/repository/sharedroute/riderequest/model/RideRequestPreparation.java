package com.zanh.route_sharing.repository.sharedroute.riderequest.model;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.riderequest.RideRequestPolicySnapshot;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewConsistencyToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record RideRequestPreparation(
        Long routeId,
        Long routeVersion,
        Long driverId,
        LoaiPhuongTien vehicleType,
        Instant expectedDepartureTime,
        Integer remainingSeats,
        BigDecimal suggestedSupportPerKm,
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,
        RideRequestGeoPoint pickupProjection,
        RideRequestGeoPoint proposedDropoff,
        RideRequestPolicySnapshot policy,
        PreviewConsistencyToken consistencyToken) {

    public RideRequestPreparation {
        requirePositive(routeId, "routeId");
        if (routeVersion == null || routeVersion < 0) {
            throw new IllegalArgumentException("routeVersion không được âm");
        }
        requirePositive(driverId, "driverId");
        Objects.requireNonNull(vehicleType, "vehicleType không được trống");
        Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống");
        if (remainingSeats == null || remainingSeats <= 0) {
            throw new IllegalArgumentException("remainingSeats phải lớn hơn 0");
        }
        if (suggestedSupportPerKm != null && suggestedSupportPerKm.signum() < 0) {
            throw new IllegalArgumentException("suggestedSupportPerKm không được âm");
        }
        Objects.requireNonNull(matchType, "matchType không được trống");
        Objects.requireNonNull(dropoffType, "dropoffType không được trống");
        Objects.requireNonNull(pickupProjection, "pickupProjection không được trống");
        Objects.requireNonNull(proposedDropoff, "proposedDropoff không được trống");
        Objects.requireNonNull(policy, "policy không được trống");
        Objects.requireNonNull(consistencyToken, "consistencyToken không được trống");
    }

    private static void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " phải là số dương");
        }
    }
}
