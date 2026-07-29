package com.zanh.route_sharing.enums;

public class AllEnums {
    public enum LoaiToChuc {
        TRUONG_HOC,
        DOANH_NGHIEP,
        CO_QUAN
    }

    public enum TrangThaiDuyet {
        PENDING,
        VERIFIED,
        REJECTED
    }

    public enum TrangThaiHocTap {
        DANG_HOC,
        BAO_LUU,
        DA_TOT_NGHIEP
    }

    public enum TrangThaiGiayTo {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED
    }

    public enum TrangThaiTaiKhoan {
        INACTIVE,
        ACTIVE,
        LOCKED,
        BANNED
    }

    public enum GioiTinh {
        NAM,
        NU,
        KHAC
    }

    public enum TrinhDoHocVan {
        CU_NHAN,
        THAC_SI,
        TIEN_SI,
        PHO_GIAO_SU,
        GIAO_SU
    }

    public enum LoaiHopDong {
        CO_HUU,
        THINH_GIANG,
        CHUYEN_GIA
    }

    // Enums Vận hành
    public enum TrangThaiChiaSe {
        WAITING,
        READY,
        ON_THE_WAY,
        COMPLETED,
        CANCELLED_BEFORE_START,
        ABORTED_MIDWAY
    }

    public enum TrangThaiYeuCau {
        PENDING,
        ACCEPTED,
        IN_TRANSIT,
        COMPLETED,
        CANCELLED_BY_PASSENGER,
        REJECTED,
        NO_SHOW,
        REPORTED_BY_DRIVER,
        VERIFICATION_FAILED,
        ABORTED_MIDWAY
    }

    public enum TrangThaiChuyenDi {
        ON_THE_WAY,
        COMPLETED,
        CANCELLED_BY_INCIDENT,
        EMERGENCY_ABORTED,
        SUSPICIOUS_LOCKED,
        SIGNAL_LOST
    }

    public enum LoaiDiemDung {
        START_DRIVER,
        PICKUP,
        DROPOFF,
        END_DRIVER
    }

    public enum TrangThaiDiemDung {
        PENDING,
        ARRIVED,
        COMPLETED,
        CANCELLED,
        FROZEN
    }

    public enum LoaiSuCo {
        DRIVER_REPORT_PASSENGER, // Tài xế báo cáo khách
        PASSENGER_REPORT_DRIVER, // Khách báo cáo tài xế
        SIGNAL_LOST, // Xe mất tín hiệu trên đường
        TECHNICAL_INCIDENT, // Hỏng xe giữa đường
        MAX_FAILED_PIN_PICKUP, // Nhập sai mã PIN quá 3 lần ở điểm đón
        MAX_FAILED_PIN_DROPOFF // Nhập sai mã PIN quá 3 lần ở điểm trả
    }

    public enum MucDoSuCo {
        INFO, // Ghi nhận thông thường (Để review)
        WARNING, // Cảnh báo (Trừ điểm uy tín)
        CRITICAL // Nghiêm trọng (Khóa tài khoản, báo công an)
    }

}