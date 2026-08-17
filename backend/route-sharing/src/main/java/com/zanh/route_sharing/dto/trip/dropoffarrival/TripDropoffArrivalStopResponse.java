package com.zanh.route_sharing.dto.trip.dropoffarrival;

import java.time.Instant;

public record TripDropoffArrivalStopResponse(
                Long stopId,
                Integer order,
                String status,
                Instant arrivedAt) {
}
