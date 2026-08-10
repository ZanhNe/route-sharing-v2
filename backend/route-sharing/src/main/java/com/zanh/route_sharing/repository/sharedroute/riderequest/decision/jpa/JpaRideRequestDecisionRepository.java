package com.zanh.route_sharing.repository.sharedroute.riderequest.decision.jpa;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.decision.RideRequestDecisionRepository;
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
public class JpaRideRequestDecisionRepository implements RideRequestDecisionRepository {


    private final EntityManager entityManager;

    public JpaRideRequestDecisionRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<LoTrinhChiaSe> lockOwnedRoute(Long actorId, Long routeId) {
        try {
            List<LoTrinhChiaSe> rows = entityManager.createQuery(
                            "select route from LoTrinhChiaSe route "
                                    + "join fetch route.taiXe driver "
                                    + "join fetch route.phuongTien vehicle "
                                    + "where route.id = :routeId and driver.id = :actorId",
                            LoTrinhChiaSe.class)
                    .setParameter("routeId", routeId)
                    .setParameter("actorId", actorId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultList();
            return rows.stream().findFirst();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    @Override
    public Optional<YeuCauDiChung> lockRideRequest(Long routeId, Long rideRequestId) {
        try {
            List<YeuCauDiChung> rows = entityManager.createQuery(
                            "select request from YeuCauDiChung request "
                                    + "join fetch request.hanhKhach passenger "
                                    + "join fetch request.cauHinhLucGui configuration "
                                    + "join fetch configuration.nhaTruong school "
                                    + "where request.id = :rideRequestId "
                                    + "and request.loTrinhChiaSe.id = :routeId",
                            YeuCauDiChung.class)
                    .setParameter("rideRequestId", rideRequestId)
                    .setParameter("routeId", routeId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .getResultList();
            return rows.stream().findFirst();
        } catch (PessimisticLockException | LockTimeoutException exception) {
            throw concurrentModification();
        }
    }

    @Override
    public Optional<CauHinhNghiepVu> lockCurrentConfiguration(YeuCauDiChung rideRequest) {
        Long schoolId = rideRequest.getCauHinhLucGui().getNhaTruong().getId();
        try {
            List<CauHinhNghiepVu> rows = entityManager.createQuery(
                            "select configuration from CauHinhNghiepVu configuration "
                                    + "join fetch configuration.nhaTruong school "
                                    + "where school.id = :schoolId and school.dangHoatDong = true",
                            CauHinhNghiepVu.class)
                    .setParameter("schoolId", schoolId)
                    .setLockMode(LockModeType.PESSIMISTIC_READ)
                    .setMaxResults(1)
                    .getResultList();
            return rows.stream().findFirst();
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
