package com.zanh.route_sharing.repository.sharedroute.common;

import com.zanh.route_sharing.utils.time.TimePolicy;

public final class PostgisSharedRouteMatchingSql {

    public static final String SEARCH_ROUTE_SCOPE = """
                  AND route.thoi_gian_khoi_hanh_du_kien >= :departureFrom
                  AND route.thoi_gian_khoi_hanh_du_kien <= :departureTo
            """;

    public static final String PREVIEW_ROUTE_SCOPE = """
                  AND route.id = :sharedRouteId
            """;

    public static final String TEMPLATE = withBusinessZone("""
            WITH input AS (
                SELECT
                    ST_SetSRID(
                        ST_MakePoint(
                            CAST(:pickupLongitude AS double precision),
                            CAST(:pickupLatitude AS double precision)
                        ),
                        4326
                    ) AS pickup,
                    ST_SetSRID(
                        ST_MakePoint(
                            CAST(:destinationLongitude AS double precision),
                            CAST(:destinationLatitude AS double precision)
                        ),
                        4326
                    ) AS destination
            ),
            route_scope AS (
                SELECT
                    route.*,
                    (
                        route.thoi_gian_khoi_hanh_du_kien
                        AT TIME ZONE '@@BUSINESS_ZONE@@'
                    )::date AS route_travel_date
                FROM lo_trinh_chia_se route
                WHERE route.trang_thai_lo_trinh = 'OPEN'
                  AND route.so_ghe_con_lai > 0
                  AND route.thoi_gian_khoi_hanh_du_kien > :now
            %s
                  AND route.tai_xe_id <> :actorUserId
                  AND ST_NPoints(route.tuyen_duong_goc) >= 2
            ),
            eligible AS (
                SELECT
                    route.id AS route_id,
                    route.tuyen_duong_goc,
                    route.diem_xuat_phat,
                    route.dia_chi_xuat_phat,
                    route.diem_dich_tai_xe,
                    route.dia_chi_dich_tai_xe,
                    route.thoi_gian_khoi_hanh_du_kien,
                    route.route_travel_date,
                    route.so_ghe_con_lai,
                    route.muc_ho_tro_goi_y_moi_km,

                    driver.id AS driver_id,
                    driver.ho_ten AS driver_name,
                    driver.anh_dai_dien_url AS driver_avatar_url,

                    vehicle.id AS vehicle_id,
                    vehicle.bien_so_xe,
                    vehicle.mau_sac_thuc_te,
                    brand.ten_hang,
                    model.ten_dong_xe,

                    input.pickup,
                    input.destination
                FROM route_scope route
                JOIN nguoi_dung driver
                  ON driver.id = route.tai_xe_id
                 AND driver.trang_thai_tai_khoan = 'ACTIVE'
                JOIN ho_so_tai_xe driver_profile
                  ON driver_profile.nguoi_dung_id = driver.id
                 AND driver_profile.trang_thai_tai_xe = 'ACTIVE'
                JOIN phuong_tien vehicle
                  ON vehicle.id = route.phuong_tien_id
                 AND vehicle.trang_thai_phuong_tien = 'ACTIVE'
                 AND vehicle.nguoi_dang_ky_su_dung_id = driver.id
                JOIN dong_xe model
                  ON model.id = vehicle.dong_xe_id
                 AND model.dang_hoat_dong = TRUE
                JOIN hang_xe brand
                  ON brand.id = model.hang_xe_id
                 AND brand.dang_hoat_dong = TRUE
                CROSS JOIN input
                WHERE EXISTS (
                    SELECT 1
                      FROM nha_truong school
                     WHERE school.id = :schoolId
                       AND school.dang_hoat_dong = TRUE
                )
                  AND EXISTS (
                    SELECT 1
                      FROM nguoi_dung actor
                      JOIN ho_so_thanh_vien actor_membership
                        ON actor_membership.nguoi_dung_id = actor.id
                     WHERE actor.id = :actorUserId
                       AND actor.trang_thai_tai_khoan = 'ACTIVE'
                       AND actor_membership.nha_truong_id = :schoolId
                       AND actor_membership.trang_thai_ho_so = 'APPROVED'
                       AND (actor_membership.ngay_bat_dau_hieu_luc IS NULL
                            OR actor_membership.ngay_bat_dau_hieu_luc
                               <= route.route_travel_date)
                       AND (actor_membership.ngay_ket_thuc_hieu_luc IS NULL
                            OR actor_membership.ngay_ket_thuc_hieu_luc
                               >= route.route_travel_date)
                  )
                  AND EXISTS (
                    SELECT 1
                      FROM ho_so_thanh_vien driver_membership
                     WHERE driver_membership.nguoi_dung_id = driver.id
                       AND driver_membership.nha_truong_id = :schoolId
                       AND driver_membership.trang_thai_ho_so = 'APPROVED'
                       AND (driver_membership.ngay_bat_dau_hieu_luc IS NULL
                            OR driver_membership.ngay_bat_dau_hieu_luc
                               <= route.route_travel_date)
                       AND (driver_membership.ngay_ket_thuc_hieu_luc IS NULL
                            OR driver_membership.ngay_ket_thuc_hieu_luc
                               >= route.route_travel_date)
                  )
                  AND ST_DWithin(
                      CAST(route.tuyen_duong_goc AS geography),
                      CAST(input.pickup AS geography),
                      :maxPickupDeviationMeters
                  )
            ),
            measured AS (
                SELECT
                    eligible.*,

                    ST_ClosestPoint(
                        eligible.tuyen_duong_goc,
                        eligible.pickup
                    ) AS pickup_projection,

                    ST_LineLocatePoint(
                        eligible.tuyen_duong_goc,
                        ST_ClosestPoint(
                            eligible.tuyen_duong_goc,
                            eligible.pickup
                        )
                    ) AS pickup_fraction,

                    ST_Distance(
                        CAST(eligible.tuyen_duong_goc AS geography),
                        CAST(eligible.pickup AS geography)
                    ) AS pickup_deviation_m,

                    ST_ClosestPoint(
                        eligible.tuyen_duong_goc,
                        eligible.diem_dich_tai_xe
                    ) AS driver_destination_projection,

                    ST_LineLocatePoint(
                        eligible.tuyen_duong_goc,
                        ST_ClosestPoint(
                            eligible.tuyen_duong_goc,
                            eligible.diem_dich_tai_xe
                        )
                    ) AS driver_destination_fraction,

                    ST_Distance(
                        CAST(eligible.diem_dich_tai_xe AS geography),
                        CAST(eligible.destination AS geography)
                    ) AS driver_destination_deviation_m,

                    ST_ClosestPoint(
                        eligible.tuyen_duong_goc,
                        eligible.destination
                    ) AS destination_projection,

                    ST_LineLocatePoint(
                        eligible.tuyen_duong_goc,
                        ST_ClosestPoint(
                            eligible.tuyen_duong_goc,
                            eligible.destination
                        )
                    ) AS destination_fraction,

                    ST_Distance(
                        CAST(eligible.tuyen_duong_goc AS geography),
                        CAST(eligible.destination AS geography)
                    ) AS destination_route_deviation_m
                FROM eligible
            ),
            classified AS (
                SELECT
                    measured.*,
                    CASE
                        WHEN measured.driver_destination_deviation_m
                                <= :sameDestinationRadiusMeters
                         AND measured.pickup_fraction
                                < measured.driver_destination_fraction
                            THEN 'CUNG_DIEM_DEN'
                        WHEN measured.destination_route_deviation_m
                                <= :destinationNearRouteRadiusMeters
                         AND measured.pickup_fraction
                                < measured.destination_fraction
                            THEN 'TRUNG_DOAN_TUYEN'
                        ELSE NULL
                    END AS match_type
                FROM measured
            ),
            matched AS (
                SELECT
                    classified.*,

                    CASE
                        WHEN classified.match_type = 'CUNG_DIEM_DEN'
                            THEN 'DIEM_DICH_CUOI_CUNG'
                        ELSE 'DIEM_THA_TRUNG_GIAN'
                    END AS dropoff_type,

                    CASE
                        WHEN classified.match_type = 'CUNG_DIEM_DEN'
                            THEN classified.destination
                        ELSE classified.destination_projection
                    END AS proposed_dropoff,

                    CASE
                        WHEN classified.match_type = 'CUNG_DIEM_DEN'
                            THEN classified.driver_destination_fraction
                        ELSE classified.destination_fraction
                    END AS shared_segment_end_fraction,

                    CASE
                        WHEN classified.match_type = 'CUNG_DIEM_DEN'
                            THEN classified.driver_destination_deviation_m
                        ELSE classified.destination_route_deviation_m
                    END AS destination_deviation_m
                FROM classified
                WHERE classified.match_type IS NOT NULL
            )
            """);

    public static String matchingCte(String routeScopePredicate) {
        if (!SEARCH_ROUTE_SCOPE.equals(routeScopePredicate)
                && !PREVIEW_ROUTE_SCOPE.equals(routeScopePredicate)) {
            throw new IllegalArgumentException("Không hỗ trợ loại route scope predicate");
        }
        return TEMPLATE.formatted(routeScopePredicate);
    }

    private static String withBusinessZone(String sql) {
        return sql.replace("@@BUSINESS_ZONE@@", TimePolicy.BUSINESS_ZONE_ID);
    }

    private PostgisSharedRouteMatchingSql() {
    }
}
