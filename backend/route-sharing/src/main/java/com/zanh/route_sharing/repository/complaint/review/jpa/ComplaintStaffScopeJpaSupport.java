package com.zanh.route_sharing.repository.complaint.review.jpa;

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
public class ComplaintStaffScopeJpaSupport {
    public static final String HANDLE_COMPLAINT = "HANDLE_COMPLAINT";
    public static final String REASSIGN_COMPLAINT = "REASSIGN_COMPLAINT";
    public static final String VIEW_SENSITIVE = "VIEW_COMPLAINT_SENSITIVE_EVIDENCE";

    private final EntityManager entityManager;

    public ComplaintStaffScopeJpaSupport(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long resolveComplaintSchoolId(Long complaintId) {
        List<Long> ids = entityManager.createQuery(
                "select distinct config.nhaTruong.id from KhieuNai c "
                        + "join c.yeuCauDiChung booking join booking.cauHinhLucGui config where c.id=:id", Long.class)
                .setParameter("id", complaintId).getResultList();
        if (ids.size() != 1 || ids.get(0) == null) throw invariant();
        return ids.get(0);
    }

    public List<Long> findActiveSchoolIds(Long userId, LocalDate date) {
        return entityManager.createQuery(
                "select distinct staff.nhaTruong.id from HoSoNhanSu staff "
                        + "where staff.nguoiDung.id=:uid and staff.nguoiDung.trangThaiTaiKhoan=:active "
                        + "and staff.trangThaiHoSo=:approved and staff.trangThaiCongTac=:working "
                        + "and staff.ngayBatDauCongTac<=:d and (staff.ngayKetThucCongTac is null or staff.ngayKetThucCongTac>=:d) "
                        + "and (staff.ngayBatDauHieuLuc is null or staff.ngayBatDauHieuLuc<=:d) "
                        + "and (staff.ngayKetThucHieuLuc is null or staff.ngayKetThucHieuLuc>=:d) order by staff.nhaTruong.id", Long.class)
                .setParameter("uid", userId).setParameter("active", TrangThaiTaiKhoan.ACTIVE)
                .setParameter("approved", TrangThaiHoSoThanhVien.APPROVED)
                .setParameter("working", TrangThaiCongTac.DANG_CONG_TAC).setParameter("d", date).getResultList();
    }

    public boolean hasActiveStaffScope(Long userId, Long schoolId, LocalDate date) {
        if (userId == null || schoolId == null || date == null) return false;
        Long count = entityManager.createQuery(
                "select count(staff) from HoSoNhanSu staff where staff.nguoiDung.id=:uid and staff.nhaTruong.id=:sid "
                        + "and staff.nguoiDung.trangThaiTaiKhoan=:active and staff.trangThaiHoSo=:approved "
                        + "and staff.trangThaiCongTac=:working and staff.ngayBatDauCongTac<=:d "
                        + "and (staff.ngayKetThucCongTac is null or staff.ngayKetThucCongTac>=:d) "
                        + "and (staff.ngayBatDauHieuLuc is null or staff.ngayBatDauHieuLuc<=:d) "
                        + "and (staff.ngayKetThucHieuLuc is null or staff.ngayKetThucHieuLuc>=:d)", Long.class)
                .setParameter("uid", userId).setParameter("sid", schoolId)
                .setParameter("active", TrangThaiTaiKhoan.ACTIVE)
                .setParameter("approved", TrangThaiHoSoThanhVien.APPROVED)
                .setParameter("working", TrangThaiCongTac.DANG_CONG_TAC).setParameter("d", date).getSingleResult();
        return count != null && count > 0;
    }

    public boolean hasEffectivePermission(Long userId, String code) {
        if (userId == null || code == null || code.isBlank()) return false;
        String sql = """
                SELECT CASE WHEN EXISTS (
                    SELECT 1 FROM nguoi_dung_quyen_truc_tiep d
                    JOIN quyen_han p ON p.id=d.quyen_han_id
                    WHERE d.nguoi_dung_id=:uid AND p.dang_hoat_dong=TRUE AND UPPER(p.ma_quyen)=UPPER(:code)
                ) OR EXISTS (
                    SELECT 1 FROM nguoi_dung_nhom_quyen g
                    JOIN nhom_quyen ng ON ng.id=g.nhom_quyen_id
                    JOIN nhom_quyen_quyen_han gp ON gp.nhom_quyen_id=ng.id
                    JOIN quyen_han p ON p.id=gp.quyen_han_id
                    WHERE g.nguoi_dung_id=:uid AND ng.dang_hoat_dong=TRUE AND p.dang_hoat_dong=TRUE
                      AND UPPER(p.ma_quyen)=UPPER(:code)
                ) THEN 1 ELSE 0 END
                """;
        Object value = entityManager.createNativeQuery(sql).setParameter("uid", userId).setParameter("code", code).getSingleResult();
        return value instanceof Number n && n.intValue() == 1;
    }

    public void requireHandler(Long actorId, Long schoolId, LocalDate date) {
        if (!hasActiveStaffScope(actorId, schoolId, date) || !hasEffectivePermission(actorId, HANDLE_COMPLAINT)) {
            throw contextNotFound();
        }
    }

    public void requireReassigner(Long actorId, Long schoolId, LocalDate date) {
        if (!hasActiveStaffScope(actorId, schoolId, date) || !hasEffectivePermission(actorId, REASSIGN_COMPLAINT)) {
            throw contextNotFound();
        }
    }

    public void requireSensitiveViewer(Long actorId, Long schoolId, LocalDate date) {
        requireHandler(actorId, schoolId, date);
        if (!hasEffectivePermission(actorId, VIEW_SENSITIVE)) throw contextNotFound();
    }

    public List<Long> findEligibleReviewerIds(Long schoolId, LocalDate date) {
        String sql = """
                SELECT DISTINCT u.id
                FROM nguoi_dung u
                JOIN ho_so_thanh_vien membership ON membership.nguoi_dung_id=u.id
                JOIN ho_so_nhan_su staff ON staff.ho_so_thanh_vien_id=membership.id
                WHERE u.trang_thai_tai_khoan='ACTIVE'
                  AND membership.nha_truong_id=:sid
                  AND membership.trang_thai_ho_so='APPROVED'
                  AND staff.trang_thai_cong_tac='DANG_CONG_TAC'
                  AND staff.ngay_bat_dau_cong_tac<=:d
                  AND (staff.ngay_ket_thuc_cong_tac IS NULL OR staff.ngay_ket_thuc_cong_tac>=:d)
                  AND (membership.ngay_bat_dau_hieu_luc IS NULL OR membership.ngay_bat_dau_hieu_luc<=:d)
                  AND (membership.ngay_ket_thuc_hieu_luc IS NULL OR membership.ngay_ket_thuc_hieu_luc>=:d)
                  AND (
                    EXISTS (SELECT 1 FROM nguoi_dung_quyen_truc_tiep dl JOIN quyen_han p ON p.id=dl.quyen_han_id
                            WHERE dl.nguoi_dung_id=u.id AND p.dang_hoat_dong=TRUE AND UPPER(p.ma_quyen)=UPPER(:code))
                    OR EXISTS (SELECT 1 FROM nguoi_dung_nhom_quyen gl JOIN nhom_quyen ng ON ng.id=gl.nhom_quyen_id
                            JOIN nhom_quyen_quyen_han gp ON gp.nhom_quyen_id=ng.id JOIN quyen_han p ON p.id=gp.quyen_han_id
                            WHERE gl.nguoi_dung_id=u.id AND ng.dang_hoat_dong=TRUE AND p.dang_hoat_dong=TRUE
                              AND UPPER(p.ma_quyen)=UPPER(:code))
                  ) ORDER BY u.id
                """;
        List<?> rows = entityManager.createNativeQuery(sql).setParameter("sid", schoolId).setParameter("d", date)
                .setParameter("code", HANDLE_COMPLAINT).getResultList();
        return rows.stream().map(v -> ((Number) v).longValue()).toList();
    }

    public NguoiDung requireUser(Long userId) {
        NguoiDung user = userId == null ? null : entityManager.find(NguoiDung.class, userId);
        if (user == null) throw contextNotFound();
        return user;
    }

    public static BusinessException contextNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "COMPLAINT_REVIEW_CONTEXT_NOT_FOUND", "Không tìm thấy complaint review phù hợp.");
    }

    public static BusinessException invariant() {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "COMPLAINT_REVIEW_INVARIANT_VIOLATION", "Dữ liệu complaint review không nhất quán.");
    }
}
