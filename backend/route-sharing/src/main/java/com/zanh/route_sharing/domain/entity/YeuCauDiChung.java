package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.domain.riderequest.RideRequestPolicySnapshot;
import com.zanh.route_sharing.domain.riderequest.RideRequestSnapshot;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

@Entity
@Table(name = "yeu_cau_di_chung", indexes = {
                @Index(name = "idx_yeu_cau_lo_trinh_trang_thai", columnList = "lo_trinh_chia_se_id,trang_thai_yeu_cau"),
                @Index(name = "idx_yeu_cau_chuyen_di", columnList = "chuyen_di_id"),
                @Index(name = "idx_yeu_cau_hanh_khach", columnList = "hanh_khach_id")
}, check = {
                @CheckConstraint(name = "ck_yeu_cau_ty_le", constraint = "ty_le_tien_duong BETWEEN 0 AND 100"),
                @CheckConstraint(name = "ck_yeu_cau_khoang_cach", constraint = "khoang_cach_lech_de_don_met >= 0 "
                                + "AND thoi_gian_lech_de_don_giay >= 0 "
                                + "AND tong_khoang_cach_mong_muon_met > 0 "
                                + "AND khoang_cach_duoc_phuc_vu_met >= 0 "
                                + "AND khoang_cach_con_lai_met >= 0 "
                                + "AND tong_khoang_cach_mong_muon_met = "
                                + "khoang_cach_duoc_phuc_vu_met + khoang_cach_con_lai_met"),
                @CheckConstraint(name = "ck_yeu_cau_matching", constraint = "(loai_ghep_tuyen = 'CUNG_DIEM_DEN' "
                                + "AND loai_diem_tha = 'DIEM_DICH_CUOI_CUNG' "
                                + "AND khoang_cach_con_lai_met = 0) "
                                + "OR (loai_ghep_tuyen = 'TRUNG_DOAN_TUYEN' "
                                + "AND loai_diem_tha = 'DIEM_THA_TRUNG_GIAN' "
                                + "AND khoang_cach_con_lai_met > 0)"),
                @CheckConstraint(name = "ck_yeu_cau_muc_ho_tro", constraint = "muc_ho_tro_hanh_khach_de_nghi >= 0 "
                                + "AND (muc_ho_tro_da_thoa_thuan IS NULL OR muc_ho_tro_da_thoa_thuan >= 0) "
                                + "AND (muc_ho_tro_goi_y_moi_km_luc_gui IS NULL "
                                + "OR muc_ho_tro_goi_y_moi_km_luc_gui >= 0) "
                                + "AND (trang_thai_yeu_cau <> 'PENDING' OR muc_ho_tro_da_thoa_thuan IS NULL)"),
                @CheckConstraint(name = "ck_yeu_cau_thoi_han", constraint = "expires_at > gui_luc "
                                + "AND request_ttl_applied_seconds > 0 "
                                + "AND booking_cutoff_applied_seconds >= 0"),
                @CheckConstraint(name = "ck_yeu_cau_idempotency", constraint = "char_length(idempotency_key) BETWEEN 8 AND 128 "
                                + "AND request_fingerprint ~ '^[0-9a-f]{64}$'"),
                @CheckConstraint(name = "ck_yeu_cau_policy_snapshot", constraint = "ban_kinh_cung_diem_den_luc_gui_met > 0 "
                                + "AND ban_kinh_diem_den_gan_tuyen_luc_gui_met > 0 "
                                + "AND lech_don_toi_da_luc_gui_met >= 0 "
                                + "AND lech_don_toi_da_luc_gui_giay >= 0 "
                                + "AND ty_le_tien_duong_toi_thieu_luc_gui BETWEEN 0 AND 100"),
                @CheckConstraint(name = "ck_yeu_cau_cooldown", constraint = "(tu_choi_luc IS NULL "
                                + "AND rejection_cooldown_applied_seconds IS NULL "
                                + "AND cooldown_until IS NULL "
                                + "AND cau_hinh_id_luc_tu_choi IS NULL "
                                + "AND cau_hinh_version_luc_tu_choi IS NULL) "
                                + "OR (tu_choi_luc IS NOT NULL "
                                + "AND rejection_cooldown_applied_seconds >= 0 "
                                + "AND cooldown_until >= tu_choi_luc "
                                + "AND cau_hinh_id_luc_tu_choi IS NOT NULL "
                                + "AND cau_hinh_version_luc_tu_choi IS NOT NULL)")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YeuCauDiChung extends Base {

        private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{8,128}");
        private static final Pattern FINGERPRINT_PATTERN = Pattern.compile("[0-9a-f]{64}");

        @Enumerated(EnumType.STRING)
        @Column(name = "loai_ghep_tuyen", nullable = false, length = 30)
        private LoaiGhepTuyen loaiGhepTuyen;

        @Column(name = "diem_don_thuc_te", nullable = false, columnDefinition = "geometry(Point,4326)")
        private Point diemDonThucTe;

        @Column(name = "dia_chi_don_thuc_te", nullable = false, length = 500)
        private String diaChiDonThucTe;

        @Column(name = "diem_dich_cuoi_cung_mong_muon", nullable = false, columnDefinition = "geometry(Point,4326)")
        private Point diemDichCuoiCungMongMuon;

        @Column(name = "dia_chi_dich_cuoi_cung", nullable = false, length = 500)
        private String diaChiDichCuoiCung;

        @Column(name = "diem_tha_de_xuat", nullable = false, columnDefinition = "geometry(Point,4326)")
        private Point diemThaDeXuat;

        @Column(name = "dia_chi_diem_tha", nullable = false, length = 500)
        private String diaChiDiemTha;

        @Enumerated(EnumType.STRING)
        @Column(name = "loai_diem_tha", nullable = false, length = 40)
        private LoaiDiemTha loaiDiemTha;

        @Column(name = "tuyen_duong_mong_muon_hanh_khach", nullable = false, columnDefinition = "geometry(LineString,4326)")
        private LineString tuyenDuongMongMuonHanhKhach;

        @Column(name = "doan_tuyen_duoc_phuc_vu", nullable = false, columnDefinition = "geometry(LineString,4326)")
        private LineString doanTuyenDuocPhucVu;

        @Column(name = "ty_le_tien_duong", nullable = false, precision = 5, scale = 2)
        private BigDecimal tyLeTienDuong;

        @Column(name = "khoang_cach_lech_de_don_met", nullable = false, precision = 14, scale = 2)
        private BigDecimal khoangCachLechDeDonMet;

        @Column(name = "thoi_gian_lech_de_don_giay", nullable = false)
        private Long thoiGianLechDeDonGiay;

        @Column(name = "tong_khoang_cach_mong_muon_met", nullable = false, precision = 14, scale = 2)
        private BigDecimal tongKhoangCachMongMuonMet;

        @Column(name = "khoang_cach_duoc_phuc_vu_met", nullable = false, precision = 14, scale = 2)
        private BigDecimal khoangCachDuocPhucVuMet;

        @Column(name = "khoang_cach_con_lai_met", nullable = false, precision = 14, scale = 2)
        private BigDecimal khoangCachConLaiMet;

        @Column(name = "muc_ho_tro_hanh_khach_de_nghi", nullable = false, precision = 15, scale = 2)
        private BigDecimal mucHoTroHanhKhachDeNghi;

        @Column(name = "muc_ho_tro_da_thoa_thuan", precision = 15, scale = 2)
        private BigDecimal mucHoTroDaThoaThuan;

        @Column(name = "ghi_chu", length = 1000)
        private String ghiChu;

        @Enumerated(EnumType.STRING)
        @Column(name = "trang_thai_yeu_cau", nullable = false, length = 40)
        private TrangThaiYeuCau trangThaiYeuCau;

        @Column(name = "gui_luc", nullable = false)
        private Instant guiLuc;

        @Column(name = "expires_at", nullable = false)
        private Instant expiresAt;

        @Column(name = "idempotency_key", nullable = false, length = 128)
        private String idempotencyKey;

        @Column(name = "request_fingerprint", nullable = false, length = 64)
        private String requestFingerprint;

        @Column(name = "route_version_luc_gui", nullable = false)
        private Long routeVersionLucGui;

        @Column(name = "cau_hinh_version_luc_gui", nullable = false)
        private Long cauHinhVersionLucGui;

        @Column(name = "thoi_gian_khoi_hanh_luc_gui", nullable = false)
        private Instant thoiGianKhoiHanhLucGui;

        @Column(name = "request_ttl_applied_seconds", nullable = false)
        private Long requestTtlAppliedSeconds;

        @Column(name = "booking_cutoff_applied_seconds", nullable = false)
        private Long bookingCutoffAppliedSeconds;

        @Column(name = "ban_kinh_cung_diem_den_luc_gui_met", nullable = false, precision = 12, scale = 2)
        private BigDecimal banKinhCungDiemDenLucGuiMet;

        @Column(name = "ban_kinh_diem_den_gan_tuyen_luc_gui_met", nullable = false, precision = 12, scale = 2)
        private BigDecimal banKinhDiemDenGanTuyenLucGuiMet;

        @Column(name = "lech_don_toi_da_luc_gui_met", nullable = false, precision = 12, scale = 2)
        private BigDecimal lechDonToiDaLucGuiMet;

        @Column(name = "lech_don_toi_da_luc_gui_giay", nullable = false)
        private Long lechDonToiDaLucGuiGiay;

        @Column(name = "ty_le_tien_duong_toi_thieu_luc_gui", nullable = false, precision = 5, scale = 2)
        private BigDecimal tyLeTienDuongToiThieuLucGui;

        @Column(name = "muc_ho_tro_goi_y_moi_km_luc_gui", precision = 15, scale = 2)
        private BigDecimal mucHoTroGoiYMoiKmLucGui;

        @Column(name = "tu_choi_luc")
        private Instant tuChoiLuc;

        @Column(name = "rejection_cooldown_applied_seconds")
        private Long rejectionCooldownAppliedSeconds;

        @Column(name = "cooldown_until")
        private Instant cooldownUntil;

        @Column(name = "cau_hinh_version_luc_tu_choi")
        private Long cauHinhVersionLucTuChoi;

        @Column(name = "chap_nhan_luc")
        private Instant chapNhanLuc;

        @Column(name = "huy_luc")
        private Instant huyLuc;

        @Column(name = "ly_do_huy", length = 2000)
        private String lyDoHuy;

        @Column(name = "tai_xe_xac_nhan_don_luc")
        private Instant taiXeXacNhanDonLuc;

        @Column(name = "hanh_khach_xac_nhan_don_luc")
        private Instant hanhKhachXacNhanDonLuc;

        @Column(name = "len_xe_luc")
        private Instant lenXeLuc;

        @Column(name = "tai_xe_xac_nhan_tra_luc")
        private Instant taiXeXacNhanTraLuc;

        @Column(name = "hanh_khach_xac_nhan_tra_luc")
        private Instant hanhKhachXacNhanTraLuc;

        @Column(name = "xuong_xe_luc")
        private Instant xuongXeLuc;

        @Column(name = "ly_do_xac_nhan_that_bai", length = 2000)
        private String lyDoXacNhanThatBai;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "hanh_khach_id", nullable = false)
        private NguoiDung hanhKhach;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "lo_trinh_chia_se_id", nullable = false)
        private LoTrinhChiaSe loTrinhChiaSe;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "tai_xe_id_luc_gui", nullable = false)
        private NguoiDung taiXeLucGui;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "cau_hinh_id_luc_gui", nullable = false)
        private CauHinhNghiepVu cauHinhLucGui;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "cau_hinh_id_luc_tu_choi")
        private CauHinhNghiepVu cauHinhLucTuChoi;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "chuyen_di_id")
        private ChuyenDi chuyenDi;

        public static YeuCauDiChung pending(
                        NguoiDung passenger,
                        LoTrinhChiaSe route,
                        NguoiDung driverAtCreation,
                        CauHinhNghiepVu configurationAtCreation,
                        RideRequestSnapshot snapshot,
                        Instant sentAt,
                        Instant expiresAt,
                        String idempotencyKey,
                        String requestFingerprint,
                        String note) {
                Objects.requireNonNull(passenger, "passenger không được trống");
                Objects.requireNonNull(route, "route không được trống");
                Objects.requireNonNull(driverAtCreation, "driverAtCreation không được trống");
                Objects.requireNonNull(configurationAtCreation, "configurationAtCreation không được trống");
                Objects.requireNonNull(snapshot, "snapshot không được trống");
                Objects.requireNonNull(sentAt, "sentAt không được trống");
                Objects.requireNonNull(expiresAt, "expiresAt không được trống");
                if (!expiresAt.isAfter(sentAt)) {
                        throw new IllegalArgumentException("expiresAt phải sau sentAt");
                }
                if (!IDEMPOTENCY_KEY_PATTERN.matcher(Objects.requireNonNull(idempotencyKey)).matches()) {
                        throw new IllegalArgumentException("idempotencyKey không hợp lệ");
                }
                if (!FINGERPRINT_PATTERN.matcher(Objects.requireNonNull(requestFingerprint)).matches()) {
                        throw new IllegalArgumentException("requestFingerprint không hợp lệ");
                }
                if (snapshot.agreedSupportAmount() != null) {
                        throw new IllegalArgumentException("Yêu cầu PENDING chưa có mức hỗ trợ đã thỏa thuận");
                }

                RideRequestPolicySnapshot policy = snapshot.policy();
                YeuCauDiChung entity = new YeuCauDiChung();
                entity.hanhKhach = passenger;
                entity.loTrinhChiaSe = route;
                entity.taiXeLucGui = driverAtCreation;
                entity.cauHinhLucGui = configurationAtCreation;
                entity.loaiGhepTuyen = snapshot.matchType();
                entity.loaiDiemTha = snapshot.dropoffType();
                entity.diemDonThucTe = snapshot.pickup().point();
                entity.diaChiDonThucTe = snapshot.pickup().address();
                entity.diemDichCuoiCungMongMuon = snapshot.passengerDestination().point();
                entity.diaChiDichCuoiCung = snapshot.passengerDestination().address();
                entity.diemThaDeXuat = snapshot.proposedDropoff().point();
                entity.diaChiDiemTha = snapshot.proposedDropoff().address();
                entity.tuyenDuongMongMuonHanhKhach = snapshot.passengerDesiredRoute();
                entity.doanTuyenDuocPhucVu = snapshot.servedRouteSegment();
                entity.tyLeTienDuong = snapshot.convenienceRatioPercent();
                entity.khoangCachLechDeDonMet = snapshot.pickupDeviationMeters();
                entity.thoiGianLechDeDonGiay = snapshot.pickupDeviationSeconds();
                entity.tongKhoangCachMongMuonMet = snapshot.passengerDesiredDistanceMeters();
                entity.khoangCachDuocPhucVuMet = snapshot.servedDistanceMeters();
                entity.khoangCachConLaiMet = snapshot.remainingDistanceMeters();
                entity.mucHoTroHanhKhachDeNghi = snapshot.proposedSupportAmount();
                entity.mucHoTroDaThoaThuan = null;
                entity.mucHoTroGoiYMoiKmLucGui = snapshot.suggestedSupportPerKm();
                entity.ghiChu = normalizeNote(note);
                entity.trangThaiYeuCau = TrangThaiYeuCau.PENDING;
                entity.guiLuc = sentAt;
                entity.expiresAt = expiresAt;
                entity.idempotencyKey = idempotencyKey;
                entity.requestFingerprint = requestFingerprint;
                entity.routeVersionLucGui = snapshot.routeVersion();
                entity.cauHinhVersionLucGui = policy.configurationVersion();
                entity.thoiGianKhoiHanhLucGui = snapshot.expectedDepartureTime();
                entity.requestTtlAppliedSeconds = policy.requestTtl().toSeconds();
                entity.bookingCutoffAppliedSeconds = policy.bookingCutoff().toSeconds();
                entity.banKinhCungDiemDenLucGuiMet = policy.sameDestinationRadiusMeters();
                entity.banKinhDiemDenGanTuyenLucGuiMet = policy.destinationNearRouteRadiusMeters();
                entity.lechDonToiDaLucGuiMet = policy.maxPickupDeviationMeters();
                entity.lechDonToiDaLucGuiGiay = policy.maxPickupDeviationSeconds();
                entity.tyLeTienDuongToiThieuLucGui = policy.minimumConvenienceRatioPercent();
                return entity;
        }

        private static String normalizeNote(String note) {
                if (note == null || note.isBlank()) {
                        return null;
                }
                String normalized = note.trim();
                if (normalized.length() > 1000) {
                        throw new IllegalArgumentException("note không được vượt quá 1000 ký tự");
                }
                return normalized;
        }
}
