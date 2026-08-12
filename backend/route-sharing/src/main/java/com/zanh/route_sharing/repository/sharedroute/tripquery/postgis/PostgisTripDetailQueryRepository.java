package com.zanh.route_sharing.repository.sharedroute.tripquery.postgis;

import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiDiemDung;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiVanHanhChuyenDi;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.repository.sharedroute.common.postgis.PostgresJdbcValues;
import com.zanh.route_sharing.repository.sharedroute.tripquery.TripDetailQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripDetailHeaderRow;
import com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripDetailParticipantRow;
import com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripDetailSnapshot;
import com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripDetailStopRow;
import com.zanh.route_sharing.repository.sharedroute.tripquery.model.TripViewerRole;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class PostgisTripDetailQueryRepository implements TripDetailQueryRepository {

        private final NamedParameterJdbcTemplate jdbcTemplate;

        public PostgisTripDetailQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
        public Optional<TripDetailSnapshot> findDetail(Long actorUserId, Long tripId) {
                MapSqlParameterSource params = new MapSqlParameterSource()
                                .addValue("actorUserId", actorUserId)
                                .addValue("tripId", tripId);

                Optional<TripDetailHeaderRow> header = jdbcTemplate.query(
                                PostgisTripDetailQuerySql.HEADER,
                                params,
                                (rs, rowNum) -> mapHeader(rs))
                                .stream()
                                .findFirst();
                if (header.isEmpty()) {
                        return Optional.empty();
                }

                boolean driverView = header.orElseThrow().viewerRole() == TripViewerRole.DRIVER;
                List<TripDetailParticipantRow> participants = jdbcTemplate.query(
                                driverView
                                                ? PostgisTripDetailQuerySql.PARTICIPANTS_DRIVER
                                                : PostgisTripDetailQuerySql.PARTICIPANTS_PASSENGER,
                                params,
                                (rs, rowNum) -> mapParticipant(rs));
                List<TripDetailStopRow> stops = jdbcTemplate.query(
                                driverView
                                                ? PostgisTripDetailQuerySql.STOPS_DRIVER
                                                : PostgisTripDetailQuerySql.STOPS_PASSENGER,
                                params,
                                (rs, rowNum) -> mapStop(rs));
                return Optional.of(new TripDetailSnapshot(header.orElseThrow(), participants, stops));
        }

        private static TripDetailHeaderRow mapHeader(ResultSet rs) throws SQLException {
                return new TripDetailHeaderRow(
                                TripViewerRole.valueOf(rs.getString("viewer_role")),
                                rs.getLong("trip_id"),
                                enumValue(TrangThaiVanHanhChuyenDi.class, rs.getString("trip_status")),
                                PostgresJdbcValues.instant(rs, "formed_at"),
                                PostgresJdbcValues.instant(rs, "started_at"),
                                PostgresJdbcValues.instant(rs, "cancelled_at"),
                                rs.getString("cancellation_reason"),
                                rs.getInt("planned_passenger_count"),
                                rs.getInt("actual_passenger_count"),
                                enumValue(TrangThaiDiemDung.class, rs.getString("driver_start_status")),
                                PostgresJdbcValues.instant(rs, "driver_start_completed_at"),
                                rs.getString("operational_route_geo_json"),
                                rs.getLong("route_id"),
                                enumValue(TrangThaiLoTrinh.class, rs.getString("route_status")),
                                PostgresJdbcValues.instant(rs, "locked_at"),
                                PostgresJdbcValues.instant(rs, "expected_departure_time"),
                                rs.getInt("offered_seats"),
                                rs.getInt("remaining_seats"),
                                rs.getBigDecimal("origin_latitude"),
                                rs.getBigDecimal("origin_longitude"),
                                rs.getString("origin_address"),
                                rs.getBigDecimal("destination_latitude"),
                                rs.getBigDecimal("destination_longitude"),
                                rs.getString("destination_address"),
                                rs.getLong("driver_id"),
                                rs.getString("driver_full_name"),
                                rs.getString("driver_avatar_url"),
                                rs.getLong("vehicle_id"),
                                rs.getString("license_plate"),
                                rs.getString("actual_color"),
                                rs.getString("brand_name"),
                                rs.getString("model_name"));
        }

        private static TripDetailParticipantRow mapParticipant(ResultSet rs) throws SQLException {
                return new TripDetailParticipantRow(
                                rs.getLong("ride_request_id"),
                                rs.getLong("passenger_id"),
                                rs.getString("passenger_full_name"),
                                rs.getString("passenger_avatar_url"),
                                enumValue(TrangThaiYeuCau.class, rs.getString("request_status")),
                                PostgresJdbcValues.instant(rs, "accepted_at"),
                                PostgresJdbcValues.instant(rs, "boarded_at"),
                                PostgresJdbcValues.instant(rs, "no_show_at"),
                                enumValue(LoaiGhepTuyen.class, rs.getString("match_type")),
                                enumValue(LoaiDiemTha.class, rs.getString("dropoff_type")),
                                rs.getBigDecimal("agreed_support_amount"),
                                rs.getString("note"),
                                rs.getLong("pickup_stop_id"),
                                rs.getInt("pickup_stop_order"),
                                rs.getLong("dropoff_stop_id"));
        }

        private static TripDetailStopRow mapStop(ResultSet rs) throws SQLException {
                return new TripDetailStopRow(
                                rs.getLong("stop_id"),
                                rs.getInt("stop_order"),
                                enumValue(LoaiDiemDung.class, rs.getString("stop_type")),
                                enumValue(TrangThaiDiemDung.class, rs.getString("stop_status")),
                                PostgresJdbcValues.longObject(rs, "ride_request_id"),
                                rs.getBigDecimal("stop_latitude"),
                                rs.getBigDecimal("stop_longitude"),
                                rs.getString("stop_address"),
                                PostgresJdbcValues.instant(rs, "arrived_at"),
                                PostgresJdbcValues.instant(rs, "waiting_started_at"),
                                PostgresJdbcValues.instant(rs, "waiting_deadline"),
                                PostgresJdbcValues.instant(rs, "completed_at"));
        }

        private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
                return value == null ? null : Enum.valueOf(type, value);
        }
}
