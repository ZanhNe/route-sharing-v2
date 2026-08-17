package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Objects;

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
                        throw new IllegalArgumentException(
                                        "Không xác định được hành khách hoặc lộ trình để thông báo hủy.");
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

        public static ThongBao routeLockedForTrip(
                        YeuCauDiChung rideRequest,
                        ChuyenDi trip) {
                if (rideRequest == null || rideRequest.getId() == null
                                || rideRequest.getHanhKhach() == null
                                || rideRequest.getLoTrinhChiaSe() == null
                                || rideRequest.getLoTrinhChiaSe().getId() == null
                                || trip == null || trip.getId() == null) {
                        throw new IllegalArgumentException(
                                        "Không xác định được booking/trip để tạo thông báo khóa lộ trình.");
                }
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.ROUTE_LOCKED)
                                .tieuDe("Lộ trình đã được khóa và hình thành chuyến đi")
                                .noiDung("Tài xế đã khóa danh sách; booking của bạn đã được đưa vào chuyến đi thực tế.")
                                .kenhGui(KenhThongBao.IN_APP)
                                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("CHUYEN_DI")
                                .doiTuongLienQuanId(trip.getId())
                                .deduplicationKey("E4-01:ROUTE:" + rideRequest.getLoTrinhChiaSe().getId()
                                                + ":REQUEST:" + rideRequest.getId() + ":LOCKED")
                                .nguoiNhan(rideRequest.getHanhKhach())
                                .build();
        }

        public static ThongBao tripStarted(
                        YeuCauDiChung rideRequest,
                        ChuyenDi trip) {
                if (rideRequest == null || rideRequest.getId() == null
                                || rideRequest.getHanhKhach() == null
                                || rideRequest.getChuyenDi() == null
                                || trip == null || trip.getId() == null
                                || !Objects.equals(rideRequest.getChuyenDi().getId(), trip.getId())) {
                        throw new IllegalArgumentException(
                                        "Không xác định được booking/trip để tạo thông báo bắt đầu chuyến.");
                }
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.TRIP_STARTED)
                                .tieuDe("Chuyến đi đã bắt đầu")
                                .noiDung("Tài xế đã bắt đầu chuyến đi mà booking của bạn đang tham gia.")
                                .kenhGui(KenhThongBao.IN_APP)
                                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("CHUYEN_DI")
                                .doiTuongLienQuanId(trip.getId())
                                .deduplicationKey("E5-01:TRIP:" + trip.getId()
                                                + ":REQUEST:" + rideRequest.getId() + ":STARTED")
                                .nguoiNhan(rideRequest.getHanhKhach())
                                .build();
        }

        public static ThongBao tripCancelledBeforeStart(
                        YeuCauDiChung rideRequest,
                        ChuyenDi trip) {
                if (rideRequest == null || rideRequest.getId() == null
                                || rideRequest.getHanhKhach() == null
                                || rideRequest.getChuyenDi() == null
                                || trip == null || trip.getId() == null
                                || !Objects.equals(rideRequest.getChuyenDi().getId(), trip.getId())
                                || trip.getLoTrinhChiaSe() == null
                                || trip.getLoTrinhChiaSe().getLyDoHuy() == null
                                || trip.getLoTrinhChiaSe().getLyDoHuy().isBlank()) {
                        throw new IllegalArgumentException(
                                        "Không xác định được booking/trip/reason để tạo thông báo hủy chuyến.");
                }
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.TRIP_CANCELLED_BEFORE_START)
                                .tieuDe("Chuyến đi đã bị hủy trước khi bắt đầu")
                                .noiDung("Tài xế đã hủy chuyến trước khi bắt đầu. Lý do: "
                                                + trip.getLoTrinhChiaSe().getLyDoHuy())
                                .kenhGui(KenhThongBao.IN_APP)
                                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("CHUYEN_DI")
                                .doiTuongLienQuanId(trip.getId())
                                .deduplicationKey("E5-05:TRIP:" + trip.getId()
                                                + ":REQUEST:" + rideRequest.getId() + ":CANCELLED_BEFORE_START")
                                .nguoiNhan(rideRequest.getHanhKhach())
                                .build();
        }

        public static ThongBao driverArrivedPickup(
                        YeuCauDiChung rideRequest,
                        ChuyenDi trip,
                        DiemDungHanhTrinh pickup) {
                if (rideRequest == null || rideRequest.getId() == null
                                || rideRequest.getHanhKhach() == null
                                || rideRequest.getChuyenDi() == null
                                || trip == null || trip.getId() == null
                                || pickup == null || pickup.getId() == null
                                || pickup.getLoaiDiemDung() != LoaiDiemDung.PICKUP
                                || pickup.getTrangThaiDiemDung() != TrangThaiDiemDung.ARRIVED
                                || pickup.getHanChoLuc() == null
                                || !Objects.equals(rideRequest.getChuyenDi().getId(), trip.getId())
                                || pickup.getYeuCauDiChung() == null
                                || !Objects.equals(pickup.getYeuCauDiChung().getId(), rideRequest.getId())) {
                        throw new IllegalArgumentException(
                                        "Không xác định được booking/trip/pickup ARRIVED để tạo thông báo.");
                }
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.DRIVER_ARRIVED_PICKUP)
                                .tieuDe("Tài xế đã đến điểm đón")
                                .noiDung("Tài xế đã đến điểm đón của bạn. Vui lòng thực hiện bước xác nhận lên xe trong thời gian chờ.")
                                .kenhGui(KenhThongBao.IN_APP)
                                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("CHUYEN_DI")
                                .doiTuongLienQuanId(trip.getId())
                                .deduplicationKey("E5-02:TRIP:" + trip.getId()
                                                + ":STOP:" + pickup.getId()
                                                + ":REQUEST:" + rideRequest.getId() + ":ARRIVED")
                                .nguoiNhan(rideRequest.getHanhKhach())
                                .build();
        }

        public static ThongBao driverArrivedDropoff(
                        YeuCauDiChung rideRequest,
                        ChuyenDi trip,
                        DiemDungHanhTrinh dropoff) {
                if (rideRequest == null || rideRequest.getId() == null
                                || rideRequest.getHanhKhach() == null
                                || rideRequest.getChuyenDi() == null
                                || rideRequest.getTrangThaiYeuCau() != TrangThaiYeuCau.ON_BOARD
                                || trip == null || trip.getId() == null
                                || dropoff == null || dropoff.getId() == null
                                || dropoff.getLoaiDiemDung() != LoaiDiemDung.DROPOFF
                                || dropoff.getTrangThaiDiemDung() != TrangThaiDiemDung.ARRIVED
                                || dropoff.getDenLuc() == null
                                || dropoff.getBatDauChoLuc() != null
                                || dropoff.getHanChoLuc() != null
                                || dropoff.getHoanThanhLuc() != null
                                || !Objects.equals(rideRequest.getChuyenDi().getId(), trip.getId())
                                || dropoff.getYeuCauDiChung() == null
                                || !Objects.equals(dropoff.getYeuCauDiChung().getId(), rideRequest.getId())) {
                        throw new IllegalArgumentException(
                                        "Không xác định được booking/trip/dropoff ARRIVED để tạo thông báo.");
                }
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.DRIVER_ARRIVED_DROPOFF)
                                .tieuDe("Tài xế đã đến điểm trả khách")
                                .noiDung("Tài xế đã đến điểm trả của bạn. Vui lòng thực hiện bước xác nhận trả khách.")
                                .kenhGui(KenhThongBao.IN_APP)
                                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("CHUYEN_DI")
                                .doiTuongLienQuanId(trip.getId())
                                .deduplicationKey("E7-01:TRIP:" + trip.getId()
                                                + ":STOP:" + dropoff.getId()
                                                + ":REQUEST:" + rideRequest.getId() + ":ARRIVED")
                                .nguoiNhan(rideRequest.getHanhKhach())
                                .build();
        }

        public static ThongBao passengerBoarded(
                        YeuCauDiChung rideRequest,
                        ChuyenDi trip,
                        DiemDungHanhTrinh pickup) {
                if (rideRequest == null || rideRequest.getId() == null
                                || rideRequest.getHanhKhach() == null
                                || rideRequest.getChuyenDi() == null
                                || trip == null || trip.getId() == null
                                || pickup == null || pickup.getId() == null
                                || pickup.getLoaiDiemDung() != LoaiDiemDung.PICKUP
                                || pickup.getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                                || rideRequest.getTrangThaiYeuCau() != TrangThaiYeuCau.ON_BOARD
                                || rideRequest.getLenXeLuc() == null
                                || pickup.getHoanThanhLuc() == null
                                || !Objects.equals(rideRequest.getChuyenDi().getId(), trip.getId())
                                || pickup.getYeuCauDiChung() == null
                                || !Objects.equals(pickup.getYeuCauDiChung().getId(), rideRequest.getId())) {
                        throw new IllegalArgumentException(
                                        "Không xác định được booking/trip/pickup đã Boarding để tạo thông báo.");
                }
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.PASSENGER_BOARDED)
                                .tieuDe("Đã xác nhận bạn lên xe")
                                .noiDung("Tài xế đã xác nhận bạn đã lên xe cho chuyến đi này.")
                                .kenhGui(KenhThongBao.IN_APP)
                                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("CHUYEN_DI")
                                .doiTuongLienQuanId(trip.getId())
                                .deduplicationKey("E5-03:TRIP:" + trip.getId()
                                                + ":STOP:" + pickup.getId()
                                                + ":REQUEST:" + rideRequest.getId() + ":ON_BOARD")
                                .nguoiNhan(rideRequest.getHanhKhach())
                                .build();
        }

        public static ThongBao passengerDroppedOff(
                        YeuCauDiChung rideRequest,
                        ChuyenDi trip,
                        DiemDungHanhTrinh dropoff) {
                if (rideRequest == null || rideRequest.getId() == null
                                || rideRequest.getHanhKhach() == null
                                || rideRequest.getChuyenDi() == null
                                || trip == null || trip.getId() == null
                                || dropoff == null || dropoff.getId() == null
                                || dropoff.getLoaiDiemDung() != LoaiDiemDung.DROPOFF
                                || dropoff.getTrangThaiDiemDung() != TrangThaiDiemDung.COMPLETED
                                || rideRequest.getTrangThaiYeuCau() != TrangThaiYeuCau.COMPLETED
                                || rideRequest.getXuongXeLuc() == null
                                || dropoff.getHoanThanhLuc() == null
                                || !rideRequest.getXuongXeLuc().equals(dropoff.getHoanThanhLuc())
                                || !Objects.equals(rideRequest.getChuyenDi().getId(), trip.getId())
                                || dropoff.getYeuCauDiChung() == null
                                || !Objects.equals(dropoff.getYeuCauDiChung().getId(), rideRequest.getId())) {
                        throw new IllegalArgumentException(
                                        "Không xác định được booking/trip/dropoff đã hoàn tất trả khách để tạo thông báo.");
                }
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.PASSENGER_DROPPED_OFF)
                                .tieuDe("Đã xác nhận trả khách")
                                .noiDung("Chuyến đi đã ghi nhận bạn được trả tại điểm trả khách đã thống nhất.")
                                .kenhGui(KenhThongBao.IN_APP)
                                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("CHUYEN_DI")
                                .doiTuongLienQuanId(trip.getId())
                                .deduplicationKey("E7-02:TRIP:" + trip.getId()
                                                + ":STOP:" + dropoff.getId()
                                                + ":REQUEST:" + rideRequest.getId() + ":COMPLETED")
                                .nguoiNhan(rideRequest.getHanhKhach())
                                .build();
        }

        public static ThongBao passengerNoShow(
                        YeuCauDiChung rideRequest,
                        ChuyenDi trip,
                        DiemDungHanhTrinh pickup,
                        DiemDungHanhTrinh dropoff) {
                if (rideRequest == null || rideRequest.getId() == null
                                || rideRequest.getHanhKhach() == null
                                || rideRequest.getChuyenDi() == null
                                || trip == null || trip.getId() == null
                                || pickup == null || pickup.getId() == null
                                || dropoff == null || dropoff.getId() == null
                                || rideRequest.getTrangThaiYeuCau() != TrangThaiYeuCau.NO_SHOW
                                || rideRequest.getKhongDenLuc() == null
                                || pickup.getLoaiDiemDung() != LoaiDiemDung.PICKUP
                                || dropoff.getLoaiDiemDung() != LoaiDiemDung.DROPOFF
                                || pickup.getTrangThaiDiemDung() != TrangThaiDiemDung.SKIPPED
                                || dropoff.getTrangThaiDiemDung() != TrangThaiDiemDung.SKIPPED
                                || !Objects.equals(rideRequest.getChuyenDi().getId(), trip.getId())
                                || pickup.getYeuCauDiChung() == null || dropoff.getYeuCauDiChung() == null
                                || !Objects.equals(pickup.getYeuCauDiChung().getId(), rideRequest.getId())
                                || !Objects.equals(dropoff.getYeuCauDiChung().getId(), rideRequest.getId())) {
                        throw new IllegalArgumentException(
                                        "Không xác định được booking/trip/stops đã No-show để tạo thông báo.");
                }
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.PASSENGER_NO_SHOW)
                                .tieuDe("Booking được ghi nhận No-show")
                                .noiDung("Tài xế đã xác nhận bạn không xuất hiện tại điểm đón sau thời gian chờ của chuyến đi này.")
                                .kenhGui(KenhThongBao.IN_APP)
                                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("CHUYEN_DI")
                                .doiTuongLienQuanId(trip.getId())
                                .deduplicationKey("E5-04:TRIP:" + trip.getId()
                                                + ":STOP:" + pickup.getId()
                                                + ":REQUEST:" + rideRequest.getId() + ":NO_SHOW")
                                .nguoiNhan(rideRequest.getHanhKhach())
                                .build();
        }

        public static ThongBao tripSafetyIncidentReported(
                        SuCoChuyenDi incident,
                        NguoiDung recipient) {
                if (incident == null || incident.getId() == null
                                || incident.getChuyenDi() == null || incident.getChuyenDi().getId() == null
                                || incident.getLoaiSuCo() == null || incident.getMucDo() == null
                                || incident.getTrangThaiXuLy() != TrangThaiXuLySuCo.OPEN
                                || recipient == null || recipient.getId() == null) {
                        throw new IllegalArgumentException(
                                        "Không xác định được incident/recipient để tạo Safety notification.");
                }
                boolean sos = incident.getLoaiSuCo() == LoaiSuCo.SOS;
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.TRIP_SAFETY_INCIDENT_REPORTED)
                                .tieuDe(sos ? "Có SOS mới cần xử lý" : "Có báo cáo sự cố chuyến đi mới")
                                .noiDung(sos
                                                ? "Một SOS mức CRITICAL đã được ghi nhận cho chuyến đi cần Safety xử lý."
                                                : "Một sự cố chuyến đi mới đã được ghi nhận và cần Safety xem xét.")
                                .kenhGui(KenhThongBao.IN_APP)
                                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("SU_CO_CHUYEN_DI")
                                .doiTuongLienQuanId(incident.getId())
                                .deduplicationKey("E6-04:INCIDENT:" + incident.getId()
                                                + ":RECIPIENT:" + recipient.getId() + ":REPORTED")
                                .nguoiNhan(recipient)
                                .build();
        }

        public static ThongBao tripSafetyIncidentAcknowledged(SuCoChuyenDi incident) {
                if (incident == null || incident.getId() == null || incident.getNguoiBaoCao() == null
                                || incident.getNguoiBaoCao().getId() == null
                                || (incident.getTrangThaiXuLy() != TrangThaiXuLySuCo.ACKNOWLEDGED
                                                && incident.getTrangThaiXuLy() != TrangThaiXuLySuCo.INVESTIGATING)) {
                        throw new IllegalArgumentException(
                                        "Không xác định được incident/reporter đã được Safety tiếp nhận.");
                }
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.TRIP_SAFETY_INCIDENT_ACKNOWLEDGED)
                                .tieuDe("Safety đã tiếp nhận báo cáo")
                                .noiDung("Báo cáo sự cố/SOS của chuyến đi đã được nhân sự Safety tiếp nhận xử lý.")
                                .kenhGui(KenhThongBao.IN_APP)
                                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("SU_CO_CHUYEN_DI")
                                .doiTuongLienQuanId(incident.getId())
                                .deduplicationKey("E6-05:INCIDENT:" + incident.getId() + ":REPORTER:"
                                                + incident.getNguoiBaoCao().getId() + ":ACKNOWLEDGED")
                                .nguoiNhan(incident.getNguoiBaoCao())
                                .build();
        }

        public static ThongBao tripSafetyIncidentFinalized(SuCoChuyenDi incident) {
                if (incident == null || incident.getId() == null || incident.getNguoiBaoCao() == null
                                || incident.getNguoiBaoCao().getId() == null
                                || (incident.getTrangThaiXuLy() != TrangThaiXuLySuCo.RESOLVED
                                                && incident.getTrangThaiXuLy() != TrangThaiXuLySuCo.FALSE_ALARM)
                                || incident.getGiaiQuyetLuc() == null || incident.getKetLuan() == null
                                || incident.getKetLuan().isBlank()) {
                        throw new IllegalArgumentException("Không xác định được incident/reporter đã hoàn tất xử lý.");
                }
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.TRIP_SAFETY_INCIDENT_FINALIZED)
                                .tieuDe("Báo cáo sự cố đã được xử lý")
                                .noiDung("Safety đã hoàn tất xử lý báo cáo. Mở chi tiết báo cáo để xem trạng thái và kết luận được phép hiển thị.")
                                .kenhGui(KenhThongBao.IN_APP)
                                .trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("SU_CO_CHUYEN_DI")
                                .doiTuongLienQuanId(incident.getId())
                                .deduplicationKey("E6-05:INCIDENT:" + incident.getId() + ":REPORTER:"
                                                + incident.getNguoiBaoCao().getId() + ":FINALIZED")
                                .nguoiNhan(incident.getNguoiBaoCao())
                                .build();
        }

        public static ThongBao passengerSafetyParticipationAborted(CanThiepAnToanChuyenDi intervention,
                        YeuCauDiChung booking) {
                requireSafetyNotification(intervention, booking == null ? null : booking.getHanhKhach());
                if (booking == null || booking.getId() == null
                                || booking.getTrangThaiYeuCau() != TrangThaiYeuCau.ABORTED) {
                        throw new IllegalArgumentException(
                                        "Booking target phải ABORTED để tạo Safety participation notification.");
                }
                NguoiDung recipient = booking.getHanhKhach();
                return ThongBao.builder()
                                .loaiThongBao(LoaiThongBao.PASSENGER_SAFETY_PARTICIPATION_ABORTED)
                                .tieuDe("Bạn không còn tham gia chuyến đi")
                                .noiDung("Việc tham gia chuyến đi của bạn đã được kết thúc vì lý do an toàn. Bạn vẫn có thể xem lịch sử chuyến theo quyền được phép.")
                                .kenhGui(KenhThongBao.IN_APP).trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("CHUYEN_DI")
                                .doiTuongLienQuanId(intervention.getChuyenDi().getId())
                                .deduplicationKey("E6-06:INTERVENTION:" + intervention.getId() + ":RECIPIENT:"
                                                + recipient.getId() + ":PASSENGER_ABORTED")
                                .nguoiNhan(recipient).build();
        }

        public static ThongBao tripSafetyHoldStarted(CanThiepAnToanChuyenDi intervention, NguoiDung recipient) {
                requireSafetyNotification(intervention, recipient);
                return safetyTripNotification(intervention, recipient, LoaiThongBao.TRIP_SAFETY_HOLD_STARTED,
                                "Chuyến đi đang tạm dừng vì an toàn",
                                "Chuyến đang tạm dừng để xử lý một tình huống an toàn. Các thao tác vận hành sẽ tiếp tục khi tình huống được xử lý hoặc chuyến sẽ kết thúc nếu không thể tiếp tục an toàn.",
                                "HOLD_STARTED");
        }

        public static ThongBao tripSafetyHoldResumed(CanThiepAnToanChuyenDi intervention, NguoiDung recipient) {
                requireSafetyNotification(intervention, recipient);
                return safetyTripNotification(intervention, recipient, LoaiThongBao.TRIP_SAFETY_HOLD_RESUMED,
                                "Chuyến đi có thể tiếp tục",
                                "Tình huống an toàn tạm thời đã được xử lý và chuyến đi có thể tiếp tục.",
                                "HOLD_RESUMED");
        }

        public static ThongBao tripEmergencyAborted(CanThiepAnToanChuyenDi intervention, NguoiDung recipient) {
                requireSafetyNotification(intervention, recipient);
                return safetyTripNotification(intervention, recipient, LoaiThongBao.TRIP_EMERGENCY_ABORTED,
                                "Chuyến đi đã kết thúc khẩn cấp",
                                "Chuyến đi đã được kết thúc vì lý do an toàn và không thể tiếp tục. Thông tin chuyến được giữ lại để đối chiếu/xử lý theo quy định của hệ thống.",
                                "TRIP_ABORTED");
        }

        private static ThongBao safetyTripNotification(CanThiepAnToanChuyenDi intervention, NguoiDung recipient,
                        LoaiThongBao type, String title, String content, String outcome) {
                return ThongBao.builder().loaiThongBao(type).tieuDe(title).noiDung(content)
                                .kenhGui(KenhThongBao.IN_APP).trangThaiThongBao(TrangThaiThongBao.PENDING)
                                .loaiDoiTuongLienQuan("CHUYEN_DI")
                                .doiTuongLienQuanId(intervention.getChuyenDi().getId())
                                .deduplicationKey("E6-06:INTERVENTION:" + intervention.getId() + ":RECIPIENT:"
                                                + recipient.getId() + ":" + outcome)
                                .nguoiNhan(recipient).build();
        }

        private static void requireSafetyNotification(CanThiepAnToanChuyenDi intervention, NguoiDung recipient) {
                if (intervention == null || intervention.getId() == null || intervention.getChuyenDi() == null
                                || intervention.getChuyenDi().getId() == null || recipient == null
                                || recipient.getId() == null) {
                        throw new IllegalArgumentException(
                                        "Không xác định được intervention/recipient để tạo Safety notification.");
                }
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
