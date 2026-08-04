package com.zanh.route_sharing.repository.sharedroute.search.postgis;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.repository.sharedroute.search.SharedRouteSearchRepository;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchContext;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchCriteria;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchPage;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchRow;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import static com.zanh.route_sharing.repository.sharedroute.search.postgis.PostgisSharedRouteSearchSql.COUNT;
import static com.zanh.route_sharing.repository.sharedroute.search.postgis.PostgisSharedRouteSearchSql.DATA;
import static com.zanh.route_sharing.repository.sharedroute.search.postgis.PostgisSharedRouteSearchSql.SEARCH_CONTEXT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostgisSharedRouteSearchRepository implements SharedRouteSearchRepository {

        private static final RowMapper<SharedRouteSearchRow> ROW_MAPPER = PostgisSharedRouteSearchRepository::mapRow;

        private final NamedParameterJdbcTemplate jdbc;

        @Override
        public Optional<SharedRouteSearchContext> findSearchContext(
                        Long actorUserId,
                        Long schoolId,
                        LocalDate requestedTravelDate) {

                MapSqlParameterSource params = new MapSqlParameterSource()
                                .addValue("actorUserId", actorUserId, Types.BIGINT)
                                .addValue("schoolId", schoolId, Types.BIGINT)
                                .addValue("requestedTravelDate", requestedTravelDate, Types.DATE);

                List<SharedRouteSearchContext> contexts = jdbc.query(
                                SEARCH_CONTEXT,
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

                Number total = jdbc.queryForObject(COUNT, params, Number.class);
                long totalElements = total == null ? 0L : total.longValue();

                if (totalElements == 0L) {
                        return new SharedRouteSearchPage(List.of(), 0L);
                }

                List<SharedRouteSearchRow> rows = jdbc.query(DATA, params, ROW_MAPPER);
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

        private static OffsetDateTime toUtcOffsetDateTime(Instant value) {
                return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
        }

        private static SharedRouteSearchRow mapRow(
                        ResultSet rs,
                        int rowNum) throws SQLException {
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

        private static Instant instant(
                        ResultSet rs,
                        String column) throws SQLException {
                OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
                return value == null ? null : value.toInstant();
        }
}
