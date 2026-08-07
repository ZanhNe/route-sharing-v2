package com.zanh.route_sharing.repository.sharedroute.riderequest.cancellation;

import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;

import java.util.Optional;

public interface RideRequestCancellationRepository {
    Optional<Long> findPassengerRequestRouteId(Long actorId, Long rideRequestId);
    Optional<LoTrinhChiaSe> lockRoute(Long routeId);
    Optional<YeuCauDiChung> lockPassengerRequest(Long actorId, Long routeId, Long rideRequestId);
    void appendStateLog(NhatKyTrangThaiYeuCau event);
    void persistNotification(ThongBao notification);
    void flush();
}
