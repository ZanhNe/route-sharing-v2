package com.zanh.route_sharing.repository;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.sql.*;

@Repository
@RequiredArgsConstructor
public class PostgisSharedRouteSearchRepository implements SharedRouteSearchRepository {

    private static final String SEARCH_CONTEXT_SQL = """
            SELECT DISTINCT
                   cfg.ban_kinh_cung_diem_den_met,
                   cfg.ban_kinh_diem_den_gan_tuyen_met,
                   cfg.khoang_cach_lech_don_toi_da_met,
                   cfg.do_lech_thoi_gian_khoi_hanh_phut
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
                    OR membership.ngay_bat_dau_hieu_luc <= :membershipDate)
               AND (membership.ngay_ket_thuc_hieu_luc IS NULL
                    OR membership.ngay_ket_thuc_hieu_luc >= :membershipDate)
             LIMIT 1
            """;

    /*
     * PostgreSQL/PostGIS convention:
     * - x = longitude, y = latitude.
     * - geometry SRID 4326 is retained for topology and line operations.
     * - geography casts are used only where the result must be in metres.
     */
    private static final String MATCHING_CTE = """
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
            eligible AS (
                SELECT
                    route.id AS route_id,
                    route.tuyen_duong_goc,
                    route.diem_xuat_phat,
                    route.dia_chi_xuat_phat,
                    route.diem_dich_tai_xe,
                    route.dia_chi_dich_tai_xe,
                    route.thoi_gian_khoi_hanh_du_kien,
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
                FROM lo_trinh_chia_se route
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
                WHERE route.trang_thai_lo_trinh = 'OPEN'
                  AND route.so_ghe_con_lai > 0
                  AND route.thoi_gian_khoi_hanh_du_kien > :now
                  AND route.thoi_gian_khoi_hanh_du_kien >= :departureFrom
                  AND route.thoi_gian_khoi_hanh_du_kien <= :departureTo
                  AND route.tai_xe_id <> :actorUserId
                  AND ST_NPoints(route.tuyen_duong_goc) >= 2
                  AND EXISTS (
                      SELECT 1
                        FROM ho_so_thanh_vien driver_membership
                       WHERE driver_membership.nguoi_dung_id = driver.id
                         AND driver_membership.nha_truong_id = :schoolId
                         AND driver_membership.trang_thai_ho_so = 'APPROVED'
                         AND (driver_membership.ngay_bat_dau_hieu_luc IS NULL
                              OR driver_membership.ngay_bat_dau_hieu_luc <= :membershipDate)
                         AND (driver_membership.ngay_ket_thuc_hieu_luc IS NULL
                              OR driver_membership.ngay_ket_thuc_hieu_luc >= :membershipDate)
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
                        ST_ClosestPoint(eligible.tuyen_duong_goc, eligible.pickup)
                    ) AS pickup_fraction,

                    ST_Distance(
                        CAST(eligible.tuyen_duong_goc AS geography),
                        CAST(eligible.pickup AS geography)
                    ) AS pickup_deviation_m,

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
                        ST_ClosestPoint(eligible.tuyen_duong_goc, eligible.destination)
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
                        WHEN measured.driver_destination_deviation_m <= :sameDestinationRadiusMeters
                         AND measured.pickup_fraction < 1.0
                            THEN 'CUNG_DIEM_DEN'
                        WHEN measured.destination_route_deviation_m <= :destinationNearRouteRadiusMeters
                         AND measured.pickup_fraction < measured.destination_fraction
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
                            THEN 1.0
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
            """;

    private static final String COUNT_SQL = MATCHING_CTE + """
            SELECT COUNT(*) AS total_elements
              FROM matched
            """;

    private static final String DATA_SQL = MATCHING_CTE + """
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

                CAST(ST_Y(matched.diem_xuat_phat) AS numeric) AS origin_latitude,
                CAST(ST_X(matched.diem_xuat_phat) AS numeric) AS origin_longitude,
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

                ST_AsGeoJSON(matched.tuyen_duong_goc, 6) AS route_geo_json,
                matched.thoi_gian_khoi_hanh_du_kien,
                matched.so_ghe_con_lai,
                matched.muc_ho_tro_goi_y_moi_km,

                CAST(ROUND(CAST(matched.pickup_deviation_m AS numeric), 2) AS numeric)
                    AS pickup_deviation_m,
                CAST(ROUND(CAST(matched.destination_deviation_m AS numeric), 2) AS numeric)
                    AS destination_deviation_m,
                CAST(ROUND(CAST(
                    ST_Length(
                        CAST(
                            ST_LineSubstring(
                                matched.tuyen_duong_goc,
                                matched.pickup_fraction,
                                matched.shared_segment_end_fraction
                            )
                            AS geography
                        )
                    )
                AS numeric), 2) AS numeric) AS shared_segment_m
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

    private static final RowMapper<SharedRouteSearchRow> ROW_MAPPER = PostgisSharedRouteSearchRepository::mapRow;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<SharedRouteSearchContext> findSearchContext(
            Long actorUserId,
            Long schoolId,
            LocalDate membershipDate) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("actorUserId", actorUserId)
                .addValue("schoolId", schoolId)
                .addValue("membershipDate", membershipDate);

        List<SharedRouteSearchContext> contexts = jdbc.query(
                SEARCH_CONTEXT_SQL,
                params,
                (rs, rowNum) -> new SharedRouteSearchContext(
                        rs.getBigDecimal("ban_kinh_cung_diem_den_met"),
                        rs.getBigDecimal("ban_kinh_diem_den_gan_tuyen_met"),
                        rs.getBigDecimal("khoang_cach_lech_don_toi_da_met"),
                        rs.getInt("do_lech_thoi_gian_khoi_hanh_phut")));

        return contexts.stream().findFirst();
    }

    @Override
    public SharedRouteSearchPage search(SharedRouteSearchCriteria criteria) {
        MapSqlParameterSource params = parameters(criteria);

        Number total = jdbc.queryForObject(COUNT_SQL, params, Number.class);
        long totalElements = total == null ? 0L : total.longValue();

        if (totalElements == 0L) {
            return new SharedRouteSearchPage(List.of(), 0L);
        }

        List<SharedRouteSearchRow> rows = jdbc.query(DATA_SQL, params, ROW_MAPPER);
        return new SharedRouteSearchPage(rows, totalElements);
    }

    private static MapSqlParameterSource parameters(
            SharedRouteSearchCriteria criteria) {
        SharedRouteSearchContext context = criteria.context();

        return new MapSqlParameterSource()
                .addValue(
                        "actorUserId",
                        criteria.actorUserId(),
                        Types.BIGINT)
                .addValue(
                        "schoolId",
                        criteria.schoolId(),
                        Types.BIGINT)
                .addValue(
                        "pickupLatitude",
                        criteria.pickupLatitude(),
                        Types.NUMERIC)
                .addValue(
                        "pickupLongitude",
                        criteria.pickupLongitude(),
                        Types.NUMERIC)
                .addValue(
                        "destinationLatitude",
                        criteria.destinationLatitude(),
                        Types.NUMERIC)
                .addValue(
                        "destinationLongitude",
                        criteria.destinationLongitude(),
                        Types.NUMERIC)
                .addValue(
                        "now",
                        toUtcOffsetDateTime(criteria.now()),
                        Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue(
                        "membershipDate",
                        criteria.membershipDate(),
                        Types.DATE)
                .addValue(
                        "departureFrom",
                        toUtcOffsetDateTime(criteria.departureFrom()),
                        Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue(
                        "departureTo",
                        toUtcOffsetDateTime(criteria.departureTo()),
                        Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue(
                        "sameDestinationRadiusMeters",
                        context.sameDestinationRadiusMeters(),
                        Types.NUMERIC)
                .addValue(
                        "destinationNearRouteRadiusMeters",
                        context.destinationNearRouteRadiusMeters(),
                        Types.NUMERIC)
                .addValue(
                        "maxPickupDeviationMeters",
                        context.maxPickupDeviationMeters(),
                        Types.NUMERIC)
                .addValue(
                        "limit",
                        criteria.size(),
                        Types.INTEGER)
                .addValue(
                        "offset",
                        criteria.offset(),
                        Types.BIGINT);
    }

    private static OffsetDateTime toUtcOffsetDateTime(
            java.time.Instant value) {
        return OffsetDateTime.ofInstant(
                value,
                ZoneOffset.UTC);
    }

    private static SharedRouteSearchRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SharedRouteSearchRow(
                rs.getLong("route_id"),
                LoaiGhepTuyen.valueOf(rs.getString("match_type")),
                LoaiDiemTha.valueOf(rs.getString("dropoff_type")),

                rs.getLong("driver_id"),
                rs.getString("driver_name"),
                rs.getString("driver_avatar_url"),

                rs.getLong("vehicle_id"),
                rs.getString("bien_so_xe"),
                rs.getString("mau_sac_thuc_te"),
                rs.getString("ten_hang"),
                rs.getString("ten_dong_xe"),

                rs.getBigDecimal("origin_latitude"),
                rs.getBigDecimal("origin_longitude"),
                rs.getString("dia_chi_xuat_phat"),

                rs.getBigDecimal("driver_destination_latitude"),
                rs.getBigDecimal("driver_destination_longitude"),
                rs.getString("dia_chi_dich_tai_xe"),

                rs.getBigDecimal("pickup_projection_latitude"),
                rs.getBigDecimal("pickup_projection_longitude"),

                rs.getBigDecimal("proposed_dropoff_latitude"),
                rs.getBigDecimal("proposed_dropoff_longitude"),

                rs.getString("route_geo_json"),
                instant(rs, "thoi_gian_khoi_hanh_du_kien"),
                rs.getInt("so_ghe_con_lai"),

                rs.getBigDecimal("muc_ho_tro_goi_y_moi_km"),
                rs.getBigDecimal("pickup_deviation_m"),
                rs.getBigDecimal("destination_deviation_m"),
                rs.getBigDecimal("shared_segment_m"));
    }

    private static java.time.Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
