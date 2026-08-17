package com.zanh.route_sharing.domain.enums;

public enum TrangThaiDiemDung {
    PENDING,
    APPROACHING,
    ARRIVED,
    COMPLETED,
    SKIPPED,
    CANCELLED;

    public boolean isResolvedForTripProgression() {
        return this == COMPLETED || this == SKIPPED || this == CANCELLED;
    }

    public boolean isUnresolvedForTripProgression() {
        return !isResolvedForTripProgression();
    }
}
