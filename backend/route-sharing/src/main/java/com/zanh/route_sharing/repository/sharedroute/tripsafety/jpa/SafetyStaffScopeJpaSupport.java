package com.zanh.route_sharing.repository.sharedroute.tripsafety.jpa;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.enums.TrangThaiCongTac;
import com.zanh.route_sharing.domain.enums.TrangThaiHoSoThanhVien;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class SafetyStaffScopeJpaSupport {
    private final EntityManager entityManager;

    public SafetyStaffScopeJpaSupport(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long resolveTripSchoolId(Long tripId) {
        List<Long> schoolIds = entityManager.createQuery(
                        "select distinct config.nhaTruong.id from YeuCauDiChung booking "
                                + "join booking.cauHinhLucGui config where booking.chuyenDi.id = :tripId",
                        Long.class)
                .setParameter("tripId", tripId)
                .getResultList();
        if (schoolIds.size() != 1 || schoolIds.get(0) == null) throw invariantViolation();
        return schoolIds.get(0);
    }

    public boolean hasActiveSafetyStaffScope(Long userId, Long schoolId, LocalDate businessDate) {
        if (userId == null || schoolId == null || businessDate == null) return false;
        Long count = entityManager.createQuery(
                        "select count(staff) from HoSoNhanSu staff "
                                + "where staff.nguoiDung.id = :userId and staff.nhaTruong.id = :schoolId "
                                + "and staff.trangThaiHoSo = :approved and staff.trangThaiCongTac = :working "
                                + "and staff.ngayBatDauCongTac <= :businessDate "
                                + "and (staff.ngayKetThucCongTac is null or staff.ngayKetThucCongTac >= :businessDate) "
                                + "and (staff.ngayBatDauHieuLuc is null or staff.ngayBatDauHieuLuc <= :businessDate) "
                                + "and (staff.ngayKetThucHieuLuc is null or staff.ngayKetThucHieuLuc >= :businessDate)", Long.class)
                .setParameter("userId", userId)
                .setParameter("schoolId", schoolId)
                .setParameter("approved", TrangThaiHoSoThanhVien.APPROVED)
                .setParameter("working", TrangThaiCongTac.DANG_CONG_TAC)
                .setParameter("businessDate", businessDate)
                .getSingleResult();
        if (count == null || count <= 0) return false;
        NguoiDung user = entityManager.find(NguoiDung.class, userId);
        return user != null && user.getTrangThaiTaiKhoan() == TrangThaiTaiKhoan.ACTIVE;
    }

    public void requireActiveSafetyStaffScope(Long userId, Long schoolId, LocalDate businessDate) {
        if (!hasActiveSafetyStaffScope(userId, schoolId, businessDate)) throw safetyIncidentNotFound();
    }

    public List<Long> findActiveSafetySchoolIds(Long userId, LocalDate businessDate) {
        return entityManager.createQuery(
                        "select distinct staff.nhaTruong.id from HoSoNhanSu staff "
                                + "where staff.nguoiDung.id = :userId "
                                + "and staff.nguoiDung.trangThaiTaiKhoan = :active "
                                + "and staff.trangThaiHoSo = :approved and staff.trangThaiCongTac = :working "
                                + "and staff.ngayBatDauCongTac <= :businessDate "
                                + "and (staff.ngayKetThucCongTac is null or staff.ngayKetThucCongTac >= :businessDate) "
                                + "and (staff.ngayBatDauHieuLuc is null or staff.ngayBatDauHieuLuc <= :businessDate) "
                                + "and (staff.ngayKetThucHieuLuc is null or staff.ngayKetThucHieuLuc >= :businessDate) "
                                + "order by staff.nhaTruong.id", Long.class)
                .setParameter("userId", userId)
                .setParameter("active", TrangThaiTaiKhoan.ACTIVE)
                .setParameter("approved", TrangThaiHoSoThanhVien.APPROVED)
                .setParameter("working", TrangThaiCongTac.DANG_CONG_TAC)
                .setParameter("businessDate", businessDate)
                .getResultList();
    }

    public List<Long> findEligibleUserIds(Long schoolId, LocalDate businessDate, String permissionCode) {
        String sql = """
                SELECT DISTINCT u.id
                FROM nguoi_dung u
                JOIN ho_so_thanh_vien membership ON membership.nguoi_dung_id = u.id
                JOIN ho_so_nhan_su staff ON staff.ho_so_thanh_vien_id = membership.id
                WHERE u.trang_thai_tai_khoan = 'ACTIVE'
                  AND membership.nha_truong_id = :schoolId
                  AND membership.trang_thai_ho_so = 'APPROVED'
                  AND staff.trang_thai_cong_tac = 'DANG_CONG_TAC'
                  AND staff.ngay_bat_dau_cong_tac <= :businessDate
                  AND (staff.ngay_ket_thuc_cong_tac IS NULL OR staff.ngay_ket_thuc_cong_tac >= :businessDate)
                  AND (membership.ngay_bat_dau_hieu_luc IS NULL OR membership.ngay_bat_dau_hieu_luc <= :businessDate)
                  AND (membership.ngay_ket_thuc_hieu_luc IS NULL OR membership.ngay_ket_thuc_hieu_luc >= :businessDate)
                  AND (
                    EXISTS (
                      SELECT 1 FROM nguoi_dung_quyen_truc_tiep direct_link
                      JOIN quyen_han permission ON permission.id = direct_link.quyen_han_id
                      WHERE direct_link.nguoi_dung_id = u.id AND permission.dang_hoat_dong = TRUE
                        AND UPPER(permission.ma_quyen) = UPPER(:permissionCode)
                    )
                    OR EXISTS (
                      SELECT 1 FROM nguoi_dung_nhom_quyen group_link
                      JOIN nhom_quyen role_group ON role_group.id = group_link.nhom_quyen_id
                      JOIN nhom_quyen_quyen_han group_permission ON group_permission.nhom_quyen_id = role_group.id
                      JOIN quyen_han permission ON permission.id = group_permission.quyen_han_id
                      WHERE group_link.nguoi_dung_id = u.id AND role_group.dang_hoat_dong = TRUE
                        AND permission.dang_hoat_dong = TRUE
                        AND UPPER(permission.ma_quyen) = UPPER(:permissionCode)
                    )
                  )
                ORDER BY u.id
                """;
        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("schoolId", schoolId)
                .setParameter("businessDate", businessDate)
                .setParameter("permissionCode", permissionCode)
                .getResultList();
        return rows.stream().map(v -> {
            if (!(v instanceof Number n)) throw invariantViolation();
            return n.longValue();
        }).toList();
    }

    public boolean hasEffectivePermission(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null || permissionCode.isBlank()) return false;
        String sql = """
                SELECT CASE WHEN EXISTS (
                    SELECT 1 FROM nguoi_dung_quyen_truc_tiep d
                    JOIN quyen_han p ON p.id = d.quyen_han_id
                    WHERE d.nguoi_dung_id = :userId AND p.dang_hoat_dong = TRUE AND UPPER(p.ma_quyen)=UPPER(:code)
                ) OR EXISTS (
                    SELECT 1 FROM nguoi_dung_nhom_quyen g
                    JOIN nhom_quyen ng ON ng.id = g.nhom_quyen_id
                    JOIN nhom_quyen_quyen_han gp ON gp.nhom_quyen_id = ng.id
                    JOIN quyen_han p ON p.id = gp.quyen_han_id
                    WHERE g.nguoi_dung_id = :userId AND ng.dang_hoat_dong=TRUE AND p.dang_hoat_dong=TRUE AND UPPER(p.ma_quyen)=UPPER(:code)
                ) THEN 1 ELSE 0 END
                """;
        Object value = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId).setParameter("code", permissionCode).getSingleResult();
        return value instanceof Number n && n.intValue() == 1;
    }

    public static BusinessException safetyIncidentNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "SAFETY_INCIDENT_NOT_FOUND", "Không tìm thấy Safety incident.");
    }

    public static BusinessException invariantViolation() {
        return new BusinessException(HttpStatus.CONFLICT, "SAFETY_INCIDENT_INVARIANT_VIOLATION", "Dữ liệu Safety incident không nhất quán.");
    }
}
