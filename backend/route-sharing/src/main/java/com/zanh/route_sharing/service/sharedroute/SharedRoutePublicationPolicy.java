package com.zanh.route_sharing.service.sharedroute;

import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiXe;
import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Component
public class SharedRoutePublicationPolicy {

    public void validate(
            NguoiDung actor,
            HoSoTaiXe driverProfile,
            PhuongTien vehicle,
            CreateSharedRouteRequest request,
            Instant now) {
        Objects.requireNonNull(actor, "actor không được trống");
        Objects.requireNonNull(driverProfile, "driverProfile không được trống");
        Objects.requireNonNull(vehicle, "vehicle không được trống");
        Objects.requireNonNull(request, "request không được trống");
        Objects.requireNonNull(now, "now không được trống");

        validateAccount(actor);
        validateDriverProfile(actor, driverProfile);
        validateVehicle(actor, vehicle);
        validateSeatCount(vehicle, request.offeredSeats());
        validateDepartureTime(request.expectedDepartureTime(), now);
        validateSupportAmount(request.suggestedSupportPerKm());
        validateDistinctEndpoints(request);
    }

    private static void validateAccount(NguoiDung actor) {
        if (actor.getTrangThaiTaiKhoan() != TrangThaiTaiKhoan.ACTIVE) {
            throw error(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE",
                    "Tài khoản chưa ở trạng thái hoạt động.");
        }
    }

    private static void validateDriverProfile(NguoiDung actor, HoSoTaiXe driverProfile) {
        if (driverProfile.getNguoiDung() == null
                || !Objects.equals(driverProfile.getNguoiDung().getId(), actor.getId())) {
            throw error(HttpStatus.CONFLICT, "DRIVER_PROFILE_MISMATCH",
                    "Hồ sơ tài xế không thuộc tài khoản hiện tại.");
        }
        if (driverProfile.getTrangThaiTaiXe() != TrangThaiTaiXe.ACTIVE) {
            throw error(HttpStatus.CONFLICT, "DRIVER_NOT_ACTIVE",
                    "Hồ sơ tài xế chưa ở trạng thái hoạt động.");
        }
    }

    private static void validateVehicle(NguoiDung actor, PhuongTien vehicle) {
        if (vehicle.getTrangThaiPhuongTien() != TrangThaiPhuongTien.ACTIVE) {
            throw error(HttpStatus.CONFLICT, "VEHICLE_NOT_ACTIVE",
                    "Phương tiện chưa ở trạng thái hoạt động.");
        }
        if (vehicle.getNguoiDangKySuDung() == null
                || !Objects.equals(vehicle.getNguoiDangKySuDung().getId(), actor.getId())) {
            throw error(HttpStatus.FORBIDDEN, "VEHICLE_NOT_AUTHORIZED",
                    "Bạn không có quyền sử dụng phương tiện này để đăng lộ trình.");
        }
        if (vehicle.getDongXe() == null || vehicle.getDongXe().getLoaiPhuongTien() == null) {
            throw error(HttpStatus.CONFLICT, "VEHICLE_TYPE_NOT_CONFIGURED",
                    "Phương tiện chưa được cấu hình loại phương tiện.");
        }
        if (vehicle.getSoChoHanhKhachDuocDuyet() == null
                || vehicle.getSoChoHanhKhachDuocDuyet() < 1) {
            throw error(HttpStatus.CONFLICT, "VEHICLE_CAPACITY_NOT_CONFIGURED",
                    "Số chỗ hành khách của phương tiện chưa hợp lệ.");
        }
    }

    private static void validateSeatCount(PhuongTien vehicle, Integer offeredSeats) {
        if (offeredSeats == null || offeredSeats < 1) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_SEAT_COUNT",
                    "Số ghế cung cấp phải lớn hơn hoặc bằng 1.");
        }
        if (offeredSeats > vehicle.getSoChoHanhKhachDuocDuyet()) {
            throw error(HttpStatus.CONFLICT, "SEAT_COUNT_EXCEEDS_VEHICLE_CAPACITY",
                    "Số ghế cung cấp vượt quá số chỗ hành khách đã được duyệt của phương tiện.");
        }
    }

    private static void validateDepartureTime(Instant expectedDepartureTime, Instant now) {
        if (expectedDepartureTime == null || !expectedDepartureTime.isAfter(now)) {
            throw error(HttpStatus.BAD_REQUEST, "DEPARTURE_TIME_MUST_BE_FUTURE",
                    "Thời gian khởi hành dự kiến phải nằm trong tương lai.");
        }
    }

    private static void validateSupportAmount(BigDecimal amount) {
        if (amount != null && amount.signum() < 0) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_SUPPORT_AMOUNT",
                    "Mức hỗ trợ gợi ý không được âm.");
        }
    }

    private static void validateDistinctEndpoints(CreateSharedRouteRequest request) {
        boolean sameLatitude = request.origin().latitude().compareTo(request.driverDestination().latitude()) == 0;
        boolean sameLongitude = request.origin().longitude().compareTo(request.driverDestination().longitude()) == 0;
        if (sameLatitude && sameLongitude) {
            throw error(HttpStatus.BAD_REQUEST, "ROUTE_ENDPOINTS_MUST_BE_DIFFERENT",
                    "Điểm xuất phát và điểm đích phải khác nhau.");
        }
    }

    private static BusinessException error(HttpStatus status, String code, String message) {
        return new BusinessException(status, code, message);
    }
}
