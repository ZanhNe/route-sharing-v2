package com.zanh.route_sharing.repository.sharedroute.tripquery.postgis;

import com.zanh.route_sharing.repository.sharedroute.triplocation.model.TripLocationCurrentOrdering;

final class PostgisTripDetailQuerySql {

  static final String HEADER = """
      SELECT
          CASE WHEN route.tai_xe_id = :actorUserId THEN 'DRIVER' ELSE 'PASSENGER' END AS viewer_role,
          trip.id AS trip_id,
          trip.trang_thai_van_hanh AS trip_status,
          trip.trang_thai_giam_sat AS monitoring_status,
          COALESCE(trip.nhan_tin_hieu_cuoi_luc, trip.bat_dau_luc) AS signal_reference_at,
          route.chot_danh_sach_luc AS formed_at,
          trip.bat_dau_luc AS started_at,
          trip.ket_thuc_luc AS ended_at,
          route.huy_luc AS cancelled_at,
          route.ly_do_huy AS cancellation_reason,
          trip.dong_bang_luc AS safety_hold_started_at,
          trip.ly_do_dong_bang AS safety_message,
          (SELECT count(*) FROM can_thiep_an_toan_chuyen_di active_hold_count
             WHERE active_hold_count.chuyen_di_id = trip.id
               AND active_hold_count.loai_can_thiep = 'GIU_DE_XUONG_XE_AN_TOAN'
               AND active_hold_count.trang_thai_can_thiep = 'DANG_THUC_HIEN') AS active_safety_hold_count,
          (SELECT min(active_hold.id) FROM can_thiep_an_toan_chuyen_di active_hold
             WHERE active_hold.chuyen_di_id = trip.id
               AND active_hold.loai_can_thiep = 'GIU_DE_XUONG_XE_AN_TOAN'
               AND active_hold.trang_thai_can_thiep = 'DANG_THUC_HIEN') AS active_safety_hold_intervention_id,
          (SELECT min(active_hold.yeu_cau_muc_tieu_id) FROM can_thiep_an_toan_chuyen_di active_hold
             WHERE active_hold.chuyen_di_id = trip.id
               AND active_hold.loai_can_thiep = 'GIU_DE_XUONG_XE_AN_TOAN'
               AND active_hold.trang_thai_can_thiep = 'DANG_THUC_HIEN') AS active_safety_hold_target_ride_request_id,
          trip.so_khach_ke_hoach AS planned_passenger_count,
          trip.so_khach_thuc_te AS actual_passenger_count,
          driver_start.trang_thai_diem_dung AS driver_start_status,
          driver_start.hoan_thanh_luc AS driver_start_completed_at,
          driver_end.trang_thai_diem_dung AS driver_end_status,
          driver_end.hoan_thanh_luc AS driver_end_completed_at,
          (driver_end.toa_do_thuc_te IS NOT NULL) AS driver_end_has_actual_point,
          ST_AsGeoJSON(trip.tuyen_duong_van_hanh) AS operational_route_geo_json,
          route.id AS route_id,
          route.trang_thai_lo_trinh AS route_status,
          route.chot_danh_sach_luc AS locked_at,
          route.thoi_gian_khoi_hanh_du_kien AS expected_departure_time,
          route.so_ghe_cung_cap AS offered_seats,
          route.so_ghe_con_lai AS remaining_seats,
          ST_Y(route.diem_xuat_phat) AS origin_latitude,
          ST_X(route.diem_xuat_phat) AS origin_longitude,
          route.dia_chi_xuat_phat AS origin_address,
          ST_Y(route.diem_dich_tai_xe) AS destination_latitude,
          ST_X(route.diem_dich_tai_xe) AS destination_longitude,
          route.dia_chi_dich_tai_xe AS destination_address,
          driver.id AS driver_id,
          driver.ho_ten AS driver_full_name,
          driver.anh_dai_dien_url AS driver_avatar_url,
          vehicle.id AS vehicle_id,
          vehicle.bien_so_xe AS license_plate,
          vehicle.mau_sac_thuc_te AS actual_color,
          brand.ten_hang AS brand_name,
          model.ten_dong_xe AS model_name
      FROM chuyen_di trip
      JOIN lo_trinh_chia_se route ON route.id = trip.lo_trinh_chia_se_id
      JOIN nguoi_dung driver ON driver.id = route.tai_xe_id
      JOIN phuong_tien vehicle ON vehicle.id = route.phuong_tien_id
      JOIN dong_xe model ON model.id = vehicle.dong_xe_id
      JOIN hang_xe brand ON brand.id = model.hang_xe_id
      LEFT JOIN diem_dung_hanh_trinh driver_start
        ON driver_start.chuyen_di_id = trip.id
       AND driver_start.loai_diem_dung = 'DRIVER_START'
      LEFT JOIN diem_dung_hanh_trinh driver_end
        ON driver_end.chuyen_di_id = trip.id
       AND driver_end.loai_diem_dung = 'DRIVER_END'
      WHERE trip.id = :tripId
        AND (
              route.tai_xe_id = :actorUserId
              OR EXISTS (
                  SELECT 1
                  FROM yeu_cau_di_chung own_request
                  WHERE own_request.chuyen_di_id = trip.id
                    AND own_request.hanh_khach_id = :actorUserId
              )
        )
      """;

  static final String PARTICIPANTS_DRIVER = """
      SELECT
          request.id AS ride_request_id,
          passenger.id AS passenger_id,
          passenger.ho_ten AS passenger_full_name,
          passenger.anh_dai_dien_url AS passenger_avatar_url,
          request.trang_thai_yeu_cau AS request_status,
          request.chap_nhan_luc AS accepted_at,
          request.len_xe_luc AS boarded_at,
          request.khong_den_luc AS no_show_at,
          request.xuong_xe_luc AS dropped_off_at,
          request.loai_ghep_tuyen AS match_type,
          request.loai_diem_tha AS dropoff_type,
          request.muc_ho_tro_da_thoa_thuan AS agreed_support_amount,
          request.ghi_chu AS note,
          pickup.id AS pickup_stop_id,
          pickup.thu_tu AS pickup_stop_order,
          dropoff.id AS dropoff_stop_id
      FROM yeu_cau_di_chung request
      JOIN nguoi_dung passenger ON passenger.id = request.hanh_khach_id
      JOIN diem_dung_hanh_trinh pickup
        ON pickup.yeu_cau_di_chung_id = request.id
       AND pickup.loai_diem_dung = 'PICKUP'
      JOIN diem_dung_hanh_trinh dropoff
        ON dropoff.yeu_cau_di_chung_id = request.id
       AND dropoff.loai_diem_dung = 'DROPOFF'
      WHERE request.chuyen_di_id = :tripId
      ORDER BY pickup.thu_tu ASC, request.id ASC
      """;

  static final String PARTICIPANTS_PASSENGER = PARTICIPANTS_DRIVER.replace(
      "WHERE request.chuyen_di_id = :tripId",
      "WHERE request.chuyen_di_id = :tripId\n  AND request.hanh_khach_id = :actorUserId");

  static final String STOPS_DRIVER = """
      SELECT
          stop.id AS stop_id,
          stop.thu_tu AS stop_order,
          stop.loai_diem_dung AS stop_type,
          stop.trang_thai_diem_dung AS stop_status,
          stop.yeu_cau_di_chung_id AS ride_request_id,
          ST_Y(stop.toa_do_ke_hoach) AS stop_latitude,
          ST_X(stop.toa_do_ke_hoach) AS stop_longitude,
          stop.dia_chi AS stop_address,
          stop.den_luc AS arrived_at,
          stop.bat_dau_cho_luc AS waiting_started_at,
          stop.han_cho_luc AS waiting_deadline,
          stop.hoan_thanh_luc AS completed_at
      FROM diem_dung_hanh_trinh stop
      WHERE stop.chuyen_di_id = :tripId
      ORDER BY stop.thu_tu ASC, stop.id ASC
      """;

  static final String STOPS_PASSENGER = """
      SELECT
          stop.id AS stop_id,
          stop.thu_tu AS stop_order,
          stop.loai_diem_dung AS stop_type,
          stop.trang_thai_diem_dung AS stop_status,
          stop.yeu_cau_di_chung_id AS ride_request_id,
          ST_Y(stop.toa_do_ke_hoach) AS stop_latitude,
          ST_X(stop.toa_do_ke_hoach) AS stop_longitude,
          stop.dia_chi AS stop_address,
          stop.den_luc AS arrived_at,
          stop.bat_dau_cho_luc AS waiting_started_at,
          stop.han_cho_luc AS waiting_deadline,
          stop.hoan_thanh_luc AS completed_at
      FROM diem_dung_hanh_trinh stop
      JOIN yeu_cau_di_chung request ON request.id = stop.yeu_cau_di_chung_id
      WHERE stop.chuyen_di_id = :tripId
        AND request.hanh_khach_id = :actorUserId
        AND stop.loai_diem_dung IN ('PICKUP', 'DROPOFF')
      ORDER BY stop.thu_tu ASC, stop.id ASC
      """;

  static final String CURRENT_LOCATION = """
      SELECT
          ST_Y(toa_do) AS latitude,
          ST_X(toa_do) AS longitude,
          thoi_gian_trinh_duyet AS observed_at,
          thoi_gian_server_nhan AS received_at,
          do_chinh_xac_met AS accuracy_meters,
          thu_tu_ban_ghi AS location_sequence
      FROM ban_ghi_dinh_vi
      WHERE chuyen_di_id = :tripId
      """ + TripLocationCurrentOrdering.SQL_ORDER_BY + " LIMIT 1";

  private PostgisTripDetailQuerySql() {
  }
}
