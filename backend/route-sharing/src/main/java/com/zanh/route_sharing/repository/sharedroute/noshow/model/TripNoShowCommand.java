package com.zanh.route_sharing.repository.sharedroute.noshow.model;

import java.time.Instant;

public record TripNoShowCommand(Long actorId, Long tripId, Instant noShowAt) {
}
