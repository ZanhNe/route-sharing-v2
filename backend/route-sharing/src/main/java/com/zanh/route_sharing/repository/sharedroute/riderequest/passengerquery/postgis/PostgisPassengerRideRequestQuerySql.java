package com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.postgis;

final class PostgisPassengerRideRequestQuerySql {

        static final String COUNT_ALL = """
                        SELECT COUNT(*)
                        FROM yeu_cau_di_chung request
                        WHERE request.hanh_khach_id = :actorUserId
                        """;

        static final String COUNT_BY_STATUS = """
                        SELECT COUNT(*)
                        FROM yeu_cau_di_chung request
                        WHERE request.hanh_khach_id = :actorUserId
                          AND request.trang_thai_yeu_cau = :status
                        """;

        private static final String SUMMARY_SELECT = """
                        SELECT
                            request.id AS ride_request_id,
                            request.trang_thai_yeu_cau AS request_status,
                            request.gui_luc AS sent_at,
                            route.id AS route_id,
                            route.trang_thai_lo_trinh AS route_status,
                            route.dia_chi_xuat_phat AS route_origin_address,
                            route.dia_chi_dich_tai_xe AS route_destination_address,
                            route.thoi_gian_khoi_hanh_du_kien AS expected_departure_time,
                            driver.id AS driver_id,
                            driver.ho_ten AS driver_full_name,
                            driver.anh_dai_dien_url AS driver_avatar_url,
                            vehicle.id AS vehicle_id,
                            vehicle.bien_so_xe AS license_plate,
                            vehicle.mau_sac_thuc_te AS actual_color,
                            brand.ten_hang AS brand_name,
                            model.ten_dong_xe AS model_name,
                            request.loai_ghep_tuyen AS match_type,
                            request.loai_diem_tha AS dropoff_type,
                            request.dia_chi_don_thuc_te AS pickup_address,
                            request.dia_chi_dich_cuoi_cung AS passenger_destination_address,
                            request.dia_chi_diem_tha AS proposed_dropoff_address,
                            request.muc_ho_tro_hanh_khach_de_nghi AS proposed_support_amount,
                            request.muc_ho_tro_da_thoa_thuan AS agreed_support_amount,
                            request.chap_nhan_luc AS accepted_at,
                            request.tu_choi_luc AS rejected_at,
                            request.cooldown_until AS cooldown_until,
                            request.huy_luc AS cancelled_at,
                            request.ly_do_huy AS cancellation_reason,
                            (request.chuyen_di_id IS NOT NULL) AS assigned_to_trip
                        FROM yeu_cau_di_chung request
                        JOIN lo_trinh_chia_se route ON route.id = request.lo_trinh_chia_se_id
                        JOIN nguoi_dung driver ON driver.id = request.tai_xe_id_luc_gui
                        JOIN phuong_tien vehicle ON vehicle.id = route.phuong_tien_id
                        JOIN dong_xe model ON model.id = vehicle.dong_xe_id
                        JOIN hang_xe brand ON brand.id = model.hang_xe_id
                        """;

        static final String PAGE_ALL = SUMMARY_SELECT + """
                        WHERE request.hanh_khach_id = :actorUserId
                        ORDER BY request.gui_luc DESC, request.id DESC
                        LIMIT :size OFFSET :offset
                        """;

        static final String PAGE_BY_STATUS = SUMMARY_SELECT + """
                        WHERE request.hanh_khach_id = :actorUserId
                          AND request.trang_thai_yeu_cau = :status
                        ORDER BY request.gui_luc DESC, request.id DESC
                        LIMIT :size OFFSET :offset
                        """;

        static final String DETAIL = """
                        SELECT
                            request.id AS ride_request_id,
                            request.trang_thai_yeu_cau AS request_status,
                            request.gui_luc AS sent_at,
                            request.chap_nhan_luc AS accepted_at,
                            request.tu_choi_luc AS rejected_at,
                            request.cooldown_until AS cooldown_until,
                            request.huy_luc AS cancelled_at,
                            request.ly_do_huy AS cancellation_reason,
                            (request.chuyen_di_id IS NOT NULL) AS assigned_to_trip,
                            request.chuyen_di_id AS trip_id,

                            route.id AS route_id,
                            route.trang_thai_lo_trinh AS route_status,
                            route.thoi_gian_khoi_hanh_du_kien AS expected_departure_time,
                            route.so_ghe_cung_cap AS offered_seats,
                            route.so_ghe_con_lai AS remaining_seats,
                            ST_Y(route.diem_xuat_phat) AS origin_latitude,
                            ST_X(route.diem_xuat_phat) AS origin_longitude,
                            route.dia_chi_xuat_phat AS origin_address,
                            ST_Y(route.diem_dich_tai_xe) AS destination_latitude,
                            ST_X(route.diem_dich_tai_xe) AS destination_longitude,
                            route.dia_chi_dich_tai_xe AS destination_address,
                            ST_AsGeoJSON(route.tuyen_duong_goc) AS original_route_geo_json,
                            route.khoang_cach_du_kien_met AS original_distance_meters,
                            route.thoi_luong_du_kien_giay AS original_duration_seconds,

                            driver.id AS driver_id,
                            driver.ho_ten AS driver_full_name,
                            driver.anh_dai_dien_url AS driver_avatar_url,

                            vehicle.id AS vehicle_id,
                            vehicle.bien_so_xe AS license_plate,
                            vehicle.mau_sac_thuc_te AS actual_color,
                            brand.ten_hang AS brand_name,
                            model.ten_dong_xe AS model_name,

                            request.loai_ghep_tuyen AS match_type,
                            request.loai_diem_tha AS dropoff_type,
                            ST_Y(request.diem_don_thuc_te) AS pickup_latitude,
                            ST_X(request.diem_don_thuc_te) AS pickup_longitude,
                            request.dia_chi_don_thuc_te AS pickup_address,
                            ST_Y(request.diem_dich_cuoi_cung_mong_muon) AS passenger_destination_latitude,
                            ST_X(request.diem_dich_cuoi_cung_mong_muon) AS passenger_destination_longitude,
                            request.dia_chi_dich_cuoi_cung AS passenger_destination_address,
                            ST_Y(request.diem_tha_de_xuat) AS proposed_dropoff_latitude,
                            ST_X(request.diem_tha_de_xuat) AS proposed_dropoff_longitude,
                            request.dia_chi_diem_tha AS proposed_dropoff_address,
                            ST_AsGeoJSON(request.tuyen_duong_mong_muon_hanh_khach) AS passenger_desired_route_geo_json,
                            ST_AsGeoJSON(request.doan_tuyen_duoc_phuc_vu) AS served_segment_geo_json,
                            request.khoang_cach_lech_de_don_met AS pickup_deviation_meters,
                            request.thoi_gian_lech_de_don_giay AS pickup_deviation_seconds,
                            request.tong_khoang_cach_mong_muon_met AS passenger_desired_distance_meters,
                            request.khoang_cach_duoc_phuc_vu_met AS served_distance_meters,
                            request.khoang_cach_con_lai_met AS remaining_distance_meters,
                            request.ty_le_tien_duong AS convenience_ratio_percent,
                            request.muc_ho_tro_goi_y_moi_km_luc_gui AS suggested_support_per_km_at_request,
                            request.muc_ho_tro_hanh_khach_de_nghi AS proposed_support_amount,
                            request.muc_ho_tro_da_thoa_thuan AS agreed_support_amount,
                            request.thoi_gian_khoi_hanh_luc_gui AS departure_time_at_request,
                            request.ghi_chu AS note
                        FROM yeu_cau_di_chung request
                        JOIN lo_trinh_chia_se route ON route.id = request.lo_trinh_chia_se_id
                        JOIN nguoi_dung driver ON driver.id = request.tai_xe_id_luc_gui
                        JOIN phuong_tien vehicle ON vehicle.id = route.phuong_tien_id
                        JOIN dong_xe model ON model.id = vehicle.dong_xe_id
                        JOIN hang_xe brand ON brand.id = model.hang_xe_id
                        WHERE request.id = :rideRequestId
                          AND request.hanh_khach_id = :actorUserId
                        """;

        private PostgisPassengerRideRequestQuerySql() {
        }
}
