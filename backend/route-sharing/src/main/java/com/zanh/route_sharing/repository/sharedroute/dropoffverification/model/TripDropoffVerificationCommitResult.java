package com.zanh.route_sharing.repository.sharedroute.dropoffverification.model;

import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import java.time.Instant;

public record TripDropoffVerificationCommitResult(
                Long tripId, Long routeId, Long rideRequestId, Long dropoffStopId, Integer dropoffStopOrder,
                TrangThaiYeuCau bookingStatus, TrangThaiDiemDung dropoffStatus, Instant droppedOffAt,
                Integer actualPassengerCount, Long realtimeRecipientUserId) {
}
