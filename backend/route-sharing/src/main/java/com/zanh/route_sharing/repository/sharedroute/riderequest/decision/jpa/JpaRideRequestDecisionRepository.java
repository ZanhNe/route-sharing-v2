package com.zanh.route_sharing.repository.sharedroute.riderequest.decision.jpa;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NhatKyTrangThaiYeuCau;
import com.zanh.route_sharing.domain.entity.ThongBao;
import com.zanh.route_sharing.domain.entity.YeuCauDiChung;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.riderequest.decision.RideRequestDecisionRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.decision.model.CurrentAcceptEligibility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaRideRequestDecisionRepository implements RideRequestDecisionRepository {

    private static final String CURRENT_ACCEPT_ELIGIBILITY_SQL = """
            SELECT
                (driver.trang_thai_tai_khoan = 'ACTIVE') AS driver_account_active,
                (driver_profile.trang_thai_tai_xe = 'ACTIVE') AS driver_profile_active,
                EXISTS (
                    SELECT 1
                    FROM ho_so_thanh_vien membership
                    WHERE membership.nguoi_dung_id = driver.id
                      AND membership.nha_truong_id = :schoolId
                      AND membership.trang_thai_ho_so = 'APPROVED'
                      AND (membership.ngay_bat_dau_hieu_luc IS NULL
                           OR membership.ngay_bat_dau_hieu_luc <= :routeTravelDate)
                      AND (membership.ngay_ket_thuc_hieu_luc IS NULL
                           OR membership.ngay_ket_thuc_hieu_luc >= :routeTravelDate)
                ) AS membership_approved,
                (vehicle.trang_thai_phuong_tien = 'ACTIVE') AS vehicle_active,
                (vehicle.nguoi_dang_ky_su_dung_id = driver.id) AS vehicle_use_right_valid,
                (model.dang_hoat_dong = TRUE) AS vehicle_model_active,
                (brand.dang_hoat_dong = TRUE) AS vehicle_brand_active,
                (school.dang_hoat_dong = TRUE) AS school_active
            FROM lo_trinh_chia_se route
            JOIN nguoi_dung driver ON driver.id = route.tai_xe_id
            JOIN ho_so_tai_xe driver_profile ON driver_profile.nguoi_dung_id = driver.id
            JOIN phuong_tien vehicle ON vehicle.id = route.phuong_tien_id
            JOIN dong_xe model ON model.id = vehicle.dong_xe_id
            JOIN hang_xe brand ON brand.id = model.hang_xe_id
            JOIN nha_truong school ON school.id = :schoolId
            WHERE route.id = :routeId
              AND route.tai_xe_id = :actorId
            """;

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
    public CurrentAcceptEligibility evaluateCurrentAcceptEligibility(
            Long actorId,
            Long routeId,
            Long schoolId,
            LocalDate routeTravelDate) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(CURRENT_ACCEPT_ELIGIBILITY_SQL)
                .setParameter("actorId", actorId)
                .setParameter("routeId", routeId)
                .setParameter("schoolId", schoolId)
                .setParameter("routeTravelDate", routeTravelDate)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) {
            return new CurrentAcceptEligibility(false, false, false, false, false, false, false, false);
        }
        Object[] row = rows.get(0);
        return new CurrentAcceptEligibility(
                bool(row[0]),
                bool(row[1]),
                bool(row[2]),
                bool(row[3]),
                bool(row[4]),
                bool(row[5]),
                bool(row[6]),
                bool(row[7]));
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

    private static boolean bool(Object value) {
        return value instanceof Boolean booleanValue
                ? booleanValue
                : Boolean.parseBoolean(String.valueOf(value));
    }

    private static BusinessException concurrentModification() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "RIDE_REQUEST_CONCURRENTLY_MODIFIED",
                "Yêu cầu hoặc số ghế đã được xử lý bởi thao tác khác. Vui lòng tải lại dữ liệu.");
    }
}
