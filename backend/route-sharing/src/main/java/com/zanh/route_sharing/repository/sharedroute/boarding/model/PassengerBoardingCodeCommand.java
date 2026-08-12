package com.zanh.route_sharing.repository.sharedroute.boarding.model;

import java.time.Instant;

public record PassengerBoardingCodeCommand(Long actorId, Long tripId, Instant requestedAt) {
}
