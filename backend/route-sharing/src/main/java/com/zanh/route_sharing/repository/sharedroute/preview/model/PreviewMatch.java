package com.zanh.route_sharing.repository.sharedroute.preview.model;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;

import java.math.BigDecimal;
import java.util.Objects;

public record PreviewMatch(
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,
        PreviewGeoPoint proposedDropoff,
        BigDecimal pickupDeviationMeters,
        BigDecimal destinationDeviationMeters,
        BigDecimal sharedSegmentMeters) {

    public PreviewMatch {
        Objects.requireNonNull(matchType, "matchType không được trống");
        Objects.requireNonNull(dropoffType, "dropoffType không được trống");
        Objects.requireNonNull(proposedDropoff, "proposedDropoff không được trống");
        Objects.requireNonNull(pickupDeviationMeters, "pickupDeviationMeters không được trống");
        Objects.requireNonNull(destinationDeviationMeters, "destinationDeviationMeters không được trống");
        Objects.requireNonNull(sharedSegmentMeters, "sharedSegmentMeters không được trống");
    }
}
