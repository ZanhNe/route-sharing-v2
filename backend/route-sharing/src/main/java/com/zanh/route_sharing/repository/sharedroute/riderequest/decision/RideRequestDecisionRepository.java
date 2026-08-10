package com.zanh.route_sharing.repository.sharedroute.riderequest.decision;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;

import java.util.Optional;

public interface RideRequestDecisionRepository {

    Optional<LoTrinhChiaSe> lockOwnedRoute(Long actorId, Long routeId);

    Optional<YeuCauDiChung> lockRideRequest(Long routeId, Long rideRequestId);

    Optional<CauHinhNghiepVu> lockCurrentConfiguration(YeuCauDiChung rideRequest);

    void appendStateLog(NhatKyTrangThaiYeuCau event);

    void persistNotification(ThongBao notification);

    void flush();
}
