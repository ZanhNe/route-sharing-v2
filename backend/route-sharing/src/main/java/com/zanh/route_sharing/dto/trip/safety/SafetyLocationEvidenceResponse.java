package com.zanh.route_sharing.dto.trip.safety;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SafetyLocationEvidenceResponse(List<Item> items, SafetyPageMeta page) {
    public record Item(Long locationSequence, BigDecimal latitude, BigDecimal longitude,
                       Instant observedAt, Instant receivedAt, Instant effectiveObservedAt,
                       BigDecimal accuracyMeters, BigDecimal speedMetersPerSecond,
                       BigDecimal headingDegrees, String source) {}
}
