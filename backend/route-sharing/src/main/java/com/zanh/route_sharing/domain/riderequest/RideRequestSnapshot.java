package com.zanh.route_sharing.domain.riderequest;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import org.locationtech.jts.geom.LineString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record RideRequestSnapshot(
        Long routeVersion,
        Long driverId,
        Instant expectedDepartureTime,
        LoaiGhepTuyen matchType,
        LoaiDiemTha dropoffType,
        RideRequestPointSnapshot pickup,
        RideRequestPointSnapshot passengerDestination,
        RideRequestPointSnapshot proposedDropoff,
        LineString passengerDesiredRoute,
        LineString servedRouteSegment,
        BigDecimal pickupDeviationMeters,
        long pickupDeviationSeconds,
        BigDecimal passengerDesiredDistanceMeters,
        BigDecimal servedDistanceMeters,
        BigDecimal remainingDistanceMeters,
        BigDecimal convenienceRatioPercent,
        BigDecimal suggestedSupportPerKm,
        BigDecimal proposedSupportAmount,
        BigDecimal agreedSupportAmount,
        RideRequestPolicySnapshot policy) {

    public RideRequestSnapshot {
        requireNonNegative(routeVersion, "routeVersion");
        requirePositive(driverId, "driverId");
        Objects.requireNonNull(expectedDepartureTime, "expectedDepartureTime không được trống");
        Objects.requireNonNull(matchType, "matchType không được trống");
        Objects.requireNonNull(dropoffType, "dropoffType không được trống");
        Objects.requireNonNull(pickup, "pickup không được trống");
        Objects.requireNonNull(passengerDestination, "passengerDestination không được trống");
        Objects.requireNonNull(proposedDropoff, "proposedDropoff không được trống");
        requireLineString(passengerDesiredRoute, "passengerDesiredRoute");
        requireLineString(servedRouteSegment, "servedRouteSegment");
        requireNonNegative(pickupDeviationMeters, "pickupDeviationMeters");
        if (pickupDeviationSeconds < 0) {
            throw new IllegalArgumentException("pickupDeviationSeconds không được âm");
        }
        requirePositive(passengerDesiredDistanceMeters, "passengerDesiredDistanceMeters");
        requireNonNegative(servedDistanceMeters, "servedDistanceMeters");
        requireNonNegative(remainingDistanceMeters, "remainingDistanceMeters");
        requirePercent(convenienceRatioPercent, "convenienceRatioPercent");
        if (suggestedSupportPerKm != null && suggestedSupportPerKm.signum() < 0) {
            throw new IllegalArgumentException("suggestedSupportPerKm không được âm");
        }
        requireNonNegative(proposedSupportAmount, "proposedSupportAmount");
        if (agreedSupportAmount != null) {
            throw new IllegalArgumentException("PENDING chưa được có agreedSupportAmount");
        }
        Objects.requireNonNull(policy, "policy không được trống");

        BigDecimal legTotal = servedDistanceMeters.add(remainingDistanceMeters);
        if (legTotal.compareTo(passengerDesiredDistanceMeters) != 0) {
            throw new IllegalArgumentException("Tổng khoảng cách các chặng phải bằng tuyến hành khách");
        }
        if (matchType == LoaiGhepTuyen.CUNG_DIEM_DEN
                && (dropoffType != LoaiDiemTha.DIEM_DICH_CUOI_CUNG
                || remainingDistanceMeters.signum() != 0)) {
            throw new IllegalArgumentException("CUNG_DIEM_DEN phải thả tại đích và không còn chặng cuối");
        }
        if (matchType == LoaiGhepTuyen.TRUNG_DOAN_TUYEN
                && (dropoffType != LoaiDiemTha.DIEM_THA_TRUNG_GIAN
                || remainingDistanceMeters.signum() <= 0)) {
            throw new IllegalArgumentException("TRUNG_DOAN_TUYEN phải có điểm thả trung gian và chặng còn lại");
        }
    }

    private static void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " phải là số dương");
        }
    }

    private static void requireNonNegative(Long value, String name) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(name + " không được âm");
        }
    }

    private static void requirePositive(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(name + " phải lớn hơn 0");
        }
    }

    private static void requireNonNegative(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(name + " không được âm");
        }
    }

    private static void requirePercent(BigDecimal value, String name) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(name + " phải trong đoạn 0..100");
        }
    }

    private static void requireLineString(LineString value, String name) {
        if (value == null || value.isEmpty() || value.getNumPoints() < 2
                || value.getLength() == 0.0d || value.getSRID() != Wgs84Coordinates.SRID) {
            throw new IllegalArgumentException(name + " phải là LineString WGS84 SRID 4326 hợp lệ");
        }
    }
}
