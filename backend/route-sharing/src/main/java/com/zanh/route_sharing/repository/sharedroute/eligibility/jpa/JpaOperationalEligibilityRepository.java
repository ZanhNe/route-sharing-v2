package com.zanh.route_sharing.repository.sharedroute.eligibility.jpa;

import com.zanh.route_sharing.repository.sharedroute.eligibility.OperationalEligibilityRepository;
import com.zanh.route_sharing.repository.sharedroute.eligibility.model.CurrentOperationalEligibility;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class JpaOperationalEligibilityRepository implements OperationalEligibilityRepository {

    private static final String CURRENT_OPERATIONAL_ELIGIBILITY_SQL = """
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

    public JpaOperationalEligibilityRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public CurrentOperationalEligibility evaluate(
            Long actorId,
            Long routeId,
            Long schoolId,
            LocalDate routeTravelDate) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(CURRENT_OPERATIONAL_ELIGIBILITY_SQL)
                .setParameter("actorId", actorId)
                .setParameter("routeId", routeId)
                .setParameter("schoolId", schoolId)
                .setParameter("routeTravelDate", routeTravelDate)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) {
            return CurrentOperationalEligibility.ineligible();
        }
        Object[] row = rows.get(0);
        return new CurrentOperationalEligibility(
                bool(row[0]),
                bool(row[1]),
                bool(row[2]),
                bool(row[3]),
                bool(row[4]),
                bool(row[5]),
                bool(row[6]),
                bool(row[7]));
    }

    private static boolean bool(Object value) {
        return value instanceof Boolean booleanValue
                ? booleanValue
                : Boolean.parseBoolean(String.valueOf(value));
    }
}
