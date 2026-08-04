package com.zanh.route_sharing.repository.sharedroute.search.postgis;

import com.zanh.route_sharing.repository.sharedroute.common.PostgisSharedRouteMatchingSql;

final class PostgisSharedRouteSearchSql {

    static final String SEARCH_CONTEXT = """
            SELECT
                   cfg.ban_kinh_cung_diem_den_met,
                   cfg.ban_kinh_diem_den_gan_tuyen_met,
                   cfg.khoang_cach_lech_don_toi_da_met,
                   cfg.do_lech_thoi_gian_khoi_hanh_phut
              FROM nguoi_dung actor
              JOIN ho_so_thanh_vien actor_membership
                ON actor_membership.nguoi_dung_id = actor.id
              JOIN nha_truong school
                ON school.id = actor_membership.nha_truong_id
              JOIN cau_hinh_nghiep_vu cfg
                ON cfg.nha_truong_id = school.id
             WHERE actor.id = :actorUserId
               AND actor.trang_thai_tai_khoan = 'ACTIVE'
               AND school.id = :schoolId
               AND school.dang_hoat_dong = TRUE
               AND actor_membership.trang_thai_ho_so = 'APPROVED'
               AND (actor_membership.ngay_bat_dau_hieu_luc IS NULL
                    OR actor_membership.ngay_bat_dau_hieu_luc <= :requestedTravelDate)
               AND (actor_membership.ngay_ket_thuc_hieu_luc IS NULL
                    OR actor_membership.ngay_ket_thuc_hieu_luc >= :requestedTravelDate)
             ORDER BY cfg.id ASC
             LIMIT 1
            """;

    private static final String MATCHING_CTE = PostgisSharedRouteMatchingSql.matchingCte(
            PostgisSharedRouteMatchingSql.SEARCH_ROUTE_SCOPE);

    static final String COUNT = MATCHING_CTE + """
            SELECT COUNT(*) AS total_elements
              FROM matched
            """;

    static final String DATA = MATCHING_CTE + """
            SELECT
                matched.route_id,
                matched.match_type,
                matched.dropoff_type,

                matched.driver_id,
                matched.driver_name,
                matched.driver_avatar_url,

                matched.vehicle_id,
                matched.bien_so_xe,
                matched.mau_sac_thuc_te,
                matched.ten_hang,
                matched.ten_dong_xe,

                CAST(ST_Y(matched.diem_xuat_phat) AS numeric)
                    AS origin_latitude,
                CAST(ST_X(matched.diem_xuat_phat) AS numeric)
                    AS origin_longitude,
                matched.dia_chi_xuat_phat,

                CAST(ST_Y(matched.diem_dich_tai_xe) AS numeric)
                    AS driver_destination_latitude,
                CAST(ST_X(matched.diem_dich_tai_xe) AS numeric)
                    AS driver_destination_longitude,
                matched.dia_chi_dich_tai_xe,

                CAST(ST_Y(matched.pickup_projection) AS numeric)
                    AS pickup_projection_latitude,
                CAST(ST_X(matched.pickup_projection) AS numeric)
                    AS pickup_projection_longitude,

                CAST(ST_Y(matched.proposed_dropoff) AS numeric)
                    AS proposed_dropoff_latitude,
                CAST(ST_X(matched.proposed_dropoff) AS numeric)
                    AS proposed_dropoff_longitude,

                ST_AsGeoJSON(matched.tuyen_duong_goc, 6)
                    AS route_geo_json,
                matched.thoi_gian_khoi_hanh_du_kien,
                matched.so_ghe_con_lai,
                matched.muc_ho_tro_goi_y_moi_km,

                CAST(
                    ROUND(CAST(matched.pickup_deviation_m AS numeric), 2)
                    AS numeric
                ) AS pickup_deviation_m,
                CAST(
                    ROUND(CAST(matched.destination_deviation_m AS numeric), 2)
                    AS numeric
                ) AS destination_deviation_m,
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
                ) AS shared_segment_m
            FROM matched
            ORDER BY
                CASE matched.match_type
                    WHEN 'CUNG_DIEM_DEN' THEN 0
                    ELSE 1
                END,
                matched.thoi_gian_khoi_hanh_du_kien ASC,
                matched.pickup_deviation_m ASC,
                matched.destination_deviation_m ASC,
                matched.route_id ASC
            LIMIT :limit
            OFFSET :offset
            """;

    private PostgisSharedRouteSearchSql() {
    }
}
