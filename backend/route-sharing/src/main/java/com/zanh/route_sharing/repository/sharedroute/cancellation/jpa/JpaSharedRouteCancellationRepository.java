package com.zanh.route_sharing.repository.sharedroute.cancellation.jpa;

import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiLoTrinh;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.cancellation.SharedRouteCancellationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaSharedRouteCancellationRepository implements SharedRouteCancellationRepository {
    private final EntityManager entityManager;

    public JpaSharedRouteCancellationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<LoTrinhChiaSe> lockOwnedRoute(Long driverId, Long routeId) {
        try {
            return entityManager.createQuery(
                            "select route from LoTrinhChiaSe route "
                                    + "join fetch route.taiXe driver "
                                    + "where route.id = :routeId and driver.id = :driverId",
                            LoTrinhChiaSe.class)
                    .setParameter("routeId", routeId)
                    .setParameter("driverId", driverId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultList()
                    .stream()
                    .findFirst();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    @Override
    public boolean existsTripForRoute(Long routeId) {
        Long count = entityManager.createQuery(
                        "select count(trip.id) from ChuyenDi trip "
                                + "where trip.loTrinhChiaSe.id = :routeId",
                        Long.class)
                .setParameter("routeId", routeId)
                .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public List<YeuCauDiChung> lockActiveRequests(Long routeId) {
        try {
            return entityManager.createQuery(
                            "select request from YeuCauDiChung request "
                                    + "join fetch request.hanhKhach passenger "
                                    + "join fetch request.loTrinhChiaSe route "
                                    + "join fetch route.taiXe driver "
                                    + "where route.id = :routeId "
                                    + "and request.trangThaiYeuCau in :states "
                                    + "order by request.id asc",
                            YeuCauDiChung.class)
                    .setParameter("routeId", routeId)
                    .setParameter("states", List.of(
                            TrangThaiYeuCau.PENDING,
                            TrangThaiYeuCau.ACCEPTED))
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    @Override
    public long nextRouteAuditSequence(Long routeId) {
        Long next = entityManager.createQuery(
                        "select coalesce(max(event.sequence), 0) + 1 "
                                + "from NhatKyTrangThaiLoTrinh event "
                                + "where event.loTrinhChiaSe.id = :routeId",
                        Long.class)
                .setParameter("routeId", routeId)
                .getSingleResult();
        return next == null ? 1L : next;
    }

    @Override
    public long nextRequestAuditSequence(Long rideRequestId) {
        Long next = entityManager.createQuery(
                        "select coalesce(max(event.sequence), 0) + 1 "
                                + "from NhatKyTrangThaiYeuCau event "
                                + "where event.yeuCauDiChung.id = :rideRequestId",
                        Long.class)
                .setParameter("rideRequestId", rideRequestId)
                .getSingleResult();
        return next == null ? 1L : next;
    }

    @Override
    public void appendRouteStateLog(NhatKyTrangThaiLoTrinh event) {
        entityManager.persist(event);
    }

    @Override
    public void appendRequestStateLog(NhatKyTrangThaiYeuCau event) {
        entityManager.persist(event);
    }

    @Override
    public void persistNotification(ThongBao notification) {
        entityManager.persist(notification);
    }

    @Override
    public void flush() {
        try {
            entityManager.flush();
        } catch (OptimisticLockException | PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "SHARED_ROUTE_CONCURRENTLY_MODIFIED",
                "Lộ trình hoặc booking đã được xử lý bởi thao tác khác. Vui lòng tải lại dữ liệu.");
    }
}
