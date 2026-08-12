package com.zanh.route_sharing.repository.sharedroute.boarding.model;

import java.time.Instant;

public record TripBoardingCommand(Long actorId, Long tripId, String boardingCode, Instant boardedAt) {
}
