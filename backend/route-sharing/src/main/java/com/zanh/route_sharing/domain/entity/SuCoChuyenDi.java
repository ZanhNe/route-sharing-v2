package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.LoaiSuCo;
import com.zanh.route_sharing.domain.enums.MucDoSuCo;
import com.zanh.route_sharing.domain.enums.NguonPhatHienSuCo;
import com.zanh.route_sharing.domain.enums.TrangThaiGiamSatChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "su_co_chuyen_di", indexes = {
        @Index(name = "idx_su_co_chuyen_di_chuyen", columnList = "chuyen_di_id"),
        @Index(name = "idx_su_co_chuyen_di_trang_thai", columnList = "trang_thai_xu_ly,muc_do"),
        @Index(name = "idx_su_co_chuyen_di_reporter_trip", columnList = "nguoi_bao_cao_id,chuyen_di_id,loai_su_co,trang_thai_xu_ly")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class SuCoChuyenDi extends Base {
    private static final Set<LoaiSuCo> MANUAL_REPORT_TYPES = Set.of(
            LoaiSuCo.ROUTE_DEVIATION,
            LoaiSuCo.TECHNICAL_INCIDENT,
            LoaiSuCo.HARASSMENT_REPORT,
            LoaiSuCo.OTHER,
            LoaiSuCo.SOS);

    @Enumerated(EnumType.STRING)
    @Column(name = "nguon_phat_hien", nullable = false, length = 20)
    private NguonPhatHienSuCo nguonPhatHien;
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_su_co", nullable = false, length = 50)
    private LoaiSuCo loaiSuCo;
    @Enumerated(EnumType.STRING)
    @Column(name = "muc_do", nullable = false, length = 20)
    private MucDoSuCo mucDo;
    @Column(name = "xay_ra_luc")
    private Instant xayRaLuc;
    @Column(name = "bao_cao_luc", nullable = false)
    private Instant baoCaoLuc;
    @Column(name = "toa_do_xay_ra", columnDefinition = "geometry(Point,4326)")
    private Point toaDoXayRa;
    @Column(name = "noi_dung", length = 5000)
    private String noiDung;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_xu_ly", nullable = false, length = 30)
    private TrangThaiXuLySuCo trangThaiXuLy = TrangThaiXuLySuCo.OPEN;
    @Column(name = "tiep_nhan_luc")
    private Instant tiepNhanLuc;
    @Column(name = "ket_luan", length = 5000)
    private String ketLuan;
    @Column(name = "giai_quyet_luc")
    private Instant giaiQuyetLuc;

    @Column(name = "toa_do_tham_chieu_van_hanh", columnDefinition = "geometry(Point,4326)")
    private Point toaDoThamChieuVanHanh;
    @Column(name = "vi_tri_tham_chieu_quan_sat_luc")
    private Instant viTriThamChieuQuanSatLuc;
    @Column(name = "vi_tri_tham_chieu_nhan_luc")
    private Instant viTriThamChieuNhanLuc;
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_giam_sat_luc_bao_cao", length = 30)
    private TrangThaiGiamSatChuyenDi trangThaiGiamSatLucBaoCao;
    @Column(name = "tin_hieu_tham_chieu_luc")
    private Instant tinHieuThamChieuLuc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chuyen_di_id", nullable = false)
    private ChuyenDi chuyenDi;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yeu_cau_di_chung_id")
    private YeuCauDiChung yeuCauDiChung;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_bao_cao_id")
    private NguoiDung nguoiBaoCao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_bi_bao_cao_id")
    private NguoiDung nguoiBiBaoCao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_tiep_nhan_id")
    private NguoiDung nguoiTiepNhan;

    public static SuCoChuyenDi participantReported(
            ChuyenDi trip,
            YeuCauDiChung relatedBooking,
            NguoiDung reporter,
            NguoiDung reportedParticipant,
            NguonPhatHienSuCo source,
            LoaiSuCo type,
            String description,
            Instant reportedAt,
            Point operationalLocation,
            Instant locationObservedAt,
            Instant locationReceivedAt,
            TrangThaiGiamSatChuyenDi monitoringStatus,
            Instant signalReferenceAt) {
        if (trip == null || trip.getId() == null) {
            throw new IllegalArgumentException("Trip phải được lưu trước khi tạo incident.");
        }
        if (reporter == null || reporter.getId() == null) {
            throw new IllegalArgumentException("Reporter phải được xác định.");
        }
        if (source != NguonPhatHienSuCo.DRIVER && source != NguonPhatHienSuCo.PASSENGER) {
            throw new IllegalArgumentException("E6-04 chỉ nhận incident do Driver/Passenger chủ động báo.");
        }
        if (type == null || !MANUAL_REPORT_TYPES.contains(type)) {
            throw new IllegalArgumentException("Incident type không thuộc E6-04 manual reporting scope.");
        }
        if (reportedAt == null) {
            throw new IllegalArgumentException("reportedAt không được trống.");
        }
        String content = normalizeContent(type, description);
        Point locationCopy = copyLocation(operationalLocation, locationObservedAt, locationReceivedAt);
        if (monitoringStatus == null || signalReferenceAt == null || signalReferenceAt.isAfter(reportedAt)) {
            throw new IllegalArgumentException("Monitoring context tại thời điểm báo không hợp lệ.");
        }

        SuCoChuyenDi incident = new SuCoChuyenDi();
        incident.nguonPhatHien = source;
        incident.loaiSuCo = type;
        incident.mucDo = type == LoaiSuCo.SOS ? MucDoSuCo.CRITICAL : MucDoSuCo.WARNING;
        // E6-04 chỉ sở hữu thời điểm báo; occurrence-time thật không được suy diễn từ report time.
        incident.xayRaLuc = null;
        incident.baoCaoLuc = reportedAt;
        incident.toaDoXayRa = null;
        incident.noiDung = content;
        incident.trangThaiXuLy = TrangThaiXuLySuCo.OPEN;
        incident.chuyenDi = trip;
        incident.yeuCauDiChung = relatedBooking;
        incident.nguoiBaoCao = reporter;
        incident.nguoiBiBaoCao = reportedParticipant;
        incident.toaDoThamChieuVanHanh = locationCopy;
        incident.viTriThamChieuQuanSatLuc = locationCopy == null ? null : locationObservedAt;
        incident.viTriThamChieuNhanLuc = locationCopy == null ? null : locationReceivedAt;
        incident.trangThaiGiamSatLucBaoCao = monitoringStatus;
        incident.tinHieuThamChieuLuc = signalReferenceAt;
        return incident;
    }


    public void acknowledge(NguoiDung handler, Instant acknowledgedAt) {
        if (this.trangThaiXuLy != TrangThaiXuLySuCo.OPEN) {
            throw new IllegalStateException("Incident không ở trạng thái OPEN để tiếp nhận.");
        }
        if (handler == null || handler.getId() == null || acknowledgedAt == null) {
            throw new IllegalArgumentException("Handler/acknowledgedAt không hợp lệ.");
        }
        if (this.nguoiTiepNhan != null || this.tiepNhanLuc != null) {
            throw new IllegalStateException("Incident đã có người tiếp nhận trước đó.");
        }
        this.nguoiTiepNhan = handler;
        this.tiepNhanLuc = acknowledgedAt;
        this.trangThaiXuLy = TrangThaiXuLySuCo.ACKNOWLEDGED;
    }

    public void beginInvestigation(NguoiDung handler) {
        requireCurrentHandler(handler);
        if (this.trangThaiXuLy != TrangThaiXuLySuCo.ACKNOWLEDGED) {
            throw new IllegalStateException("Incident phải ACKNOWLEDGED trước khi chuyển INVESTIGATING.");
        }
        this.trangThaiXuLy = TrangThaiXuLySuCo.INVESTIGATING;
    }

    public void reassign(NguoiDung newHandler) {
        if (this.trangThaiXuLy != TrangThaiXuLySuCo.ACKNOWLEDGED
                && this.trangThaiXuLy != TrangThaiXuLySuCo.INVESTIGATING) {
            throw new IllegalStateException("Incident không ở trạng thái cho phép chuyển người xử lý.");
        }
        if (this.nguoiTiepNhan == null || newHandler == null || newHandler.getId() == null) {
            throw new IllegalArgumentException("Handler chuyển giao không hợp lệ.");
        }
        this.nguoiTiepNhan = newHandler;
    }

    public void finalizeHandling(NguoiDung handler, TrangThaiXuLySuCo outcome, String safeConclusion, Instant resolvedAt) {
        requireCurrentHandler(handler);
        if (this.trangThaiXuLy != TrangThaiXuLySuCo.ACKNOWLEDGED
                && this.trangThaiXuLy != TrangThaiXuLySuCo.INVESTIGATING) {
            throw new IllegalStateException("Incident không ở trạng thái có thể kết thúc xử lý.");
        }
        if (outcome != TrangThaiXuLySuCo.RESOLVED && outcome != TrangThaiXuLySuCo.FALSE_ALARM) {
            throw new IllegalArgumentException("Terminal outcome không hợp lệ.");
        }
        String normalized = safeConclusion == null ? null : safeConclusion.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("safeConclusion không được trống.");
        }
        if (normalized.length() > 5000) {
            throw new IllegalArgumentException("safeConclusion không được vượt quá 5000 ký tự.");
        }
        if (resolvedAt == null) {
            throw new IllegalArgumentException("resolvedAt không được trống.");
        }
        this.trangThaiXuLy = outcome;
        this.ketLuan = normalized;
        this.giaiQuyetLuc = resolvedAt;
    }

    public boolean isTerminalHandlingState() {
        return this.trangThaiXuLy == TrangThaiXuLySuCo.RESOLVED
                || this.trangThaiXuLy == TrangThaiXuLySuCo.FALSE_ALARM
                || this.trangThaiXuLy == TrangThaiXuLySuCo.CLOSED;
    }

    private void requireCurrentHandler(NguoiDung handler) {
        if (handler == null || handler.getId() == null || this.nguoiTiepNhan == null
                || !java.util.Objects.equals(this.nguoiTiepNhan.getId(), handler.getId())) {
            throw new IllegalStateException("Actor không phải current incident handler.");
        }
    }

    public boolean isUnresolved() {
        return trangThaiXuLy == TrangThaiXuLySuCo.OPEN
                || trangThaiXuLy == TrangThaiXuLySuCo.ACKNOWLEDGED
                || trangThaiXuLy == TrangThaiXuLySuCo.INVESTIGATING;
    }

    private static String normalizeContent(LoaiSuCo type, String description) {
        String normalized = description == null ? null : description.trim();
        if (type == LoaiSuCo.SOS && (normalized == null || normalized.isEmpty())) {
            return null;
        }
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("Ordinary incident phải có description.");
        }
        if (normalized.length() > 5000) {
            throw new IllegalArgumentException("Incident description không được vượt quá 5000 ký tự.");
        }
        return normalized;
    }

    private static Point copyLocation(Point point, Instant observedAt, Instant receivedAt) {
        if (point == null) {
            if (observedAt != null || receivedAt != null) {
                throw new IllegalArgumentException("Location timestamps không được tồn tại khi không có location snapshot.");
            }
            return null;
        }
        if (point.isEmpty() || point.getSRID() != Wgs84Coordinates.SRID || receivedAt == null) {
            throw new IllegalArgumentException("Operational location context không hợp lệ.");
        }
        Point copy = (Point) point.copy();
        copy.setSRID(Wgs84Coordinates.SRID);
        return copy;
    }
}
