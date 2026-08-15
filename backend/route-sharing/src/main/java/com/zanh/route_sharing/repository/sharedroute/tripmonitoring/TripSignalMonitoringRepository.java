package com.zanh.route_sharing.repository.sharedroute.tripmonitoring;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.NhatKyGiamSatTinHieu;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TripSignalMonitoringRepository {
    List<Long> findTrackingActiveTripIds();
    ChuyenDi lockTrip(Long tripId);
    CauHinhNghiepVu loadCurrentConfiguration(Long tripId);
    Optional<NhatKyGiamSatTinHieu> findLatestTransition(Long tripId);
    long nextTransitionSequence(Long tripId);
    void persistTransition(NhatKyGiamSatTinHieu transition);
    List<Long> findRealtimeRecipientUserIds(Long tripId, Set<TrangThaiYeuCau> activePassengerStates);
    void flush();
}
