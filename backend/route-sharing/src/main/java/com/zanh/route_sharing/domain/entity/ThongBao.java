package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "thong_bao", indexes = {
        @Index(name = "idx_thong_bao_nguoi_nhan_trang_thai", columnList = "nguoi_nhan_id,trang_thai_thong_bao"),
        @Index(name = "idx_thong_bao_doi_tuong", columnList = "loai_doi_tuong_lien_quan,doi_tuong_lien_quan_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ThongBao extends Base {
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_thong_bao", nullable = false, length = 50)
    private LoaiThongBao loaiThongBao;
    @Column(name = "tieu_de", nullable = false, length = 255)
    private String tieuDe;
    @Column(name = "noi_dung", nullable = false, length = 3000)
    private String noiDung;
    @Enumerated(EnumType.STRING)
    @Column(name = "kenh_gui", nullable = false, length = 20)
    private KenhThongBao kenhGui;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_thong_bao", nullable = false, length = 20)
    private TrangThaiThongBao trangThaiThongBao = TrangThaiThongBao.PENDING;
    @Column(name = "gui_luc")
    private Instant guiLuc;
    @Column(name = "doc_luc")
    private Instant docLuc;
    @Column(name = "loai_doi_tuong_lien_quan", length = 100)
    private String loaiDoiTuongLienQuan;
    @Column(name = "doi_tuong_lien_quan_id")
    private Long doiTuongLienQuanId;
    @Column(name = "deduplication_key", length = 180)
    private String deduplicationKey;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_nhan_id", nullable = false)
    private NguoiDung nguoiNhan;

    public static ThongBao bookingRequest(
            YeuCauDiChung rideRequest,
            NguoiDung driver) {
        if (rideRequest == null || rideRequest.getId() == null) {
            throw new IllegalArgumentException("Yêu cầu đi chung phải được lưu trước khi tạo thông báo.");
        }
        if (driver == null) {
            throw new IllegalArgumentException("Tài xế nhận thông báo không được trống.");
        }

        return ThongBao.builder()
                .loaiThongBao(LoaiThongBao.BOOKING_REQUEST)
                .tieuDe("Có yêu cầu đi chung mới")
                .noiDung("Một hành khách đã gửi yêu cầu tham gia lộ trình của bạn.")
                .kenhGui(KenhThongBao.IN_APP)
                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                .loaiDoiTuongLienQuan("YEU_CAU_DI_CHUNG")
                .doiTuongLienQuanId(rideRequest.getId())
                .deduplicationKey("BOOKING_REQUEST:" + rideRequest.getId() + ":CREATED")
                .nguoiNhan(driver)
                .build();
    }

    public static ThongBao bookingAccepted(YeuCauDiChung rideRequest) {
        return bookingDecision(
                rideRequest,
                LoaiThongBao.BOOKING_ACCEPTED,
                "Yêu cầu đi chung đã được chấp nhận",
                "Tài xế đã chấp nhận yêu cầu đi chung của bạn.",
                "ACCEPTED");
    }

    public static ThongBao bookingRejected(YeuCauDiChung rideRequest) {
        return bookingDecision(
                rideRequest,
                LoaiThongBao.BOOKING_REJECTED,
                "Yêu cầu đi chung đã bị từ chối",
                "Tài xế đã từ chối yêu cầu đi chung của bạn.",
                "REJECTED");
    }

    public static ThongBao bookingCancelledByPassenger(YeuCauDiChung rideRequest) {
        if (rideRequest == null || rideRequest.getId() == null
                || rideRequest.getLoTrinhChiaSe() == null
                || rideRequest.getLoTrinhChiaSe().getTaiXe() == null) {
            throw new IllegalArgumentException("Không xác định được tài xế nhận thông báo hủy.");
        }
        return bookingCancellation(
                rideRequest,
                rideRequest.getLoTrinhChiaSe().getTaiXe(),
                LoaiThongBao.BOOKING_CANCELLED_BY_PASSENGER,
                "Hành khách đã hủy yêu cầu đi chung",
                "Hành khách đã hủy yêu cầu hoặc booking trên lộ trình của bạn.",
                "CANCELLED_BY_PASSENGER");
    }

    public static ThongBao routeCancelledByDriver(YeuCauDiChung rideRequest) {
        if (rideRequest == null || rideRequest.getId() == null
                || rideRequest.getHanhKhach() == null
                || rideRequest.getLoTrinhChiaSe() == null
                || rideRequest.getLoTrinhChiaSe().getId() == null) {
            throw new IllegalArgumentException("Không xác định được hành khách hoặc lộ trình để thông báo hủy.");
        }
        return ThongBao.builder()
                .loaiThongBao(LoaiThongBao.ROUTE_CANCELLED_BY_DRIVER)
                .tieuDe("Tài xế đã hủy lộ trình chia sẻ")
                .noiDung("Tài xế đã hủy lộ trình; yêu cầu hoặc booking của bạn đã được kết thúc.")
                .kenhGui(KenhThongBao.IN_APP)
                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                .loaiDoiTuongLienQuan("LO_TRINH_CHIA_SE")
                .doiTuongLienQuanId(rideRequest.getLoTrinhChiaSe().getId())
                .deduplicationKey("ROUTE_CANCELLED_BY_DRIVER:"
                        + rideRequest.getLoTrinhChiaSe().getId() + ":"
                        + rideRequest.getId() + ":CANCELLED_BY_DRIVER")
                .nguoiNhan(rideRequest.getHanhKhach())
                .build();
    }

    private static ThongBao bookingCancellation(
            YeuCauDiChung rideRequest,
            NguoiDung recipient,
            LoaiThongBao type,
            String title,
            String content,
            String state) {
        return ThongBao.builder()
                .loaiThongBao(type)
                .tieuDe(title)
                .noiDung(content)
                .kenhGui(KenhThongBao.IN_APP)
                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                .loaiDoiTuongLienQuan("YEU_CAU_DI_CHUNG")
                .doiTuongLienQuanId(rideRequest.getId())
                .deduplicationKey(type.name() + ":" + rideRequest.getId() + ":" + state)
                .nguoiNhan(recipient)
                .build();
    }

    private static ThongBao bookingDecision(
            YeuCauDiChung rideRequest,
            LoaiThongBao notificationType,
            String title,
            String content,
            String decision) {
        if (rideRequest == null || rideRequest.getId() == null) {
            throw new IllegalArgumentException("Yêu cầu đi chung phải được lưu trước khi tạo thông báo.");
        }
        NguoiDung passenger = rideRequest.getHanhKhach();
        if (passenger == null) {
            throw new IllegalArgumentException("Hành khách nhận thông báo không được trống.");
        }
        return ThongBao.builder()
                .loaiThongBao(notificationType)
                .tieuDe(title)
                .noiDung(content)
                .kenhGui(KenhThongBao.IN_APP)
                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                .loaiDoiTuongLienQuan("YEU_CAU_DI_CHUNG")
                .doiTuongLienQuanId(rideRequest.getId())
                .deduplicationKey(notificationType.name() + ":" + rideRequest.getId() + ":" + decision)
                .nguoiNhan(passenger)
                .build();
    }

}
