package com.zanh.route_sharing.repository.sharedroute.dropoffarrival.model;

import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import java.time.Instant;

public record TripDropoffArrivalCommitResult(
        Long tripId, Long routeId, TrangThaiVanHanhChuyenDi tripStatus,
        Long rideRequestId, TrangThaiYeuCau bookingStatus, Integer actualPassengerCount,
        Long dropoffStopId, Integer dropoffStopOrder, TrangThaiDiemDung dropoffStatus,
        Instant arrivedAt, Long realtimeRecipientUserId) { }
