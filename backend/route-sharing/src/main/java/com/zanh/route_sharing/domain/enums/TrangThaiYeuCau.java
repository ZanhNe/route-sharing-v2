package com.zanh.route_sharing.domain.enums;

import java.util.Set;

public enum TrangThaiYeuCau {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED_BY_PASSENGER,
    CANCELLED_BY_DRIVER,
    NO_SHOW,
    PICKUP_FAILED,
    ON_BOARD,
    COMPLETED,
    ABORTED,
    DISPUTED;

    private static final Set<TrangThaiYeuCau> BLOCKING_NEW_REQUEST_STATES = Set.of(
            PENDING, ACCEPTED, ON_BOARD, DISPUTED);
    private static final Set<TrangThaiYeuCau> ACTIVE_TRIP_PARTICIPANT_STATES = Set.of(
            ACCEPTED, ON_BOARD);

    public static Set<TrangThaiYeuCau> blockingNewRequestStates() {
        return BLOCKING_NEW_REQUEST_STATES;
    }

    public static Set<TrangThaiYeuCau> activeTripParticipantStates() {
        return ACTIVE_TRIP_PARTICIPANT_STATES;
    }

    public boolean isActiveTripParticipant() {
        return ACTIVE_TRIP_PARTICIPANT_STATES.contains(this);
    }

    public boolean blocksNewRequest() {
        return BLOCKING_NEW_REQUEST_STATES.contains(this);
    }

    public boolean isTerminal() {
        return !blocksNewRequest();
    }
}
