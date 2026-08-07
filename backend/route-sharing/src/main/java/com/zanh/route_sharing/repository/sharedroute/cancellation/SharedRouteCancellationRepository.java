package com.zanh.route_sharing.repository.sharedroute.cancellation;

import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiLoTrinh;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;

import java.util.List;
import java.util.Optional;

public interface SharedRouteCancellationRepository {
    Optional<LoTrinhChiaSe> lockOwnedRoute(Long driverId, Long routeId);
    boolean existsTripForRoute(Long routeId);
    List<YeuCauDiChung> lockActiveRequests(Long routeId);
    long nextRouteAuditSequence(Long routeId);
    long nextRequestAuditSequence(Long rideRequestId);
    void appendRouteStateLog(NhatKyTrangThaiLoTrinh event);
    void appendRequestStateLog(NhatKyTrangThaiYeuCau event);
    void persistNotification(ThongBao notification);
    void flush();
}
