package com.zanh.route_sharing.repository.sharedroute.preview.postgis;

import com.zanh.route_sharing.repository.sharedroute.common.PostgisSharedRouteMatchingSql;

final class PostgisSharedRoutePreviewSql {

  static final String PREVIEW_CONTEXT = """
      SELECT
             cfg.ban_kinh_cung_diem_den_met,
             cfg.ban_kinh_diem_den_gan_tuyen_met,
             cfg.khoang_cach_lech_don_toi_da_met,
             cfg.do_lech_thoi_gian_khoi_hanh_phut
        FROM lo_trinh_chia_se route
        JOIN nguoi_dung actor
          ON actor.id = :actorUserId
         AND actor.trang_thai_tai_khoan = 'ACTIVE'
        JOIN nha_truong school
          ON school.id = :schoolId
         AND school.dang_hoat_dong = TRUE
        JOIN cau_hinh_nghiep_vu cfg
          ON cfg.nha_truong_id = school.id
        JOIN ho_so_thanh_vien actor_membership
          ON actor_membership.nguoi_dung_id = actor.id
         AND actor_membership.nha_truong_id = school.id
         AND actor_membership.trang_thai_ho_so = 'APPROVED'
       WHERE route.id = :sharedRouteId
         AND (actor_membership.ngay_bat_dau_hieu_luc IS NULL
              OR actor_membership.ngay_bat_dau_hieu_luc <= (
                  route.thoi_gian_khoi_hanh_du_kien
                  AT TIME ZONE 'Asia/Ho_Chi_Minh'
              )::date)
         AND (actor_membership.ngay_ket_thuc_hieu_luc IS NULL
              OR actor_membership.ngay_ket_thuc_hieu_luc >= (
                  route.thoi_gian_khoi_hanh_du_kien
                  AT TIME ZONE 'Asia/Ho_Chi_Minh'
              )::date)
       ORDER BY cfg.id ASC, actor_membership.id ASC
       LIMIT 1
      """;

  private static final String MATCHING_CTE = PostgisSharedRouteMatchingSql.matchingCte(
      PostgisSharedRouteMatchingSql.PREVIEW_ROUTE_SCOPE);

  static final String PREPARE = MATCHING_CTE + """
      SELECT
          matched.route_id,
          CAST(:schoolId AS bigint) AS school_id,
          route.version AS route_version,
          route.trang_thai_lo_trinh,

          CAST(ST_Y(matched.diem_xuat_phat) AS numeric) AS origin_latitude,
          CAST(ST_X(matched.diem_xuat_phat) AS numeric) AS origin_longitude,
          matched.dia_chi_xuat_phat,

          CAST(ST_Y(matched.diem_dich_tai_xe) AS numeric) AS driver_destination_latitude,
          CAST(ST_X(matched.diem_dich_tai_xe) AS numeric) AS driver_destination_longitude,
          matched.dia_chi_dich_tai_xe,

          ST_AsGeoJSON(matched.tuyen_duong_goc, 6) AS original_route_geo_json,
          route.khoang_cach_du_kien_met AS original_distance_m,
          route.thoi_luong_du_kien_giay AS original_duration_s,
          matched.thoi_gian_khoi_hanh_du_kien,
          matched.so_ghe_con_lai,
          matched.muc_ho_tro_goi_y_moi_km,

          matched.driver_id,
          matched.driver_name,
          matched.driver_avatar_url,

          matched.vehicle_id,
          matched.bien_so_xe,
          matched.mau_sac_thuc_te,
          matched.ten_hang,
          matched.ten_dong_xe,
          model.loai_phuong_tien,

          matched.match_type,
          matched.dropoff_type,
          CAST(ST_Y(matched.pickup_projection) AS numeric) AS pickup_projection_latitude,
          CAST(ST_X(matched.pickup_projection) AS numeric) AS pickup_projection_longitude,
          CAST(ST_Y(matched.proposed_dropoff) AS numeric) AS proposed_dropoff_latitude,
          CAST(ST_X(matched.proposed_dropoff) AS numeric) AS proposed_dropoff_longitude,
          CAST(ROUND(CAST(matched.pickup_deviation_m AS numeric), 2) AS numeric)
              AS pickup_deviation_m,
          CAST(ROUND(CAST(matched.destination_deviation_m AS numeric), 2) AS numeric)
              AS destination_deviation_m,
          CAST(
              ROUND(
                  CAST(
                      ST_Length(
                          CAST(
                              ST_LineSubstring(
                                  matched.tuyen_duong_goc,
                                  matched.pickup_fraction,
                                  matched.shared_segment_end_fraction
                              ) AS geography
                          )
                      ) AS numeric
                  ),
                  2
              ) AS numeric
          ) AS shared_segment_m,

          actor.id AS actor_user_id,
          actor.version AS actor_user_version,
          actor.security_version AS actor_security_version,
          driver.version AS driver_user_version,
          driver.security_version AS driver_security_version,
          driver_profile.id AS driver_profile_id,
          driver_profile.version AS driver_profile_version,
          vehicle.version AS vehicle_version,
          model.id AS model_id,
          model.version AS model_version,
          brand.id AS brand_id,
          brand.version AS brand_version,
          actor_membership.id AS actor_membership_id,
          actor_membership.version AS actor_membership_version,
          driver_membership.id AS driver_membership_id,
          driver_membership.version AS driver_membership_version,
          school.version AS school_version,
          cfg.id AS business_config_id,
          cfg.version AS business_config_version,
          CAST(:sameDestinationRadiusMeters AS numeric) AS used_same_destination_radius_m,
          CAST(:destinationNearRouteRadiusMeters AS numeric) AS used_destination_near_route_radius_m,
          CAST(:maxPickupDeviationMeters AS numeric) AS used_max_pickup_deviation_m,
          cfg.thoi_gian_lech_don_toi_da_giay AS used_max_pickup_deviation_s,
          cfg.ty_le_tien_duong_toi_thieu AS used_minimum_convenience_ratio,
          cfg.booking_cutoff_seconds AS used_booking_cutoff_s,
          cfg.rejection_cooldown_seconds AS used_rejection_cooldown_s
      FROM matched
      JOIN lo_trinh_chia_se route
        ON route.id = matched.route_id
      JOIN nguoi_dung actor
        ON actor.id = :actorUserId
      JOIN nguoi_dung driver
        ON driver.id = matched.driver_id
      JOIN ho_so_tai_xe driver_profile
        ON driver_profile.nguoi_dung_id = driver.id
       AND driver_profile.trang_thai_tai_xe = 'ACTIVE'
      JOIN phuong_tien vehicle
        ON vehicle.id = matched.vehicle_id
      JOIN dong_xe model
        ON model.id = vehicle.dong_xe_id
      JOIN hang_xe brand
        ON brand.id = model.hang_xe_id
      JOIN nha_truong school
        ON school.id = :schoolId
      JOIN cau_hinh_nghiep_vu cfg
        ON cfg.nha_truong_id = school.id
      JOIN LATERAL (
          SELECT membership.id, membership.version
            FROM ho_so_thanh_vien membership
           WHERE membership.nguoi_dung_id = actor.id
             AND membership.nha_truong_id = school.id
             AND membership.trang_thai_ho_so = 'APPROVED'
             AND (membership.ngay_bat_dau_hieu_luc IS NULL
                  OR membership.ngay_bat_dau_hieu_luc <= matched.route_travel_date)
             AND (membership.ngay_ket_thuc_hieu_luc IS NULL
                  OR membership.ngay_ket_thuc_hieu_luc >= matched.route_travel_date)
           ORDER BY membership.id ASC
           LIMIT 1
      ) actor_membership ON TRUE
      JOIN LATERAL (
          SELECT membership.id, membership.version
            FROM ho_so_thanh_vien membership
           WHERE membership.nguoi_dung_id = driver.id
             AND membership.nha_truong_id = school.id
             AND membership.trang_thai_ho_so = 'APPROVED'
             AND (membership.ngay_bat_dau_hieu_luc IS NULL
                  OR membership.ngay_bat_dau_hieu_luc <= matched.route_travel_date)
             AND (membership.ngay_ket_thuc_hieu_luc IS NULL
                  OR membership.ngay_ket_thuc_hieu_luc >= matched.route_travel_date)
           ORDER BY membership.id ASC
           LIMIT 1
      ) driver_membership ON TRUE
      LIMIT 1
      """;

  static final String DIAGNOSE = """
      SELECT CASE
          WHEN NOT EXISTS (
              SELECT 1
                FROM nguoi_dung actor
                JOIN ho_so_thanh_vien membership
                  ON membership.nguoi_dung_id = actor.id
                JOIN nha_truong school
                  ON school.id = membership.nha_truong_id
                JOIN cau_hinh_nghiep_vu cfg
                  ON cfg.nha_truong_id = school.id
               WHERE actor.id = :actorUserId
                 AND actor.trang_thai_tai_khoan = 'ACTIVE'
                 AND school.id = :schoolId
                 AND school.dang_hoat_dong = TRUE
                 AND membership.trang_thai_ho_so = 'APPROVED'
                 AND (membership.ngay_bat_dau_hieu_luc IS NULL
                      OR membership.ngay_bat_dau_hieu_luc <= (
                          route.thoi_gian_khoi_hanh_du_kien
                          AT TIME ZONE 'Asia/Ho_Chi_Minh'
                      )::date)
                 AND (membership.ngay_ket_thuc_hieu_luc IS NULL
                      OR membership.ngay_ket_thuc_hieu_luc >= (
                          route.thoi_gian_khoi_hanh_du_kien
                          AT TIME ZONE 'Asia/Ho_Chi_Minh'
                      )::date)
          ) THEN 'NOT_FOUND_OR_INACCESSIBLE'
          WHEN NOT EXISTS (
              SELECT 1
                FROM ho_so_thanh_vien membership
               WHERE membership.nguoi_dung_id = route.tai_xe_id
                 AND membership.nha_truong_id = :schoolId
                 AND membership.trang_thai_ho_so = 'APPROVED'
                 AND (membership.ngay_bat_dau_hieu_luc IS NULL
                      OR membership.ngay_bat_dau_hieu_luc <= (
                          route.thoi_gian_khoi_hanh_du_kien
                          AT TIME ZONE 'Asia/Ho_Chi_Minh'
                      )::date)
                 AND (membership.ngay_ket_thuc_hieu_luc IS NULL
                      OR membership.ngay_ket_thuc_hieu_luc >= (
                          route.thoi_gian_khoi_hanh_du_kien
                          AT TIME ZONE 'Asia/Ho_Chi_Minh'
                      )::date)
          ) THEN 'NOT_FOUND_OR_INACCESSIBLE'
          WHEN route.tai_xe_id = :actorUserId THEN 'SELF_ROUTE'
          WHEN route.trang_thai_lo_trinh <> 'OPEN'
            OR route.so_ghe_con_lai <= 0
            OR route.thoi_gian_khoi_hanh_du_kien <= :now
              THEN 'ROUTE_UNAVAILABLE'
          WHEN driver.trang_thai_tai_khoan IS DISTINCT FROM 'ACTIVE'
            OR driver_profile.trang_thai_tai_xe IS DISTINCT FROM 'ACTIVE'
            OR vehicle.trang_thai_phuong_tien IS DISTINCT FROM 'ACTIVE'
            OR vehicle.nguoi_dang_ky_su_dung_id IS DISTINCT FROM driver.id
            OR model.dang_hoat_dong IS DISTINCT FROM TRUE
            OR brand.dang_hoat_dong IS DISTINCT FROM TRUE
              THEN 'DRIVER_OR_VEHICLE_INELIGIBLE'
          ELSE 'NO_LONGER_MATCHES'
      END AS evaluation_status
        FROM lo_trinh_chia_se route
        LEFT JOIN nguoi_dung driver
          ON driver.id = route.tai_xe_id
        LEFT JOIN ho_so_tai_xe driver_profile
          ON driver_profile.nguoi_dung_id = driver.id
        LEFT JOIN phuong_tien vehicle
          ON vehicle.id = route.phuong_tien_id
        LEFT JOIN dong_xe model
          ON model.id = vehicle.dong_xe_id
        LEFT JOIN hang_xe brand
          ON brand.id = model.hang_xe_id
       WHERE route.id = :sharedRouteId
       LIMIT 1
      """;

  static final String REMAINS_CURRENT = """
      SELECT EXISTS (
          SELECT 1
            FROM lo_trinh_chia_se route
            JOIN nguoi_dung actor
              ON actor.id = :actorUserId
             AND actor.version = :actorUserVersion
             AND actor.security_version = :actorSecurityVersion
             AND actor.trang_thai_tai_khoan = 'ACTIVE'
            JOIN nguoi_dung driver
              ON driver.id = :driverId
             AND driver.version = :driverUserVersion
             AND driver.security_version = :driverSecurityVersion
             AND driver.trang_thai_tai_khoan = 'ACTIVE'
            JOIN ho_so_tai_xe driver_profile
              ON driver_profile.id = :driverProfileId
             AND driver_profile.nguoi_dung_id = driver.id
             AND driver_profile.version = :driverProfileVersion
             AND driver_profile.trang_thai_tai_xe = 'ACTIVE'
            JOIN phuong_tien vehicle
              ON vehicle.id = :vehicleId
             AND vehicle.version = :vehicleVersion
             AND vehicle.trang_thai_phuong_tien = 'ACTIVE'
             AND vehicle.nguoi_dang_ky_su_dung_id = driver.id
            JOIN dong_xe model
              ON model.id = :modelId
             AND model.version = :modelVersion
             AND model.dang_hoat_dong = TRUE
             AND model.id = vehicle.dong_xe_id
            JOIN hang_xe brand
              ON brand.id = :brandId
             AND brand.version = :brandVersion
             AND brand.dang_hoat_dong = TRUE
             AND brand.id = model.hang_xe_id
            JOIN nha_truong school
              ON school.id = :schoolId
             AND school.version = :schoolVersion
             AND school.dang_hoat_dong = TRUE
            JOIN cau_hinh_nghiep_vu cfg
              ON cfg.id = :businessConfigId
             AND cfg.version = :businessConfigVersion
             AND cfg.nha_truong_id = school.id
             AND cfg.ban_kinh_cung_diem_den_met = :sameDestinationRadiusMeters
             AND cfg.ban_kinh_diem_den_gan_tuyen_met = :destinationNearRouteRadiusMeters
             AND cfg.khoang_cach_lech_don_toi_da_met = :maxPickupDeviationMeters
             AND cfg.thoi_gian_lech_don_toi_da_giay = :maxPickupDeviationSeconds
             AND cfg.ty_le_tien_duong_toi_thieu = :minimumConvenienceRatioPercent
             AND cfg.booking_cutoff_seconds = :bookingCutoffSeconds
             AND cfg.rejection_cooldown_seconds = :rejectionCooldownSeconds
            JOIN ho_so_thanh_vien actor_membership
              ON actor_membership.id = :actorMembershipId
             AND actor_membership.version = :actorMembershipVersion
             AND actor_membership.nguoi_dung_id = actor.id
             AND actor_membership.nha_truong_id = school.id
             AND actor_membership.trang_thai_ho_so = 'APPROVED'
            JOIN ho_so_thanh_vien driver_membership
              ON driver_membership.id = :driverMembershipId
             AND driver_membership.version = :driverMembershipVersion
             AND driver_membership.nguoi_dung_id = driver.id
             AND driver_membership.nha_truong_id = school.id
             AND driver_membership.trang_thai_ho_so = 'APPROVED'
           WHERE route.id = :routeId
             AND route.version = :routeVersion
             AND route.tai_xe_id = driver.id
             AND route.phuong_tien_id = vehicle.id
             AND route.trang_thai_lo_trinh = 'OPEN'
             AND route.so_ghe_con_lai = :remainingSeats
             AND route.so_ghe_con_lai > 0
             AND route.thoi_gian_khoi_hanh_du_kien = :expectedDepartureTime
             AND route.thoi_gian_khoi_hanh_du_kien > :checkedAt
             AND route.tai_xe_id <> actor.id
             AND (actor_membership.ngay_bat_dau_hieu_luc IS NULL
                  OR actor_membership.ngay_bat_dau_hieu_luc <= (
                      route.thoi_gian_khoi_hanh_du_kien
                      AT TIME ZONE 'Asia/Ho_Chi_Minh'
                  )::date)
             AND (actor_membership.ngay_ket_thuc_hieu_luc IS NULL
                  OR actor_membership.ngay_ket_thuc_hieu_luc >= (
                      route.thoi_gian_khoi_hanh_du_kien
                      AT TIME ZONE 'Asia/Ho_Chi_Minh'
                  )::date)
             AND (driver_membership.ngay_bat_dau_hieu_luc IS NULL
                  OR driver_membership.ngay_bat_dau_hieu_luc <= (
                      route.thoi_gian_khoi_hanh_du_kien
                      AT TIME ZONE 'Asia/Ho_Chi_Minh'
                  )::date)
             AND (driver_membership.ngay_ket_thuc_hieu_luc IS NULL
                  OR driver_membership.ngay_ket_thuc_hieu_luc >= (
                      route.thoi_gian_khoi_hanh_du_kien
                      AT TIME ZONE 'Asia/Ho_Chi_Minh'
                  )::date)
      ) AS current
      """;

  private PostgisSharedRoutePreviewSql() {
  }
}
