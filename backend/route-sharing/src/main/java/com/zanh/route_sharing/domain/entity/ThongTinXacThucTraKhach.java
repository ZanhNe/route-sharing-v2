package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.security.dropoff.model.ProtectedDropoffCode;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "thong_tin_xac_thuc_tra_khach", uniqueConstraints = {
        @UniqueConstraint(name = "uk_xac_thuc_tra_khach_dropoff", columnNames = "diem_dung_hanh_trinh_id")
}, indexes = @Index(name = "idx_xac_thuc_tra_khach_can_thiep", columnList = "can_thiep_an_toan_vo_hieu_hoa_id"), check = {
        @CheckConstraint(name = "ck_xac_thuc_tra_khach_material", constraint = "(vo_hieu_hoa_luc IS NULL AND ma_ma_hoa IS NOT NULL AND nonce_ma_hoa IS NOT NULL AND phien_ban_khoa IS NOT NULL) OR (vo_hieu_hoa_luc IS NOT NULL AND ma_ma_hoa IS NULL AND nonce_ma_hoa IS NULL AND phien_ban_khoa IS NULL)"),
        @CheckConstraint(name = "ck_xac_thuc_tra_khach_time", constraint = "vo_hieu_hoa_luc IS NULL OR vo_hieu_hoa_luc >= kich_hoat_luc")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ThongTinXacThucTraKhach extends Base {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chuyen_di_id", nullable = false)
    private ChuyenDi chuyenDi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yeu_cau_di_chung_id", nullable = false)
    private YeuCauDiChung yeuCauDiChung;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diem_dung_hanh_trinh_id", nullable = false)
    private DiemDungHanhTrinh diemDungHanhTrinh;

    @Column(name = "ma_ma_hoa", columnDefinition = "bytea")
    private byte[] maMaHoa;

    @Column(name = "nonce_ma_hoa", columnDefinition = "bytea")
    private byte[] nonceMaHoa;

    @Column(name = "phien_ban_khoa", length = 64)
    private String phienBanKhoa;

    @Column(name = "kich_hoat_luc", nullable = false)
    private Instant kichHoatLuc;

    @Column(name = "vo_hieu_hoa_luc")
    private Instant voHieuHoaLuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "can_thiep_an_toan_vo_hieu_hoa_id")
    private CanThiepAnToanChuyenDi canThiepAnToanVoHieuHoa;

    public static ThongTinXacThucTraKhach activate(ChuyenDi trip, YeuCauDiChung booking,
            DiemDungHanhTrinh dropoff, ProtectedDropoffCode protectedCode, Instant activatedAt) {
        Objects.requireNonNull(trip, "trip không được trống");
        Objects.requireNonNull(booking, "booking không được trống");
        Objects.requireNonNull(dropoff, "dropoff không được trống");
        Objects.requireNonNull(protectedCode, "protectedCode không được trống");
        Objects.requireNonNull(activatedAt, "activatedAt không được trống");
        if (trip.getId() == null || booking.getId() == null || dropoff.getId() == null) {
            throw new IllegalArgumentException("Trip/booking/dropoff phải được lưu trước khi tạo dropoff credential.");
        }
        if (booking.getChuyenDi() == null || !Objects.equals(booking.getChuyenDi().getId(), trip.getId())
                || dropoff.getChuyenDi() == null || !Objects.equals(dropoff.getChuyenDi().getId(), trip.getId())
                || dropoff.getYeuCauDiChung() == null
                || !Objects.equals(dropoff.getYeuCauDiChung().getId(), booking.getId())) {
            throw new IllegalArgumentException("Dropoff credential phải bind đúng Trip + booking + dropoff.");
        }
        if (trip.getTrangThaiVanHanh() != TrangThaiVanHanhChuyenDi.IN_PROGRESS
                || trip.getBatDauLuc() == null || trip.getKetThucLuc() != null
                || trip.getLoTrinhChiaSe() == null
                || trip.getLoTrinhChiaSe().getTrangThaiLoTrinh() != TrangThaiLoTrinh.LOCKED
                || trip.getLoTrinhChiaSe().getChuyenDi() != trip) {
            throw new IllegalStateException(
                    "Dropoff credential chỉ được active cho Trip IN_PROGRESS trên Route LOCKED đúng binding.");
        }
        if (dropoff.getLoaiDiemDung() != LoaiDiemDung.DROPOFF
                || dropoff.getTrangThaiDiemDung() != TrangThaiDiemDung.ARRIVED
                || booking.getTrangThaiYeuCau() != TrangThaiYeuCau.ON_BOARD) {
            throw new IllegalStateException(
                    "Chỉ DROPOFF ARRIVED của booking ON_BOARD mới có dropoff credential active.");
        }
        if (dropoff.getDenLuc() == null || activatedAt.isBefore(dropoff.getDenLuc())) {
            throw new IllegalArgumentException("Dropoff credential chỉ được kích hoạt sau khi DROPOFF đã ARRIVED.");
        }
        ThongTinXacThucTraKhach credential = new ThongTinXacThucTraKhach();
        credential.chuyenDi = trip;
        credential.yeuCauDiChung = booking;
        credential.diemDungHanhTrinh = dropoff;
        credential.maMaHoa = protectedCode.encryptedCode();
        credential.nonceMaHoa = protectedCode.nonce();
        credential.phienBanKhoa = protectedCode.keyVersion();
        credential.kichHoatLuc = activatedAt;
        return credential;
    }

    public boolean isActive() {
        return voHieuHoaLuc == null && maMaHoa != null && maMaHoa.length > 0
                && nonceMaHoa != null && nonceMaHoa.length == 12
                && phienBanKhoa != null && !phienBanKhoa.isBlank();
    }

    public ProtectedDropoffCode protectedCode() {
        if (!isActive())
            throw new IllegalStateException("Dropoff credential không còn active.");
        return new ProtectedDropoffCode(maMaHoa, nonceMaHoa, phienBanKhoa);
    }

    public void resolveForSafety(Instant resolvedAt, CanThiepAnToanChuyenDi intervention) {
        Objects.requireNonNull(intervention, "intervention không được trống");
        resolve(resolvedAt);
        this.canThiepAnToanVoHieuHoa = intervention;
    }

    public void resolve(Instant resolvedAt) {
        Objects.requireNonNull(resolvedAt, "resolvedAt không được trống");
        if (!isActive())
            throw new IllegalStateException("Dropoff credential đã được resolve trước đó.");
        if (kichHoatLuc == null || resolvedAt.isBefore(kichHoatLuc))
            throw new IllegalArgumentException("resolvedAt không hợp lệ.");
        if (maMaHoa != null)
            Arrays.fill(maMaHoa, (byte) 0);
        if (nonceMaHoa != null)
            Arrays.fill(nonceMaHoa, (byte) 0);
        maMaHoa = null;
        nonceMaHoa = null;
        phienBanKhoa = null;
        voHieuHoaLuc = resolvedAt;
    }
}
