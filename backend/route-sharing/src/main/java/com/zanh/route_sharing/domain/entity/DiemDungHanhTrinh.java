package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "diem_dung_hanh_trinh", uniqueConstraints = {
                @UniqueConstraint(name = "uk_diem_dung_thu_tu", columnNames = { "chuyen_di_id", "thu_tu" }),
                @UniqueConstraint(name = "uk_diem_dung_booking_loai", columnNames = { "yeu_cau_di_chung_id",
                                "loai_diem_dung" })
}, indexes = {
                @Index(name = "idx_diem_dung_chuyen_trang_thai", columnList = "chuyen_di_id,trang_thai_diem_dung")
}, check = {
                @CheckConstraint(name = "ck_diem_dung_thu_tu", constraint = "thu_tu >= 0"),
                @CheckConstraint(name = "ck_diem_dung_ban_kinh", constraint = "ban_kinh_xac_dinh_da_den_met > 0"),
                @CheckConstraint(name = "ck_diem_dung_booking", constraint = "(loai_diem_dung IN ('DRIVER_START','DRIVER_END') "
                                + "AND yeu_cau_di_chung_id IS NULL) "
                                + "OR (loai_diem_dung IN ('PICKUP','DROPOFF') "
                                + "AND yeu_cau_di_chung_id IS NOT NULL)")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class DiemDungHanhTrinh extends Base {
        @Column(name = "thu_tu", nullable = false)
        private Integer thuTu;
        @Column(name = "toa_do_ke_hoach", nullable = false, columnDefinition = "geometry(Point,4326)")
        private Point toaDoKeHoach;
        @Column(name = "toa_do_thuc_te", columnDefinition = "geometry(Point,4326)")
        private Point toaDoThucTe;
        @Column(name = "dia_chi", nullable = false, length = 500)
        private String diaChi;
        @Enumerated(EnumType.STRING)
        @Column(name = "loai_diem_dung", nullable = false, length = 30)
        private LoaiDiemDung loaiDiemDung;
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "trang_thai_diem_dung", nullable = false, length = 30)
        private TrangThaiDiemDung trangThaiDiemDung = TrangThaiDiemDung.PENDING;
        @Column(name = "ban_kinh_xac_dinh_da_den_met", nullable = false, precision = 12, scale = 2)
        private BigDecimal banKinhXacDinhDaDenMet;
        @Column(name = "den_gan_luc")
        private Instant denGanLuc;
        @Column(name = "den_luc")
        private Instant denLuc;
        @Column(name = "bat_dau_cho_luc")
        private Instant batDauChoLuc;
        @Column(name = "han_cho_luc")
        private Instant hanChoLuc;
        @Column(name = "hoan_thanh_luc")
        private Instant hoanThanhLuc;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "chuyen_di_id", nullable = false)
        private ChuyenDi chuyenDi;
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "yeu_cau_di_chung_id")
        private YeuCauDiChung yeuCauDiChung;

        public static DiemDungHanhTrinh planned(
                        ChuyenDi trip,
                        YeuCauDiChung rideRequest,
                        int order,
                        Point plannedPoint,
                        String address,
                        LoaiDiemDung type,
                        BigDecimal arrivalRadiusMeters) {
                if (trip == null) {
                        throw new IllegalArgumentException("trip không được trống");
                }
                if (order <= 0) {
                        throw new IllegalArgumentException("Thứ tự điểm dừng phải là số dương.");
                }
                if (plannedPoint == null || plannedPoint.isEmpty() || plannedPoint.getSRID() != Wgs84Coordinates.SRID) {
                        throw new IllegalArgumentException("Điểm dừng phải là Point SRID 4326 hợp lệ.");
                }
                if (address == null || address.isBlank() || address.trim().length() > 500) {
                        throw new IllegalArgumentException("Địa chỉ điểm dừng không hợp lệ.");
                }
                if (type == null) {
                        throw new IllegalArgumentException("Loại điểm dừng không được trống.");
                }
                boolean driverBoundary = type == LoaiDiemDung.DRIVER_START || type == LoaiDiemDung.DRIVER_END;
                if (driverBoundary && rideRequest != null) {
                        throw new IllegalArgumentException("Điểm đầu/cuối tài xế không được gắn booking.");
                }
                if (!driverBoundary && rideRequest == null) {
                        throw new IllegalArgumentException("PICKUP/DROPOFF phải gắn booking.");
                }
                if (rideRequest != null && rideRequest.getChuyenDi() != trip) {
                        throw new IllegalArgumentException(
                                        "Booking phải được gắn vào cùng chuyến đi trước khi tạo điểm dừng.");
                }
                if (arrivalRadiusMeters == null || arrivalRadiusMeters.signum() <= 0) {
                        throw new IllegalArgumentException("Bán kính xác định đã đến phải lớn hơn 0.");
                }
                Point pointCopy = (Point) plannedPoint.copy();
                pointCopy.setSRID(Wgs84Coordinates.SRID);
                DiemDungHanhTrinh stop = new DiemDungHanhTrinh();
                stop.chuyenDi = trip;
                stop.yeuCauDiChung = rideRequest;
                stop.thuTu = order;
                stop.toaDoKeHoach = pointCopy;
                stop.diaChi = address.trim();
                stop.loaiDiemDung = type;
                stop.trangThaiDiemDung = TrangThaiDiemDung.PENDING;
                stop.banKinhXacDinhDaDenMet = arrivalRadiusMeters;
                return stop;
        }

        public void completeDriverStart(Point actualPoint, Instant completedAt) {
                if (this.loaiDiemDung != LoaiDiemDung.DRIVER_START) {
                        throw new IllegalStateException(
                                        "Chỉ DRIVER_START mới được hoàn thành bởi thao tác bắt đầu chuyến.");
                }
                if (this.trangThaiDiemDung != TrangThaiDiemDung.PENDING) {
                        throw new IllegalStateException("DRIVER_START phải đang PENDING trước khi bắt đầu chuyến.");
                }
                if (this.toaDoThucTe != null || this.hoanThanhLuc != null) {
                        throw new IllegalStateException("DRIVER_START đã có bằng chứng hoàn thành trước đó.");
                }
                if (actualPoint == null || actualPoint.isEmpty() || actualPoint.getSRID() != Wgs84Coordinates.SRID) {
                        throw new IllegalArgumentException("Vị trí thực tế phải là Point WGS84 SRID 4326 hợp lệ.");
                }
                if (completedAt == null) {
                        throw new IllegalArgumentException("completedAt không được trống.");
                }
                Point pointCopy = (Point) actualPoint.copy();
                pointCopy.setSRID(Wgs84Coordinates.SRID);
                this.toaDoThucTe = pointCopy;
                this.hoanThanhLuc = completedAt;
                this.trangThaiDiemDung = TrangThaiDiemDung.COMPLETED;
        }

        public void completeDriverEnd(Point actualPoint, Instant completedAt) {
                if (this.loaiDiemDung != LoaiDiemDung.DRIVER_END) {
                        throw new IllegalStateException(
                                        "Chỉ DRIVER_END mới được hoàn thành bởi thao tác kết thúc chuyến.");
                }
                if (this.trangThaiDiemDung != TrangThaiDiemDung.PENDING) {
                        throw new IllegalStateException("DRIVER_END phải đang PENDING trước khi kết thúc chuyến.");
                }
                if (this.yeuCauDiChung != null) {
                        throw new IllegalStateException("DRIVER_END không được gắn booking.");
                }
                if (this.toaDoKeHoach == null || this.toaDoKeHoach.isEmpty()
                                || this.toaDoKeHoach.getSRID() != Wgs84Coordinates.SRID
                                || this.banKinhXacDinhDaDenMet == null
                                || this.banKinhXacDinhDaDenMet.signum() <= 0) {
                        throw new IllegalStateException("DRIVER_END thiếu planned point/radius hợp lệ.");
                }
                if (this.toaDoThucTe != null || this.denGanLuc != null || this.denLuc != null
                                || this.batDauChoLuc != null || this.hanChoLuc != null || this.hoanThanhLuc != null) {
                        throw new IllegalStateException("DRIVER_END đã có bằng chứng vận hành trước đó.");
                }
                if (actualPoint == null || actualPoint.isEmpty() || actualPoint.getSRID() != Wgs84Coordinates.SRID
                                || !Wgs84Coordinates.isValidLongitudeLatitude(actualPoint.getX(), actualPoint.getY())) {
                        throw new IllegalArgumentException("Vị trí thực tế phải là Point WGS84 SRID 4326 hợp lệ.");
                }
                if (completedAt == null) {
                        throw new IllegalArgumentException("completedAt không được trống.");
                }
                Point pointCopy = (Point) actualPoint.copy();
                pointCopy.setSRID(Wgs84Coordinates.SRID);
                this.toaDoThucTe = pointCopy;
                this.hoanThanhLuc = completedAt;
                this.trangThaiDiemDung = TrangThaiDiemDung.COMPLETED;
        }

        public void arrivePickup(Point actualPoint, Instant arrivedAt, Instant waitingDeadline) {
                if (this.loaiDiemDung != LoaiDiemDung.PICKUP) {
                        throw new IllegalStateException("Chỉ PICKUP mới được ghi nhận đã đến bởi E5-02.");
                }
                if (this.trangThaiDiemDung != TrangThaiDiemDung.PENDING) {
                        throw new IllegalStateException("PICKUP phải đang PENDING trước khi ghi nhận ARRIVED.");
                }
                if (this.yeuCauDiChung == null) {
                        throw new IllegalStateException("PICKUP phải gắn booking.");
                }
                if (actualPoint == null || actualPoint.isEmpty() || actualPoint.getSRID() != Wgs84Coordinates.SRID) {
                        throw new IllegalArgumentException("Vị trí thực tế phải là Point WGS84 SRID 4326 hợp lệ.");
                }
                if (arrivedAt == null || waitingDeadline == null || waitingDeadline.isBefore(arrivedAt)) {
                        throw new IllegalArgumentException("Thời điểm arrival/waiting deadline không hợp lệ.");
                }
                if (this.toaDoThucTe != null
                                || this.denGanLuc != null
                                || this.denLuc != null
                                || this.batDauChoLuc != null
                                || this.hanChoLuc != null
                                || this.hoanThanhLuc != null) {
                        throw new IllegalStateException("PICKUP đã có bằng chứng vận hành trước đó.");
                }
                Point pointCopy = (Point) actualPoint.copy();
                pointCopy.setSRID(Wgs84Coordinates.SRID);
                this.toaDoThucTe = pointCopy;
                this.denLuc = arrivedAt;
                this.batDauChoLuc = arrivedAt;
                this.hanChoLuc = waitingDeadline;
                this.trangThaiDiemDung = TrangThaiDiemDung.ARRIVED;
        }

        public void arriveDropoff(Point actualPoint, Instant arrivedAt) {
                if (this.loaiDiemDung != LoaiDiemDung.DROPOFF) {
                        throw new IllegalStateException("Chỉ DROPOFF mới được ghi nhận đã đến bởi E7-01.");
                }
                if (this.trangThaiDiemDung != TrangThaiDiemDung.PENDING) {
                        throw new IllegalStateException("DROPOFF phải đang PENDING trước khi ghi nhận ARRIVED.");
                }
                if (this.yeuCauDiChung == null) {
                        throw new IllegalStateException("DROPOFF phải gắn booking.");
                }
                if (actualPoint == null || actualPoint.isEmpty() || actualPoint.getSRID() != Wgs84Coordinates.SRID) {
                        throw new IllegalArgumentException("Vị trí thực tế phải là Point WGS84 SRID 4326 hợp lệ.");
                }
                if (arrivedAt == null) {
                        throw new IllegalArgumentException("arrivedAt không được trống.");
                }
                if (this.toaDoThucTe != null
                                || this.denGanLuc != null
                                || this.denLuc != null
                                || this.batDauChoLuc != null
                                || this.hanChoLuc != null
                                || this.hoanThanhLuc != null) {
                        throw new IllegalStateException("DROPOFF đã có bằng chứng vận hành trước đó.");
                }
                Point pointCopy = (Point) actualPoint.copy();
                pointCopy.setSRID(Wgs84Coordinates.SRID);
                this.toaDoThucTe = pointCopy;
                this.denLuc = arrivedAt;
                this.trangThaiDiemDung = TrangThaiDiemDung.ARRIVED;
        }

        public void completeArrivedDropoff(Instant completedAt) {
                if (this.loaiDiemDung != LoaiDiemDung.DROPOFF) {
                        throw new IllegalStateException("Chỉ DROPOFF mới được hoàn thành bởi E7-02.");
                }
                if (this.trangThaiDiemDung != TrangThaiDiemDung.ARRIVED) {
                        throw new IllegalStateException("DROPOFF phải ARRIVED trước khi xác nhận trả khách.");
                }
                if (this.yeuCauDiChung == null) {
                        throw new IllegalStateException("DROPOFF phải gắn booking.");
                }
                if (this.toaDoThucTe == null || this.denLuc == null
                                || this.batDauChoLuc != null || this.hanChoLuc != null) {
                        throw new IllegalStateException("DROPOFF ARRIVED thiếu arrival evidence hợp lệ.");
                }
                if (completedAt == null || completedAt.isBefore(this.denLuc)) {
                        throw new IllegalArgumentException("completedAt không hợp lệ.");
                }
                if (this.hoanThanhLuc != null) {
                        throw new IllegalStateException("DROPOFF đã được hoàn thành trước đó.");
                }
                this.hoanThanhLuc = completedAt;
                this.trangThaiDiemDung = TrangThaiDiemDung.COMPLETED;
        }

        public void completeArrivedPickup(Instant completedAt) {
                if (this.loaiDiemDung != LoaiDiemDung.PICKUP) {
                        throw new IllegalStateException("Chỉ PICKUP mới được hoàn thành bởi Boarding.");
                }
                if (this.trangThaiDiemDung != TrangThaiDiemDung.ARRIVED) {
                        throw new IllegalStateException("PICKUP phải ARRIVED trước khi Passenger lên xe.");
                }
                if (this.yeuCauDiChung == null) {
                        throw new IllegalStateException("PICKUP phải gắn booking.");
                }
                if (this.denLuc == null || this.batDauChoLuc == null || this.hanChoLuc == null
                                || !this.denLuc.equals(this.batDauChoLuc)) {
                        throw new IllegalStateException("PICKUP ARRIVED thiếu waiting evidence hợp lệ.");
                }
                if (completedAt == null || completedAt.isBefore(this.denLuc)) {
                        throw new IllegalArgumentException("completedAt không hợp lệ.");
                }
                if (this.hoanThanhLuc != null) {
                        throw new IllegalStateException("PICKUP đã được hoàn thành trước đó.");
                }
                this.hoanThanhLuc = completedAt;
                this.trangThaiDiemDung = TrangThaiDiemDung.COMPLETED;
        }

        public void skipArrivedPickupForNoShow(Instant noShowAt) {
                if (this.loaiDiemDung != LoaiDiemDung.PICKUP) {
                        throw new IllegalStateException("Chỉ PICKUP mới được SKIPPED bởi No-show.");
                }
                if (this.trangThaiDiemDung != TrangThaiDiemDung.ARRIVED || this.yeuCauDiChung == null) {
                        throw new IllegalStateException("PICKUP phải ARRIVED và gắn booking trước No-show.");
                }
                if (this.denLuc == null || this.batDauChoLuc == null || this.hanChoLuc == null
                                || !this.denLuc.equals(this.batDauChoLuc)
                                || this.hanChoLuc.isBefore(this.denLuc)
                                || this.hoanThanhLuc != null) {
                        throw new IllegalStateException("PICKUP ARRIVED thiếu waiting evidence hợp lệ.");
                }
                if (noShowAt == null || noShowAt.isBefore(this.hanChoLuc)) {
                        throw new IllegalArgumentException("No-show chỉ hợp lệ tại hoặc sau waitingDeadline.");
                }
                this.trangThaiDiemDung = TrangThaiDiemDung.SKIPPED;
        }

        public void skipPendingDropoffForNoShow() {
                if (this.loaiDiemDung != LoaiDiemDung.DROPOFF) {
                        throw new IllegalStateException("Chỉ DROPOFF mới được skip như hậu quả No-show.");
                }
                if (this.trangThaiDiemDung != TrangThaiDiemDung.PENDING || this.yeuCauDiChung == null) {
                        throw new IllegalStateException("DROPOFF phải PENDING và gắn booking trước No-show.");
                }
                if (this.toaDoThucTe != null || this.denGanLuc != null || this.denLuc != null
                                || this.batDauChoLuc != null || this.hanChoLuc != null || this.hoanThanhLuc != null) {
                        throw new IllegalStateException(
                                        "DROPOFF PENDING không được có operational evidence trước No-show.");
                }
                this.trangThaiDiemDung = TrangThaiDiemDung.SKIPPED;
        }

        public TrangThaiDiemDung cancelForSafety() {
                if (this.trangThaiDiemDung != TrangThaiDiemDung.PENDING
                                && this.trangThaiDiemDung != TrangThaiDiemDung.APPROACHING
                                && this.trangThaiDiemDung != TrangThaiDiemDung.ARRIVED) {
                        throw new IllegalStateException("Chỉ stop chưa hoàn tất mới được Safety cancel.");
                }
                TrangThaiDiemDung previous = this.trangThaiDiemDung;
                this.trangThaiDiemDung = TrangThaiDiemDung.CANCELLED;
                return previous;
        }

        public Instant extendWaitingDeadlineForSafety(java.time.Duration holdDuration) {
                if (this.loaiDiemDung != LoaiDiemDung.PICKUP || this.trangThaiDiemDung != TrangThaiDiemDung.ARRIVED
                                || this.hanChoLuc == null || this.denLuc == null || this.batDauChoLuc == null) {
                        throw new IllegalStateException("Chỉ PICKUP ARRIVED có waiting deadline mới được gia hạn.");
                }
                if (holdDuration == null || holdDuration.isNegative() || holdDuration.isZero()) {
                        throw new IllegalArgumentException("holdDuration phải dương.");
                }
                Instant previous = this.hanChoLuc;
                try {
                        this.hanChoLuc = this.hanChoLuc.plus(holdDuration);
                } catch (java.time.DateTimeException | ArithmeticException ex) {
                        throw new IllegalArgumentException("Không thể gia hạn waiting deadline.", ex);
                }
                return previous;
        }

        public void cancelBeforeStart() {
                if (this.trangThaiDiemDung != TrangThaiDiemDung.PENDING) {
                        throw new IllegalStateException("Chỉ điểm dừng PENDING mới được hủy trước khi chuyến bắt đầu.");
                }
                if (this.toaDoThucTe != null
                                || this.denGanLuc != null
                                || this.denLuc != null
                                || this.batDauChoLuc != null
                                || this.hanChoLuc != null
                                || this.hoanThanhLuc != null) {
                        throw new IllegalStateException(
                                        "Điểm dừng PREPARING không được có bằng chứng vận hành trước khi hủy.");
                }
                this.trangThaiDiemDung = TrangThaiDiemDung.CANCELLED;
        }

}
