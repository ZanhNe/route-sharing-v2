package com.zanh.route_sharing.repository.sharedroute.riderequest.cancellation.jpa;

import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.cancellation.RideRequestCancellationRepository;
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
public class JpaRideRequestCancellationRepository implements RideRequestCancellationRepository {
    private final EntityManager entityManager;

    public JpaRideRequestCancellationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Long> findPassengerRequestRouteId(Long actorId, Long rideRequestId) {
        List<Long> rows = entityManager.createQuery(
                        "select request.loTrinhChiaSe.id from YeuCauDiChung request "
                                + "where request.id = :rideRequestId and request.hanhKhach.id = :actorId",
                        Long.class)
                .setParameter("rideRequestId", rideRequestId)
                .setParameter("actorId", actorId)
                .setMaxResults(1)
                .getResultList();
        return rows.stream().findFirst();
    }

    @Override
    public Optional<LoTrinhChiaSe> lockRoute(Long routeId) {
        try {
            return entityManager.createQuery(
                            "select route from LoTrinhChiaSe route "
                                    + "join fetch route.taiXe driver "
                                    + "where route.id = :routeId",
                            LoTrinhChiaSe.class)
                    .setParameter("routeId", routeId)
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
    public Optional<YeuCauDiChung> lockPassengerRequest(Long actorId, Long routeId, Long rideRequestId) {
        try {
            return entityManager.createQuery(
                            "select request from YeuCauDiChung request "
                                    + "join fetch request.hanhKhach passenger "
                                    + "join fetch request.loTrinhChiaSe route "
                                    + "join fetch route.taiXe driver "
                                    + "where request.id = :rideRequestId "
                                    + "and route.id = :routeId "
                                    + "and passenger.id = :actorId",
                            YeuCauDiChung.class)
                    .setParameter("rideRequestId", rideRequestId)
                    .setParameter("routeId", routeId)
                    .setParameter("actorId", actorId)
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
    public void appendStateLog(NhatKyTrangThaiYeuCau event) {
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
                "RIDE_REQUEST_CONCURRENTLY_MODIFIED",
                "Yêu cầu hoặc số ghế đã được xử lý bởi thao tác khác. Vui lòng tải lại dữ liệu.");
    }
}
