package com.zanh.route_sharing.repository.sharedroute.dropoffverification.model;

import java.time.Instant;

public record TripDropoffVerificationCommand(Long actorId, Long tripId, String dropoffCode, Instant droppedOffAt) {
}
