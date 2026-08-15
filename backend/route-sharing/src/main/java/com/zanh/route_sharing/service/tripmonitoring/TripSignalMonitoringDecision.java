package com.zanh.route_sharing.service.tripmonitoring;

import com.zanh.route_sharing.domain.enums.TrangThaiGiamSatChuyenDi;

import java.util.Objects;

public record TripSignalMonitoringDecision(
        TrangThaiGiamSatChuyenDi previousState,
        TrangThaiGiamSatChuyenDi desiredState,
        boolean changed,
        TripSignalMonitoringReason reason) {

    public TripSignalMonitoringDecision {
        Objects.requireNonNull(previousState, "previousState không được trống");
        Objects.requireNonNull(desiredState, "desiredState không được trống");
        if (changed && previousState == desiredState) {
            throw new IllegalArgumentException("Changed decision phải đổi monitoring state.");
        }
        if (!changed && previousState != desiredState) {
            throw new IllegalArgumentException("Unchanged decision phải giữ monitoring state.");
        }
        if (changed) {
            Objects.requireNonNull(reason, "Changed decision phải có reason");
        } else if (reason != null) {
            throw new IllegalArgumentException("Unchanged decision không có transition reason.");
        }
    }

    public static TripSignalMonitoringDecision unchanged(TrangThaiGiamSatChuyenDi state) {
        return new TripSignalMonitoringDecision(state, state, false, null);
    }
}
