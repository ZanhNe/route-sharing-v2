package com.zanh.route_sharing.repository.sharedroute.tripsafety.model;

import com.zanh.route_sharing.domain.enums.LoaiSuCo;

import java.time.Instant;

public record TripSafetyIncidentCommand(
        Long actorId,
        Long tripId,
        LoaiSuCo type,
        String description,
        Long reportedParticipantId,
        Instant reportedAt) {
}
