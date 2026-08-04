package com.zanh.route_sharing.repository.sharedroute.preview.postgis;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.repository.sharedroute.preview.SharedRoutePreviewRepository;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewConsistencyToken;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewDriverSnapshot;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewEvaluation;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewEvaluationStatus;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewGeoPoint;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewMatch;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewRouteSnapshot;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewVehicleSnapshot;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewCriteria;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewPreparation;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchContext;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class PostgisSharedRoutePreviewRepository implements SharedRoutePreviewRepository {

        private final NamedParameterJdbcTemplate jdbc;

        public PostgisSharedRoutePreviewRepository(NamedParameterJdbcTemplate jdbc) {
                this.jdbc = jdbc;
        }

        @Override
        @Transactional(readOnly = true)
        public PreviewEvaluation evaluate(SharedRoutePreviewCriteria criteria) {
                MapSqlParameterSource baseParameters = baseParameters(criteria);
                Optional<SharedRouteSearchContext> context = findPreviewContext(baseParameters);

                if (context.isEmpty()) {
                        return PreviewEvaluation.ineligible(diagnose(baseParameters));
                }

                MapSqlParameterSource matchingParameters = matchingParameters(
                                criteria,
                                context.orElseThrow());

                List<SharedRoutePreviewPreparation> preparations = jdbc.query(
                                PostgisSharedRoutePreviewSql.PREPARE,
                                matchingParameters,
                                PostgisSharedRoutePreviewRepository::mapPreparation);

                if (!preparations.isEmpty()) {
                        return PreviewEvaluation.eligible(preparations.get(0));
                }

                return PreviewEvaluation.ineligible(diagnose(baseParameters));
        }

        @Override
        @Transactional(readOnly = true)
        public boolean remainsCurrent(
                        PreviewConsistencyToken token,
                        Instant checkedAt) {
                if (token == null || checkedAt == null) {
                        return false;
                }

                MapSqlParameterSource params = new MapSqlParameterSource()
                                .addValue("routeId", token.routeId(), Types.BIGINT)
                                .addValue("schoolId", token.schoolId(), Types.BIGINT)
                                .addValue("routeVersion", token.routeVersion(), Types.BIGINT)
                                .addValue("actorUserId", token.actorUserId(), Types.BIGINT)
                                .addValue("actorUserVersion", token.actorUserVersion(), Types.BIGINT)
                                .addValue("actorSecurityVersion", token.actorSecurityVersion(), Types.BIGINT)
                                .addValue("driverId", token.driverId(), Types.BIGINT)
                                .addValue("driverUserVersion", token.driverUserVersion(), Types.BIGINT)
                                .addValue("driverSecurityVersion", token.driverSecurityVersion(), Types.BIGINT)
                                .addValue("driverProfileId", token.driverProfileId(), Types.BIGINT)
                                .addValue("driverProfileVersion", token.driverProfileVersion(), Types.BIGINT)
                                .addValue("vehicleId", token.vehicleId(), Types.BIGINT)
                                .addValue("vehicleVersion", token.vehicleVersion(), Types.BIGINT)
                                .addValue("modelId", token.modelId(), Types.BIGINT)
                                .addValue("modelVersion", token.modelVersion(), Types.BIGINT)
                                .addValue("brandId", token.brandId(), Types.BIGINT)
                                .addValue("brandVersion", token.brandVersion(), Types.BIGINT)
                                .addValue("actorMembershipId", token.actorMembershipId(), Types.BIGINT)
                                .addValue("actorMembershipVersion", token.actorMembershipVersion(), Types.BIGINT)
                                .addValue("driverMembershipId", token.driverMembershipId(), Types.BIGINT)
                                .addValue("driverMembershipVersion", token.driverMembershipVersion(), Types.BIGINT)
                                .addValue("schoolVersion", token.schoolVersion(), Types.BIGINT)
                                .addValue("businessConfigId", token.businessConfigId(), Types.BIGINT)
                                .addValue("businessConfigVersion", token.businessConfigVersion(), Types.BIGINT)
                                .addValue("sameDestinationRadiusMeters",
                                                token.sameDestinationRadiusMeters(), Types.NUMERIC)
                                .addValue("destinationNearRouteRadiusMeters",
                                                token.destinationNearRouteRadiusMeters(), Types.NUMERIC)
                                .addValue("maxPickupDeviationMeters",
                                                token.maxPickupDeviationMeters(), Types.NUMERIC)
                                .addValue("expectedDepartureTime",
                                                toUtcOffsetDateTime(token.expectedDepartureTime()),
                                                Types.TIMESTAMP_WITH_TIMEZONE)
                                .addValue("remainingSeats", token.remainingSeats(), Types.INTEGER)
                                .addValue("checkedAt", toUtcOffsetDateTime(checkedAt), Types.TIMESTAMP_WITH_TIMEZONE);

                Boolean current = jdbc.queryForObject(
                                PostgisSharedRoutePreviewSql.REMAINS_CURRENT,
                                params,
                                Boolean.class);
                return Boolean.TRUE.equals(current);
        }

        private Optional<SharedRouteSearchContext> findPreviewContext(
                        MapSqlParameterSource parameters) {
                List<SharedRouteSearchContext> contexts = jdbc.query(
                                PostgisSharedRoutePreviewSql.PREVIEW_CONTEXT,
                                parameters,
                                (rs, rowNum) -> new SharedRouteSearchContext(
                                                rs.getBigDecimal("ban_kinh_cung_diem_den_met"),
                                                rs.getBigDecimal("ban_kinh_diem_den_gan_tuyen_met"),
                                                rs.getBigDecimal("khoang_cach_lech_don_toi_da_met"),
                                                rs.getInt("do_lech_thoi_gian_khoi_hanh_phut")));
                return contexts.stream().findFirst();
        }

        private PreviewEvaluationStatus diagnose(MapSqlParameterSource parameters) {
                List<String> statuses = jdbc.query(
                                PostgisSharedRoutePreviewSql.DIAGNOSE,
                                parameters,
                                (rs, rowNum) -> rs.getString("evaluation_status"));

                if (statuses.isEmpty() || statuses.get(0) == null) {
                        return PreviewEvaluationStatus.NOT_FOUND_OR_INACCESSIBLE;
                }

                try {
                        return PreviewEvaluationStatus.valueOf(statuses.get(0));
                } catch (IllegalArgumentException exception) {
                        return PreviewEvaluationStatus.NOT_FOUND_OR_INACCESSIBLE;
                }
        }

        private static MapSqlParameterSource baseParameters(
                        SharedRoutePreviewCriteria criteria) {
                return new MapSqlParameterSource()
                                .addValue("actorUserId", criteria.actorUserId(), Types.BIGINT)
                                .addValue("schoolId", criteria.schoolId(), Types.BIGINT)
                                .addValue("sharedRouteId", criteria.sharedRouteId(), Types.BIGINT)
                                .addValue("now", toUtcOffsetDateTime(criteria.now()), Types.TIMESTAMP_WITH_TIMEZONE);
        }

        private static MapSqlParameterSource matchingParameters(
                        SharedRoutePreviewCriteria criteria,
                        SharedRouteSearchContext context) {
                return baseParameters(criteria)
                                .addValue("pickupLatitude", criteria.pickupLatitude(), Types.NUMERIC)
                                .addValue("pickupLongitude", criteria.pickupLongitude(), Types.NUMERIC)
                                .addValue("destinationLatitude", criteria.destinationLatitude(), Types.NUMERIC)
                                .addValue("destinationLongitude", criteria.destinationLongitude(), Types.NUMERIC)
                                .addValue("sameDestinationRadiusMeters",
                                                context.sameDestinationRadiusMeters(),
                                                Types.NUMERIC)
                                .addValue("destinationNearRouteRadiusMeters",
                                                context.destinationNearRouteRadiusMeters(),
                                                Types.NUMERIC)
                                .addValue("maxPickupDeviationMeters",
                                                context.maxPickupDeviationMeters(),
                                                Types.NUMERIC);
        }

        private static SharedRoutePreviewPreparation mapPreparation(
                        ResultSet rs,
                        int rowNum) throws SQLException {
                PreviewRouteSnapshot route = new PreviewRouteSnapshot(
                                rs.getLong("route_id"),
                                longObject(rs, "route_version"),
                                TrangThaiLoTrinh.valueOf(rs.getString("trang_thai_lo_trinh")),
                                new PreviewGeoPoint(
                                                rs.getBigDecimal("origin_latitude"),
                                                rs.getBigDecimal("origin_longitude"),
                                                rs.getString("dia_chi_xuat_phat")),
                                new PreviewGeoPoint(
                                                rs.getBigDecimal("driver_destination_latitude"),
                                                rs.getBigDecimal("driver_destination_longitude"),
                                                rs.getString("dia_chi_dich_tai_xe")),
                                rs.getString("original_route_geo_json"),
                                rs.getBigDecimal("original_distance_m"),
                                rs.getLong("original_duration_s"),
                                instant(rs, "thoi_gian_khoi_hanh_du_kien"),
                                rs.getInt("so_ghe_con_lai"),
                                rs.getBigDecimal("muc_ho_tro_goi_y_moi_km"));

                PreviewDriverSnapshot driver = new PreviewDriverSnapshot(
                                rs.getLong("driver_id"),
                                rs.getString("driver_name"),
                                rs.getString("driver_avatar_url"));

                PreviewVehicleSnapshot vehicle = new PreviewVehicleSnapshot(
                                rs.getLong("vehicle_id"),
                                rs.getString("bien_so_xe"),
                                rs.getString("mau_sac_thuc_te"),
                                rs.getString("ten_hang"),
                                rs.getString("ten_dong_xe"),
                                LoaiPhuongTien.valueOf(rs.getString("loai_phuong_tien")));

                PreviewMatch match = new PreviewMatch(
                                LoaiGhepTuyen.valueOf(rs.getString("match_type")),
                                LoaiDiemTha.valueOf(rs.getString("dropoff_type")),
                                new PreviewGeoPoint(
                                                rs.getBigDecimal("proposed_dropoff_latitude"),
                                                rs.getBigDecimal("proposed_dropoff_longitude"),
                                                null),
                                rs.getBigDecimal("pickup_deviation_m"),
                                rs.getBigDecimal("destination_deviation_m"),
                                rs.getBigDecimal("shared_segment_m"));

                PreviewConsistencyToken token = new PreviewConsistencyToken(
                                route.routeId(),
                                rs.getLong("school_id"),
                                route.routeVersion(),
                                rs.getLong("actor_user_id"),
                                longObject(rs, "actor_user_version"),
                                longObject(rs, "actor_security_version"),
                                driver.id(),
                                longObject(rs, "driver_user_version"),
                                longObject(rs, "driver_security_version"),
                                rs.getLong("driver_profile_id"),
                                longObject(rs, "driver_profile_version"),
                                vehicle.id(),
                                longObject(rs, "vehicle_version"),
                                rs.getLong("model_id"),
                                longObject(rs, "model_version"),
                                rs.getLong("brand_id"),
                                longObject(rs, "brand_version"),
                                rs.getLong("actor_membership_id"),
                                longObject(rs, "actor_membership_version"),
                                rs.getLong("driver_membership_id"),
                                longObject(rs, "driver_membership_version"),
                                longObject(rs, "school_version"),
                                rs.getLong("business_config_id"),
                                longObject(rs, "business_config_version"),
                                rs.getBigDecimal("used_same_destination_radius_m"),
                                rs.getBigDecimal("used_destination_near_route_radius_m"),
                                rs.getBigDecimal("used_max_pickup_deviation_m"),
                                route.expectedDepartureTime(),
                                route.remainingSeats());

                return new SharedRoutePreviewPreparation(route, driver, vehicle, match, token);
        }

        private static Long longObject(ResultSet rs, String column) throws SQLException {
                return rs.getObject(column, Long.class);
        }

        private static Instant instant(ResultSet rs, String column) throws SQLException {
                OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
                return value == null ? null : value.toInstant();
        }

        private static OffsetDateTime toUtcOffsetDateTime(Instant value) {
                return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
        }
}
