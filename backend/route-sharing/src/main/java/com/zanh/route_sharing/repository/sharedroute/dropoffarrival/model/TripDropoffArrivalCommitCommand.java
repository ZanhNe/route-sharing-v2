package com.zanh.route_sharing.repository.sharedroute.dropoffarrival.model;

import org.locationtech.jts.geom.Point;
import java.time.Instant;

public record TripDropoffArrivalCommitCommand(Long actorId, Long tripId, Point currentLocation, Instant arrivedAt) { }
