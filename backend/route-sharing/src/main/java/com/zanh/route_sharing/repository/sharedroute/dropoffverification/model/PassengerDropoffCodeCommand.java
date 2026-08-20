package com.zanh.route_sharing.repository.sharedroute.dropoffverification.model;

import java.time.Instant;

public record PassengerDropoffCodeCommand(Long actorId, Long tripId, Instant requestedAt) {}
