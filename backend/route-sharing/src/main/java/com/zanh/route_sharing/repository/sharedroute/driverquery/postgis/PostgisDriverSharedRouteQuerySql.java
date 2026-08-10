package com.zanh.route_sharing.repository.sharedroute.driverquery.postgis;

public final class PostgisDriverSharedRouteQuerySql {

    static final String COUNT_ALL = """
            SELECT COUNT(*)
            FROM lo_trinh_chia_se route
            WHERE route.tai_xe_id = :actorUserId
            """;

    static final String COUNT_BY_STATUS = """
            SELECT COUNT(*)
            FROM lo_trinh_chia_se route
            WHERE route.tai_xe_id = :actorUserId
              AND route.trang_thai_lo_trinh = :status
            """;

    private static final String BOOKING_AGGREGATE = """
            LEFT JOIN LATERAL (
                SELECT
                    COUNT(*) AS total_requests,
                    COUNT(*) FILTER (WHERE request.trang_thai_yeu_cau = 'PENDING') AS pending_requests,
                    COUNT(*) FILTER (WHERE request.trang_thai_yeu_cau = 'ACCEPTED') AS accepted_bookings,
                    COUNT(*) FILTER (WHERE request.trang_thai_yeu_cau = 'REJECTED') AS rejected_requests,
                    COUNT(*) FILTER (WHERE request.trang_thai_yeu_cau = 'CANCELLED_BY_PASSENGER') AS cancelled_by_passenger,
                    COUNT(*) FILTER (WHERE request.trang_thai_yeu_cau = 'CANCELLED_BY_DRIVER') AS cancelled_by_driver
                FROM yeu_cau_di_chung request
                WHERE request.lo_trinh_chia_se_id = route.id
            ) booking ON TRUE
            """;

    private static final String PAGE_SELECT = """
            SELECT
                route.id AS route_id,
                route.trang_thai_lo_trinh AS route_status,
                route.created_at AS created_at,
                route.thoi_gian_khoi_hanh_du_kien AS expected_departure_time,
                ST_Y(route.diem_xuat_phat) AS origin_latitude,
                ST_X(route.diem_xuat_phat) AS origin_longitude,
                route.dia_chi_xuat_phat AS origin_address,
                ST_Y(route.diem_dich_tai_xe) AS destination_latitude,
                ST_X(route.diem_dich_tai_xe) AS destination_longitude,
                route.dia_chi_dich_tai_xe AS destination_address,
                route.so_ghe_cung_cap AS offered_seats,
                route.so_ghe_con_lai AS remaining_seats,
                vehicle.id AS vehicle_id,
                vehicle.bien_so_xe AS license_plate,
                vehicle.mau_sac_thuc_te AS actual_color,
                brand.ten_hang AS brand_name,
                model.ten_dong_xe AS model_name,
                COALESCE(booking.total_requests, 0) AS total_requests,
                COALESCE(booking.pending_requests, 0) AS pending_requests,
                COALESCE(booking.accepted_bookings, 0) AS accepted_bookings,
                COALESCE(booking.rejected_requests, 0) AS rejected_requests,
                COALESCE(booking.cancelled_by_passenger, 0) AS cancelled_by_passenger,
                COALESCE(booking.cancelled_by_driver, 0) AS cancelled_by_driver,
                EXISTS (
                    SELECT 1
                    FROM chuyen_di trip
                    WHERE trip.lo_trinh_chia_se_id = route.id
                ) AS assigned_to_trip
            FROM lo_trinh_chia_se route
            JOIN phuong_tien vehicle ON vehicle.id = route.phuong_tien_id
            JOIN dong_xe model ON model.id = vehicle.dong_xe_id
            JOIN hang_xe brand ON brand.id = model.hang_xe_id
            """ + BOOKING_AGGREGATE;

    static final String PAGE_ALL = PAGE_SELECT + """
            WHERE route.tai_xe_id = :actorUserId
            ORDER BY route.created_at DESC, route.id DESC
            LIMIT :size OFFSET :offset
            """;

    static final String PAGE_BY_STATUS = PAGE_SELECT + """
            WHERE route.tai_xe_id = :actorUserId
              AND route.trang_thai_lo_trinh = :status
            ORDER BY route.created_at DESC, route.id DESC
            LIMIT :size OFFSET :offset
            """;

    static final String DETAIL = """
            SELECT
                route.id AS route_id,
                route.trang_thai_lo_trinh AS route_status,
                route.created_at AS created_at,
                route.thoi_gian_khoi_hanh_du_kien AS expected_departure_time,
                ST_Y(route.diem_xuat_phat) AS origin_latitude,
                ST_X(route.diem_xuat_phat) AS origin_longitude,
                route.dia_chi_xuat_phat AS origin_address,
                ST_Y(route.diem_dich_tai_xe) AS destination_latitude,
                ST_X(route.diem_dich_tai_xe) AS destination_longitude,
                route.dia_chi_dich_tai_xe AS destination_address,
                route.so_ghe_cung_cap AS offered_seats,
                route.so_ghe_con_lai AS remaining_seats,
                route.muc_ho_tro_goi_y_moi_km AS suggested_support_per_km,
                ST_AsGeoJSON(route.tuyen_duong_goc) AS original_route_geo_json,
                route.khoang_cach_du_kien_met AS original_distance_meters,
                route.thoi_luong_du_kien_giay AS original_duration_seconds,
                vehicle.id AS vehicle_id,
                vehicle.bien_so_xe AS license_plate,
                vehicle.mau_sac_thuc_te AS actual_color,
                brand.ten_hang AS brand_name,
                model.ten_dong_xe AS model_name,
                COALESCE(booking.total_requests, 0) AS total_requests,
                COALESCE(booking.pending_requests, 0) AS pending_requests,
                COALESCE(booking.accepted_bookings, 0) AS accepted_bookings,
                COALESCE(booking.rejected_requests, 0) AS rejected_requests,
                COALESCE(booking.cancelled_by_passenger, 0) AS cancelled_by_passenger,
                COALESCE(booking.cancelled_by_driver, 0) AS cancelled_by_driver,
                EXISTS (
                    SELECT 1
                    FROM chuyen_di trip
                    WHERE trip.lo_trinh_chia_se_id = route.id
                ) AS assigned_to_trip,
                (
                    SELECT trip.id
                    FROM chuyen_di trip
                    WHERE trip.lo_trinh_chia_se_id = route.id
                ) AS trip_id,
                route.huy_luc AS cancelled_at,
                route.ly_do_huy AS cancellation_reason
            FROM lo_trinh_chia_se route
            JOIN phuong_tien vehicle ON vehicle.id = route.phuong_tien_id
            JOIN dong_xe model ON model.id = vehicle.dong_xe_id
            JOIN hang_xe brand ON brand.id = model.hang_xe_id
            """ + BOOKING_AGGREGATE + """
            WHERE route.id = :routeId
              AND route.tai_xe_id = :actorUserId
            """;

    private PostgisDriverSharedRouteQuerySql() {
    }
}
