package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.security.boarding.model.ProtectedBoardingCode;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "thong_tin_xac_thuc_len_xe", uniqueConstraints = {
        @UniqueConstraint(name = "uk_xac_thuc_len_xe_pickup", columnNames = "diem_dung_hanh_trinh_id")
}, check = {
        @CheckConstraint(name = "ck_xac_thuc_len_xe_material", constraint = "(vo_hieu_hoa_luc IS NULL AND ma_ma_hoa IS NOT NULL AND nonce_ma_hoa IS NOT NULL AND phien_ban_khoa IS NOT NULL) OR (vo_hieu_hoa_luc IS NOT NULL AND ma_ma_hoa IS NULL AND nonce_ma_hoa IS NULL AND phien_ban_khoa IS NULL)"),
        @CheckConstraint(name = "ck_xac_thuc_len_xe_time", constraint = "vo_hieu_hoa_luc IS NULL OR vo_hieu_hoa_luc >= kich_hoat_luc")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ThongTinXacThucLenXe extends Base {

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

    public static ThongTinXacThucLenXe activate(
            ChuyenDi trip,
            YeuCauDiChung booking,
            DiemDungHanhTrinh pickup,
            ProtectedBoardingCode protectedCode,
            Instant activatedAt) {
        Objects.requireNonNull(trip, "trip không được trống");
        Objects.requireNonNull(booking, "booking không được trống");
        Objects.requireNonNull(pickup, "pickup không được trống");
        Objects.requireNonNull(protectedCode, "protectedCode không được trống");
        Objects.requireNonNull(activatedAt, "activatedAt không được trống");
        if (trip.getId() == null || booking.getId() == null || pickup.getId() == null) {
            throw new IllegalArgumentException("Trip/booking/pickup phải được lưu trước khi tạo boarding credential.");
        }
        if (booking.getChuyenDi() == null || !Objects.equals(booking.getChuyenDi().getId(), trip.getId())
                || pickup.getChuyenDi() == null || !Objects.equals(pickup.getChuyenDi().getId(), trip.getId())
                || pickup.getYeuCauDiChung() == null || !Objects.equals(pickup.getYeuCauDiChung().getId(), booking.getId())) {
            throw new IllegalArgumentException("Boarding credential phải bind đúng Trip + booking + pickup.");
        }
        if (pickup.getLoaiDiemDung() != LoaiDiemDung.PICKUP
                || pickup.getTrangThaiDiemDung() != TrangThaiDiemDung.ARRIVED
                || booking.getTrangThaiYeuCau() != TrangThaiYeuCau.ACCEPTED) {
            throw new IllegalStateException("Chỉ pickup ARRIVED của booking ACCEPTED mới có boarding credential active.");
        }
        if (pickup.getDenLuc() == null || activatedAt.isBefore(pickup.getDenLuc())) {
            throw new IllegalArgumentException("Boarding credential chỉ được kích hoạt sau khi pickup đã ARRIVED.");
        }
        ThongTinXacThucLenXe credential = new ThongTinXacThucLenXe();
        credential.chuyenDi = trip;
        credential.yeuCauDiChung = booking;
        credential.diemDungHanhTrinh = pickup;
        credential.maMaHoa = protectedCode.encryptedCode();
        credential.nonceMaHoa = protectedCode.nonce();
        credential.phienBanKhoa = protectedCode.keyVersion();
        credential.kichHoatLuc = activatedAt;
        return credential;
    }

    public boolean isActive() {
        return voHieuHoaLuc == null
                && maMaHoa != null && maMaHoa.length > 0
                && nonceMaHoa != null && nonceMaHoa.length == 12
                && phienBanKhoa != null && !phienBanKhoa.isBlank();
    }

    public ProtectedBoardingCode protectedCode() {
        if (!isActive()) {
            throw new IllegalStateException("Boarding credential không còn active.");
        }
        return new ProtectedBoardingCode(maMaHoa, nonceMaHoa, phienBanKhoa);
    }

    public void resolve(Instant resolvedAt) {
        Objects.requireNonNull(resolvedAt, "resolvedAt không được trống");
        if (!isActive()) {
            throw new IllegalStateException("Boarding credential đã được resolve trước đó.");
        }
        if (kichHoatLuc == null || resolvedAt.isBefore(kichHoatLuc)) {
            throw new IllegalArgumentException("resolvedAt không hợp lệ.");
        }
        if (maMaHoa != null) {
            Arrays.fill(maMaHoa, (byte) 0);
        }
        if (nonceMaHoa != null) {
            Arrays.fill(nonceMaHoa, (byte) 0);
        }
        maMaHoa = null;
        nonceMaHoa = null;
        phienBanKhoa = null;
        voHieuHoaLuc = resolvedAt;
    }
}
