package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
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
import lombok.Setter;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Objects;

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
        @CheckConstraint(name = "ck_yeu_cau_booking_policy", constraint = "booking_cutoff_applied_seconds >= 0"),
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
                + "AND cau_hinh_version_luc_tu_choi IS NOT NULL)"),
        @CheckConstraint(name = "ck_yeu_cau_huy", constraint = "(huy_luc IS NULL "
                + "AND ly_do_huy IS NULL "
                + "AND trang_thai_yeu_cau NOT IN ('CANCELLED_BY_PASSENGER', 'CANCELLED_BY_DRIVER')) "
                + "OR (huy_luc IS NOT NULL "
                + "AND ly_do_huy IS NOT NULL "
                + "AND length(trim(ly_do_huy)) BETWEEN 1 AND 2000 "
                + "AND trang_thai_yeu_cau IN ('CANCELLED_BY_PASSENGER', 'CANCELLED_BY_DRIVER'))"),
        @CheckConstraint(name = "ck_yeu_cau_no_show", constraint = "(trang_thai_yeu_cau <> 'NO_SHOW' OR khong_den_luc IS NOT NULL) "
                + "AND (khong_den_luc IS NULL OR (chap_nhan_luc IS NOT NULL AND len_xe_luc IS NULL AND khong_den_luc >= chap_nhan_luc))")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YeuCauDiChung extends Base {

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
    @Column(name = "route_version_luc_gui", nullable = false)
    private Long routeVersionLucGui;
    @Column(name = "cau_hinh_version_luc_gui", nullable = false)
    private Long cauHinhVersionLucGui;
    @Column(name = "thoi_gian_khoi_hanh_luc_gui", nullable = false)
    private Instant thoiGianKhoiHanhLucGui;
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
    @Column(name = "khong_den_luc")
    private Instant khongDenLuc;
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
            String note) {
        Objects.requireNonNull(passenger, "passenger không được trống");
        Objects.requireNonNull(route, "route không được trống");
        Objects.requireNonNull(driverAtCreation, "driverAtCreation không được trống");
        Objects.requireNonNull(configurationAtCreation, "configurationAtCreation không được trống");
        Objects.requireNonNull(snapshot, "snapshot không được trống");
        Objects.requireNonNull(sentAt, "sentAt không được trống");
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
        entity.routeVersionLucGui = snapshot.routeVersion();
        entity.cauHinhVersionLucGui = policy.configurationVersion();
        entity.thoiGianKhoiHanhLucGui = snapshot.expectedDepartureTime();
        entity.bookingCutoffAppliedSeconds = policy.bookingCutoff().toSeconds();
        entity.banKinhCungDiemDenLucGuiMet = policy.sameDestinationRadiusMeters();
        entity.banKinhDiemDenGanTuyenLucGuiMet = policy.destinationNearRouteRadiusMeters();
        entity.lechDonToiDaLucGuiMet = policy.maxPickupDeviationMeters();
        entity.lechDonToiDaLucGuiGiay = policy.maxPickupDeviationSeconds();
        entity.tyLeTienDuongToiThieuLucGui = policy.minimumConvenienceRatioPercent();
        return entity;
    }

    public void accept(Instant acceptedAt) {
        Objects.requireNonNull(acceptedAt, "acceptedAt không được trống");
        requirePendingState();
        this.trangThaiYeuCau = TrangThaiYeuCau.ACCEPTED;
        this.chapNhanLuc = acceptedAt;
        this.mucHoTroDaThoaThuan = Objects.requireNonNull(
                this.mucHoTroHanhKhachDeNghi,
                "Mức hỗ trợ hành khách đề nghị không được trống");
    }

    public void reject(Instant rejectedAt, CauHinhNghiepVu currentConfiguration, long rejectionCooldownSeconds) {
        Objects.requireNonNull(rejectedAt, "rejectedAt không được trống");
        Objects.requireNonNull(currentConfiguration, "currentConfiguration không được trống");
        if (currentConfiguration.getId() == null || currentConfiguration.getVersion() == null) {
            throw new IllegalArgumentException("Cấu hình từ chối phải là bản ghi đã được lưu.");
        }
        if (rejectionCooldownSeconds < 0) {
            throw new IllegalArgumentException("rejectionCooldownSeconds không được âm");
        }
        requirePendingState();
        final Instant calculatedCooldownUntil;
        try {
            calculatedCooldownUntil = rejectedAt.plusSeconds(rejectionCooldownSeconds);
        } catch (DateTimeException | ArithmeticException exception) {
            throw new IllegalArgumentException("Không thể tính thời điểm kết thúc cooldown.", exception);
        }
        this.trangThaiYeuCau = TrangThaiYeuCau.REJECTED;
        this.tuChoiLuc = rejectedAt;
        this.rejectionCooldownAppliedSeconds = rejectionCooldownSeconds;
        this.cooldownUntil = calculatedCooldownUntil;
        this.cauHinhLucTuChoi = currentConfiguration;
        this.cauHinhVersionLucTuChoi = currentConfiguration.getVersion();
        this.mucHoTroDaThoaThuan = null;
    }

    public void assignToTrip(ChuyenDi trip) {
        Objects.requireNonNull(trip, "trip không được trống");
        if (this.trangThaiYeuCau != TrangThaiYeuCau.ACCEPTED) {
            throw new IllegalStateException("Chỉ booking ACCEPTED mới được gắn vào chuyến đi.");
        }
        if (this.chuyenDi != null) {
            throw new IllegalStateException("Booking đã thuộc một chuyến đi thực tế.");
        }
        if (trip.getLoTrinhChiaSe() == null || this.loTrinhChiaSe == null
                || !Objects.equals(trip.getLoTrinhChiaSe().getId(), this.loTrinhChiaSe.getId())) {
            throw new IllegalArgumentException("Booking và chuyến đi phải thuộc cùng lộ trình chia sẻ.");
        }
        this.chuyenDi = trip;
    }

    public void board(ChuyenDi trip, Instant boardedAt) {
        Objects.requireNonNull(trip, "trip không được trống");
        Objects.requireNonNull(boardedAt, "boardedAt không được trống");
        if (this.trangThaiYeuCau != TrangThaiYeuCau.ACCEPTED) {
            throw new IllegalStateException("Chỉ booking ACCEPTED mới có thể chuyển sang ON_BOARD.");
        }
        if (this.chuyenDi == null || this.chuyenDi.getId() == null || trip.getId() == null
                || !Objects.equals(this.chuyenDi.getId(), trip.getId())) {
            throw new IllegalStateException("Booking phải thuộc chính chuyến đang xác nhận Boarding.");
        }
        if (this.lenXeLuc != null) {
            throw new IllegalStateException("Booking đã có thời điểm lên xe trước đó.");
        }
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS) {
            throw new IllegalStateException("Chuyến phải IN_PROGRESS khi Passenger lên xe.");
        }
        this.trangThaiYeuCau = TrangThaiYeuCau.ON_BOARD;
        this.lenXeLuc = boardedAt;
    }

    public void markNoShow(ChuyenDi trip, Instant noShowAt) {
        Objects.requireNonNull(trip, "trip không được trống");
        Objects.requireNonNull(noShowAt, "noShowAt không được trống");
        if (this.trangThaiYeuCau != TrangThaiYeuCau.ACCEPTED) {
            throw new IllegalStateException("Chỉ booking ACCEPTED mới có thể chuyển sang NO_SHOW.");
        }
        if (this.chuyenDi == null || this.chuyenDi.getId() == null || trip.getId() == null
                || !Objects.equals(this.chuyenDi.getId(), trip.getId())) {
            throw new IllegalStateException("Booking phải thuộc chính chuyến đang xác nhận No-show.");
        }
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS) {
            throw new IllegalStateException("Chuyến phải IN_PROGRESS khi xác nhận No-show.");
        }
        if (this.chapNhanLuc == null || noShowAt.isBefore(this.chapNhanLuc)) {
            throw new IllegalArgumentException("noShowAt không hợp lệ so với acceptedAt.");
        }
        if (this.lenXeLuc != null || this.khongDenLuc != null) {
            throw new IllegalStateException("Booking đã được resolve trước đó.");
        }
        this.trangThaiYeuCau = TrangThaiYeuCau.NO_SHOW;
        this.khongDenLuc = noShowAt;
    }

    public TrangThaiYeuCau abortForSafety(ChuyenDi trip) {
        Objects.requireNonNull(trip, "trip không được trống");
        if (this.trangThaiYeuCau != TrangThaiYeuCau.ACCEPTED && this.trangThaiYeuCau != TrangThaiYeuCau.ON_BOARD) {
            throw new IllegalStateException("Chỉ booking ACCEPTED/ON_BOARD mới được emergency-abort.");
        }
        if (this.chuyenDi == null || this.chuyenDi.getId() == null || trip.getId() == null
                || !Objects.equals(this.chuyenDi.getId(), trip.getId())) {
            throw new IllegalStateException("Booking phải thuộc chính Trip đang can thiệp an toàn.");
        }
        TrangThaiYeuCau previous = this.trangThaiYeuCau;
        if (previous == TrangThaiYeuCau.ON_BOARD && this.lenXeLuc == null) {
            throw new IllegalStateException("Booking ON_BOARD phải giữ boardedAt lịch sử.");
        }
        if (previous == TrangThaiYeuCau.ACCEPTED && this.lenXeLuc != null) {
            throw new IllegalStateException("Booking ACCEPTED không được có boardedAt.");
        }
        this.trangThaiYeuCau = TrangThaiYeuCau.ABORTED;
        return previous;
    }

    public TrangThaiYeuCau cancelByPassenger(Instant cancelledAt, String reason) {
        Objects.requireNonNull(cancelledAt, "cancelledAt không được trống");
        if (this.trangThaiYeuCau != TrangThaiYeuCau.PENDING
                && this.trangThaiYeuCau != TrangThaiYeuCau.ACCEPTED) {
            throw new IllegalStateException("Hành khách chỉ được hủy yêu cầu PENDING hoặc ACCEPTED.");
        }
        requireNotAssignedToTrip();
        String normalizedReason = normalizeCancellationReason(reason);
        TrangThaiYeuCau previous = this.trangThaiYeuCau;
        this.trangThaiYeuCau = TrangThaiYeuCau.CANCELLED_BY_PASSENGER;
        this.huyLuc = cancelledAt;
        this.lyDoHuy = normalizedReason;
        return previous;
    }

    public TrangThaiYeuCau cancelBecauseRouteCancelledByDriver(Instant cancelledAt, String reason) {
        Objects.requireNonNull(cancelledAt, "cancelledAt không được trống");
        if (this.trangThaiYeuCau != TrangThaiYeuCau.PENDING
                && this.trangThaiYeuCau != TrangThaiYeuCau.ACCEPTED) {
            throw new IllegalStateException(
                    "Chỉ yêu cầu PENDING hoặc ACCEPTED mới được kết thúc khi lộ trình bị hủy.");
        }
        requireNotAssignedToTrip();
        String normalizedReason = normalizeCancellationReason(reason);
        TrangThaiYeuCau previous = this.trangThaiYeuCau;
        this.trangThaiYeuCau = TrangThaiYeuCau.CANCELLED_BY_DRIVER;
        this.huyLuc = cancelledAt;
        this.lyDoHuy = normalizedReason;
        return previous;
    }

    public void cancelBecauseTripCancelledBeforeStart(ChuyenDi trip, Instant cancelledAt, String reason) {
        Objects.requireNonNull(trip, "trip không được trống");
        Objects.requireNonNull(cancelledAt, "cancelledAt không được trống");
        if (this.trangThaiYeuCau != TrangThaiYeuCau.ACCEPTED) {
            throw new IllegalStateException("Chỉ booking ACCEPTED mới được kết thúc khi chuyến bị hủy trước Start.");
        }
        if (this.chuyenDi == null || !Objects.equals(this.chuyenDi.getId(), trip.getId())) {
            throw new IllegalStateException("Booking phải đang thuộc chính chuyến bị hủy.");
        }
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.CANCELLED_BEFORE_START) {
            throw new IllegalStateException("Chuyến phải ở trạng thái CANCELLED_BEFORE_START.");
        }
        String normalizedReason = normalizeCancellationReason(reason);
        this.trangThaiYeuCau = TrangThaiYeuCau.CANCELLED_BY_DRIVER;
        this.huyLuc = cancelledAt;
        this.lyDoHuy = normalizedReason;
    }

    private void requirePendingState() {
        if (this.trangThaiYeuCau != TrangThaiYeuCau.PENDING) {
            throw new IllegalStateException("Chỉ yêu cầu PENDING mới được xử lý.");
        }
    }

    private void requireNotAssignedToTrip() {
        if (this.chuyenDi != null) {
            throw new IllegalStateException("Yêu cầu đã thuộc một chuyến đi thực tế.");
        }
    }

    private static String normalizeCancellationReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do hủy không được để trống.");
        }
        String normalized = reason.trim();
        if (normalized.length() > 2000) {
            throw new IllegalArgumentException("Lý do hủy không được vượt quá 2000 ký tự.");
        }
        return normalized;
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
