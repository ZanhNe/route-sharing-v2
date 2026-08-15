package com.zanh.route_sharing.repository.sharedroute.tripquery.model;

import java.util.List;
import java.util.Objects;

public record TripDetailSnapshot(
        TripDetailHeaderRow header,
        List<TripDetailParticipantRow> participants,
        List<TripDetailStopRow> stops,
        TripDetailCurrentLocationRow currentDriverLocation) {

    public TripDetailSnapshot(TripDetailHeaderRow header, List<TripDetailParticipantRow> participants, List<TripDetailStopRow> stops) {
        this(header, participants, stops, null);
    }

    public TripDetailSnapshot {
        Objects.requireNonNull(header, "header không được trống.");
        participants = participants == null ? List.of() : List.copyOf(participants);
        stops = stops == null ? List.of() : List.copyOf(stops);
    }
}
