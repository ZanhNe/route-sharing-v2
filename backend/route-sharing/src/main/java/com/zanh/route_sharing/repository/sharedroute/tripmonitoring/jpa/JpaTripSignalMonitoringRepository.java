package com.zanh.route_sharing.repository.sharedroute.tripmonitoring.jpa;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.ChuyenDi;
import com.zanh.route_sharing.domain.entity.NhatKyGiamSatTinHieu;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.repository.sharedroute.tripmonitoring.TripSignalMonitoringRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class JpaTripSignalMonitoringRepository implements TripSignalMonitoringRepository {
    private static final Set<TrangThaiVanHanhChuyenDi> TRACKING_ACTIVE_STATES = Set.of(
            TrangThaiVanHanhChuyenDi.IN_PROGRESS,
            TrangThaiVanHanhChuyenDi.SECURITY_FROZEN);

    private final EntityManager entityManager;

    public JpaTripSignalMonitoringRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findTrackingActiveTripIds() {
        return entityManager.createQuery(
                "select trip.id from ChuyenDi trip "
                        + "where trip.trangThaiVanHanh in :states "
                        + "order by trip.id asc",
                Long.class)
                .setParameter("states", TRACKING_ACTIVE_STATES)
                .getResultList();
    }

    @Override
    public ChuyenDi lockTrip(Long tripId) {
        ChuyenDi trip = entityManager.find(ChuyenDi.class, tripId, LockModeType.PESSIMISTIC_WRITE);
        if (trip == null) {
            throw new IllegalStateException("Trip monitoring target không tồn tại.");
        }
        return trip;
    }

    @Override
    public CauHinhNghiepVu loadCurrentConfiguration(Long tripId) {
        List<Long> schoolIds = entityManager.createQuery(
                "select distinct school.id from YeuCauDiChung request "
                        + "join request.cauHinhLucGui requestConfig "
                        + "join requestConfig.nhaTruong school "
                        + "where request.chuyenDi.id = :tripId",
                Long.class)
                .setParameter("tripId", tripId)
                .getResultList();
        if (schoolIds.size() != 1 || schoolIds.get(0) == null) {
            throw new IllegalStateException("Trip monitoring phải resolve đúng một nhà trường.");
        }
        return entityManager.createQuery(
                "select config from CauHinhNghiepVu config "
                        + "where config.nhaTruong.id = :schoolId",
                CauHinhNghiepVu.class)
                .setParameter("schoolId", schoolIds.get(0))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy cấu hình giám sát hiện hành."));
    }

    @Override
    public Optional<NhatKyGiamSatTinHieu> findLatestTransition(Long tripId) {
        return entityManager.createQuery(
                "select history from NhatKyGiamSatTinHieu history "
                        + "where history.chuyenDi.id = :tripId "
                        + "order by history.sequence desc",
                NhatKyGiamSatTinHieu.class)
                .setParameter("tripId", tripId)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public long nextTransitionSequence(Long tripId) {
        Long current = entityManager.createQuery(
                "select max(history.sequence) from NhatKyGiamSatTinHieu history "
                        + "where history.chuyenDi.id = :tripId",
                Long.class)
                .setParameter("tripId", tripId)
                .getSingleResult();
        return current == null ? 1L : current + 1L;
    }

    @Override
    public void persistTransition(NhatKyGiamSatTinHieu transition) {
        entityManager.persist(transition);
    }

    @Override
    public List<Long> findRealtimeRecipientUserIds(Long tripId, Set<TrangThaiYeuCau> activePassengerStates) {
        if (activePassengerStates == null || activePassengerStates.isEmpty()) {
            throw new IllegalArgumentException("activePassengerStates không được trống.");
        }
        Long driverId = entityManager.createQuery(
                "select route.taiXe.id from ChuyenDi trip join trip.loTrinhChiaSe route where trip.id = :tripId",
                Long.class)
                .setParameter("tripId", tripId)
                .getSingleResult();
        List<Long> passengerIds = entityManager.createQuery(
                "select request.hanhKhach.id from YeuCauDiChung request "
                        + "where request.chuyenDi.id = :tripId "
                        + "and request.trangThaiYeuCau in :states "
                        + "order by request.id asc",
                Long.class)
                .setParameter("tripId", tripId)
                .setParameter("states", activePassengerStates)
                .getResultList();
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        unique.add(driverId);
        unique.addAll(passengerIds);
        return new ArrayList<>(unique);
    }

    @Override
    public void flush() {
        entityManager.flush();
    }
}
