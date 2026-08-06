package com.zanh.route_sharing.repository.sharedroute.riderequest.query.postgis;

final class PostgisRouteRideRequestQuerySql {

  static final String OWNED_ROUTE = """
      SELECT
          route.id AS route_id,
          route.trang_thai_lo_trinh AS route_status,
          route.thoi_gian_khoi_hanh_du_kien AS expected_departure_time,
          route.so_ghe_cung_cap AS offered_seats,
          route.so_ghe_con_lai AS remaining_seats,
          ST_Y(route.diem_xuat_phat) AS origin_latitude,
          ST_X(route.diem_xuat_phat) AS origin_longitude,
          route.dia_chi_xuat_phat AS origin_address,
          ST_Y(route.diem_dich_tai_xe) AS driver_destination_latitude,
          ST_X(route.diem_dich_tai_xe) AS driver_destination_longitude,
          route.dia_chi_dich_tai_xe AS driver_destination_address,
          ST_AsGeoJSON(route.tuyen_duong_goc) AS original_route_geo_json,
          route.khoang_cach_du_kien_met AS original_distance_meters,
          route.thoi_luong_du_kien_giay AS original_duration_seconds
      FROM lo_trinh_chia_se route
      WHERE route.id = :routeId
        AND route.tai_xe_id = :actorUserId
      """;

  static final String COUNT_PENDING = """
      SELECT COUNT(*)
      FROM yeu_cau_di_chung request
      WHERE request.lo_trinh_chia_se_id = :routeId
        AND request.trang_thai_yeu_cau = 'PENDING'
      """;

  static final String PENDING_PAGE = """
      SELECT
          request.id AS ride_request_id,
          request.trang_thai_yeu_cau AS request_status,
          request.gui_luc AS sent_at,
          request.expires_at AS expires_at,
          passenger.id AS passenger_id,
          passenger.ho_ten AS passenger_full_name,
          passenger.anh_dai_dien_url AS passenger_avatar_url,
          request.dia_chi_don_thuc_te AS pickup_address,
          request.dia_chi_dich_cuoi_cung AS passenger_destination_address,
          request.loai_ghep_tuyen AS match_type,
          request.loai_diem_tha AS dropoff_type,
          request.muc_ho_tro_hanh_khach_de_nghi AS proposed_support_amount
      FROM yeu_cau_di_chung request
      JOIN nguoi_dung passenger ON passenger.id = request.hanh_khach_id
      WHERE request.lo_trinh_chia_se_id = :routeId
        AND request.trang_thai_yeu_cau = 'PENDING'
      ORDER BY request.gui_luc ASC, request.id ASC
      LIMIT :size OFFSET :offset
      """;

  static final String PENDING_DETAIL = """
      SELECT
          request.id AS ride_request_id,
          request.trang_thai_yeu_cau AS request_status,
          request.gui_luc AS sent_at,
          request.expires_at AS expires_at,
          request.ghi_chu AS note,
          passenger.id AS passenger_id,
          passenger.ho_ten AS passenger_full_name,
          passenger.anh_dai_dien_url AS passenger_avatar_url,
          passenger.gioi_tinh AS passenger_gender,
          passenger.ngay_sinh AS passenger_date_of_birth,
          ST_Y(request.diem_don_thuc_te) AS pickup_latitude,
          ST_X(request.diem_don_thuc_te) AS pickup_longitude,
          request.dia_chi_don_thuc_te AS pickup_address,
          ST_Y(request.diem_dich_cuoi_cung_mong_muon) AS passenger_destination_latitude,
          ST_X(request.diem_dich_cuoi_cung_mong_muon) AS passenger_destination_longitude,
          request.dia_chi_dich_cuoi_cung AS passenger_destination_address,
          ST_Y(request.diem_tha_de_xuat) AS proposed_dropoff_latitude,
          ST_X(request.diem_tha_de_xuat) AS proposed_dropoff_longitude,
          request.dia_chi_diem_tha AS proposed_dropoff_address,
          request.loai_ghep_tuyen AS match_type,
          request.loai_diem_tha AS dropoff_type,
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
          request.thoi_gian_khoi_hanh_luc_gui AS departure_time_at_request
      FROM yeu_cau_di_chung request
      JOIN nguoi_dung passenger ON passenger.id = request.hanh_khach_id
      WHERE request.id = :rideRequestId
        AND request.lo_trinh_chia_se_id = :routeId
        AND request.trang_thai_yeu_cau = 'PENDING'
      """;

  private PostgisRouteRideRequestQuerySql() {
  }
}
