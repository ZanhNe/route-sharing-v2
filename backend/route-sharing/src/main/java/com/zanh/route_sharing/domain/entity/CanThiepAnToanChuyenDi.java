package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiCanThiepAnToan;
import com.zanh.route_sharing.domain.enums.TrangThaiCanThiepAnToan;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "can_thiep_an_toan_chuyen_di", uniqueConstraints = @UniqueConstraint(name = "uk_can_thiep_an_toan_trip_sequence", columnNames = {
        "chuyen_di_id", "thu_tu_can_thiep" }), indexes = {
                @Index(name = "idx_can_thiep_an_toan_trip_state", columnList = "chuyen_di_id,trang_thai_can_thiep"),
                @Index(name = "idx_can_thiep_an_toan_incident_time", columnList = "su_co_chuyen_di_id,khoi_tao_luc"),
                @Index(name = "idx_can_thiep_an_toan_booking_time", columnList = "yeu_cau_muc_tieu_id,khoi_tao_luc")
        }, check = {
                @CheckConstraint(name = "ck_can_thiep_an_toan_sequence", constraint = "thu_tu_can_thiep > 0"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_count_start", constraint = "so_khach_thuc_te_luc_bat_dau >= 0"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_count_end", constraint = "so_khach_thuc_te_luc_ket_thuc IS NULL OR so_khach_thuc_te_luc_ket_thuc >= 0"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_credential_count", constraint = "so_xac_thuc_len_xe_vo_hieu_hoa >= 0"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_target", constraint = "loai_can_thiep = 'HUY_CHUYEN_KHAN_CAP' OR yeu_cau_muc_tieu_id IS NOT NULL"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_completion_shape", constraint = "(trang_thai_can_thiep = 'DANG_THUC_HIEN' AND ket_thuc_luc IS NULL AND nguoi_ket_thuc_id IS NULL) OR (trang_thai_can_thiep <> 'DANG_THUC_HIEN' AND ket_thuc_luc IS NOT NULL AND nguoi_ket_thuc_id IS NOT NULL)"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_time_order", constraint = "ket_thuc_luc IS NULL OR ket_thuc_luc >= khoi_tao_luc"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_safe_exit_pair", constraint = "(toa_do_xuong_xe_khan_cap IS NULL AND xuong_xe_khan_cap_luc IS NULL) OR (toa_do_xuong_xe_khan_cap IS NOT NULL AND xuong_xe_khan_cap_luc IS NOT NULL)"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_stopped_hold", constraint = "trang_thai_can_thiep <> 'DUNG_DO_CHUYEN_DI_KET_THUC' OR loai_can_thiep = 'GIU_DE_XUONG_XE_AN_TOAN'"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_active_hold", constraint = "trang_thai_can_thiep <> 'DANG_THUC_HIEN' OR loai_can_thiep = 'GIU_DE_XUONG_XE_AN_TOAN'"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_non_hold_completed", constraint = "loai_can_thiep = 'GIU_DE_XUONG_XE_AN_TOAN' OR trang_thai_can_thiep = 'HOAN_TAT'"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_safe_exit_end_time", constraint = "xuong_xe_khan_cap_luc IS NULL OR xuong_xe_khan_cap_luc = ket_thuc_luc"),
                @CheckConstraint(name = "ck_can_thiep_an_toan_safe_exit_only_completed_hold", constraint = "toa_do_xuong_xe_khan_cap IS NULL OR (loai_can_thiep = 'GIU_DE_XUONG_XE_AN_TOAN' AND trang_thai_can_thiep = 'HOAN_TAT')")
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class CanThiepAnToanChuyenDi extends Base {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chuyen_di_id", nullable = false)
    private ChuyenDi chuyenDi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "su_co_chuyen_di_id", nullable = false)
    private SuCoChuyenDi suCoChuyenDi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yeu_cau_muc_tieu_id")
    private YeuCauDiChung yeuCauMucTieu;

    @Column(name = "thu_tu_can_thiep", nullable = false)
    private Long thuTuCanThiep;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_can_thiep", nullable = false, length = 40)
    private LoaiCanThiepAnToan loaiCanThiep;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_can_thiep", nullable = false, length = 40)
    private TrangThaiCanThiepAnToan trangThaiCanThiep;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_khoi_tao_id", nullable = false)
    private NguoiDung nguoiKhoiTao;

    @Column(name = "khoi_tao_luc", nullable = false)
    private Instant khoiTaoLuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_ket_thuc_id")
    private NguoiDung nguoiKetThuc;

    @Column(name = "ket_thuc_luc")
    private Instant ketThucLuc;

    @Column(name = "ly_do_an_toan", nullable = false, length = 1000)
    private String lyDoAnToan;

    @Column(name = "toa_do_xuong_xe_khan_cap", columnDefinition = "geometry(Point,4326)")
    private Point toaDoXuongXeKhanCap;

    @Column(name = "xuong_xe_khan_cap_luc")
    private Instant xuongXeKhanCapLuc;

    @Column(name = "so_khach_thuc_te_luc_bat_dau", nullable = false)
    private Integer soKhachThucTeLucBatDau;

    @Column(name = "so_khach_thuc_te_luc_ket_thuc")
    private Integer soKhachThucTeLucKetThuc;

    @Column(name = "so_xac_thuc_len_xe_vo_hieu_hoa", nullable = false)
    private Integer soXacThucLenXeVoHieuHoa;

    public static CanThiepAnToanChuyenDi hoanTatNgay(
            ChuyenDi trip, SuCoChuyenDi incident, YeuCauDiChung target, long sequence,
            LoaiCanThiepAnToan type, NguoiDung actor, Instant occurredAt, String safeReason,
            int passengerCountBefore, int passengerCountAfter, int invalidatedCredentials) {
        if (type == LoaiCanThiepAnToan.GIU_DE_XUONG_XE_AN_TOAN) {
            throw new IllegalArgumentException("GIU_DE_XUONG_XE_AN_TOAN phải bắt đầu ở trạng thái DANG_THUC_HIEN.");
        }
        CanThiepAnToanChuyenDi x = base(trip, incident, target, sequence, type, actor, occurredAt,
                safeReason, passengerCountBefore);
        x.trangThaiCanThiep = TrangThaiCanThiepAnToan.HOAN_TAT;
        x.nguoiKetThuc = actor;
        x.ketThucLuc = occurredAt;
        x.soKhachThucTeLucKetThuc = passengerCountAfter;
        x.soXacThucLenXeVoHieuHoa = requireNonNegative(invalidatedCredentials, "invalidatedCredentials");
        x.validateShape();
        return x;
    }

    public static CanThiepAnToanChuyenDi batDauGiuDeXuongXeAnToan(
            ChuyenDi trip, SuCoChuyenDi incident, YeuCauDiChung target, long sequence,
            NguoiDung actor, Instant occurredAt, String safeReason, int passengerCountBefore) {
        if (target == null)
            throw new IllegalArgumentException("Safe-exit hold phải có target booking.");
        CanThiepAnToanChuyenDi x = base(trip, incident, target, sequence,
                LoaiCanThiepAnToan.GIU_DE_XUONG_XE_AN_TOAN, actor, occurredAt, safeReason, passengerCountBefore);
        x.trangThaiCanThiep = TrangThaiCanThiepAnToan.DANG_THUC_HIEN;
        x.soXacThucLenXeVoHieuHoa = 0;
        x.validateShape();
        return x;
    }

    public void hoanTatXuongXeAnToan(NguoiDung actor, Instant occurredAt, Point position,
            int passengerCountAfter, int invalidatedCredentials) {
        requireActiveHold();
        Objects.requireNonNull(actor, "actor không được trống");
        Objects.requireNonNull(occurredAt, "occurredAt không được trống");
        if (occurredAt.isBefore(khoiTaoLuc))
            throw new IllegalArgumentException("occurredAt không hợp lệ.");
        if (position == null || position.isEmpty() || position.getSRID() != Wgs84Coordinates.SRID
                || !Wgs84Coordinates.isValidLongitudeLatitude(position.getX(), position.getY())) {
            throw new IllegalArgumentException("Safe-exit position phải là Point WGS84 hợp lệ.");
        }
        Point copy = (Point) position.copy();
        copy.setSRID(Wgs84Coordinates.SRID);
        this.toaDoXuongXeKhanCap = copy;
        this.xuongXeKhanCapLuc = occurredAt;
        this.nguoiKetThuc = actor;
        this.ketThucLuc = occurredAt;
        this.soKhachThucTeLucKetThuc = requireNonNegative(passengerCountAfter, "passengerCountAfter");
        this.soXacThucLenXeVoHieuHoa = requireNonNegative(invalidatedCredentials, "invalidatedCredentials");
        this.trangThaiCanThiep = TrangThaiCanThiepAnToan.HOAN_TAT;
        validateShape();
    }

    public void dungDoChuyenDiKetThuc(NguoiDung actor, Instant occurredAt) {
        requireActiveHold();
        this.nguoiKetThuc = Objects.requireNonNull(actor, "actor không được trống");
        this.ketThucLuc = Objects.requireNonNull(occurredAt, "occurredAt không được trống");
        if (occurredAt.isBefore(khoiTaoLuc))
            throw new IllegalArgumentException("occurredAt không hợp lệ.");
        this.soKhachThucTeLucKetThuc = this.soKhachThucTeLucBatDau;
        this.trangThaiCanThiep = TrangThaiCanThiepAnToan.DUNG_DO_CHUYEN_DI_KET_THUC;
        validateShape();
    }

    public boolean dangThucHien() {
        return trangThaiCanThiep == TrangThaiCanThiepAnToan.DANG_THUC_HIEN;
    }

    private static CanThiepAnToanChuyenDi base(ChuyenDi trip, SuCoChuyenDi incident, YeuCauDiChung target,
            long sequence, LoaiCanThiepAnToan type, NguoiDung actor,
            Instant occurredAt, String safeReason, int countBefore) {
        if (trip == null || trip.getId() == null || incident == null || incident.getId() == null
                || incident.getChuyenDi() == null || !Objects.equals(incident.getChuyenDi().getId(), trip.getId())) {
            throw new IllegalArgumentException("Intervention phải bind đúng persisted Trip + Incident.");
        }
        if (sequence <= 0 || type == null || actor == null || actor.getId() == null || occurredAt == null) {
            throw new IllegalArgumentException("Intervention identity/time không hợp lệ.");
        }
        if (type != LoaiCanThiepAnToan.HUY_CHUYEN_KHAN_CAP && target == null) {
            throw new IllegalArgumentException("Participant intervention phải có target booking.");
        }
        if (target != null && (target.getId() == null || target.getChuyenDi() == null
                || !Objects.equals(target.getChuyenDi().getId(), trip.getId()))) {
            throw new IllegalArgumentException("Target booking phải thuộc Trip.");
        }
        String normalized = safeReason == null ? null : safeReason.trim();
        if (normalized == null || normalized.isEmpty() || normalized.length() > 1000) {
            throw new IllegalArgumentException("lyDoAnToan phải có nội dung participant-safe <= 1000 ký tự.");
        }
        CanThiepAnToanChuyenDi x = new CanThiepAnToanChuyenDi();
        x.chuyenDi = trip;
        x.suCoChuyenDi = incident;
        x.yeuCauMucTieu = target;
        x.thuTuCanThiep = sequence;
        x.loaiCanThiep = type;
        x.nguoiKhoiTao = actor;
        x.khoiTaoLuc = occurredAt;
        x.lyDoAnToan = normalized;
        x.soKhachThucTeLucBatDau = requireNonNegative(countBefore, "passengerCountBefore");
        return x;
    }

    private void requireActiveHold() {
        if (loaiCanThiep != LoaiCanThiepAnToan.GIU_DE_XUONG_XE_AN_TOAN
                || trangThaiCanThiep != TrangThaiCanThiepAnToan.DANG_THUC_HIEN) {
            throw new IllegalStateException("Intervention không phải active safe-exit hold.");
        }
    }

    private void validateShape() {
        if (trangThaiCanThiep == TrangThaiCanThiepAnToan.DANG_THUC_HIEN) {
            if (loaiCanThiep != LoaiCanThiepAnToan.GIU_DE_XUONG_XE_AN_TOAN || ketThucLuc != null || nguoiKetThuc != null
                    || soKhachThucTeLucKetThuc != null || toaDoXuongXeKhanCap != null || xuongXeKhanCapLuc != null) {
                throw new IllegalStateException("Active hold shape không hợp lệ.");
            }
        } else if (ketThucLuc == null || nguoiKetThuc == null || soKhachThucTeLucKetThuc == null) {
            throw new IllegalStateException("Completed/stopped intervention thiếu kết thúc/count truth.");
        }
        if (trangThaiCanThiep == TrangThaiCanThiepAnToan.DUNG_DO_CHUYEN_DI_KET_THUC
                && loaiCanThiep != LoaiCanThiepAnToan.GIU_DE_XUONG_XE_AN_TOAN) {
            throw new IllegalStateException("Chỉ safe-exit hold mới có thể DUNG_DO_CHUYEN_DI_KET_THUC.");
        }
        if (loaiCanThiep != LoaiCanThiepAnToan.GIU_DE_XUONG_XE_AN_TOAN
                && trangThaiCanThiep != TrangThaiCanThiepAnToan.HOAN_TAT) {
            throw new IllegalStateException("Non-hold intervention phải HOAN_TAT.");
        }
    }

    private static int requireNonNegative(int value, String field) {
        if (value < 0)
            throw new IllegalArgumentException(field + " không được âm.");
        return value;
    }
}
