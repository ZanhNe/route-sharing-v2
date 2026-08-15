package com.zanh.route_sharing.repository.sharedroute.triplocation.model;

import java.time.Instant;
import java.util.Objects;

public final class TripLocationCurrentOrdering {

    public static final String SQL_ORDER_BY = """
            ORDER BY LEAST(thoi_gian_trinh_duyet, thoi_gian_server_nhan) DESC,
                     thu_tu_ban_ghi DESC
            """;

    private TripLocationCurrentOrdering() {
    }

    public static int compare(
            Instant observedAt,
            Instant receivedAt,
            long sequence,
            Instant currentObservedAt,
            Instant currentReceivedAt,
            long currentSequence) {
        Instant effective = effectiveObservedAt(observedAt, receivedAt);
        Instant currentEffective = effectiveObservedAt(currentObservedAt, currentReceivedAt);
        int timeComparison = effective.compareTo(currentEffective);
        return timeComparison != 0 ? timeComparison : Long.compare(sequence, currentSequence);
    }

    public static Instant effectiveObservedAt(Instant observedAt, Instant receivedAt) {
        Objects.requireNonNull(observedAt, "observedAt không được trống.");
        Objects.requireNonNull(receivedAt, "receivedAt không được trống.");
        return observedAt.isBefore(receivedAt) ? observedAt : receivedAt;
    }
}
