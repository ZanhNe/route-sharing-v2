package com.zanh.route_sharing.repository.sharedroute.driverquery.postgis;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.repository.sharedroute.common.postgis.PostgresJdbcValues;
import com.zanh.route_sharing.repository.sharedroute.driverquery.DriverSharedRouteQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRouteDetailRow;
import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRoutePageSnapshot;
import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRouteQueryCriteria;
import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRouteSummaryRow;
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
public class PostgisDriverSharedRouteQueryRepository implements DriverSharedRouteQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgisDriverSharedRouteQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DriverSharedRoutePageSnapshot findPage(DriverSharedRouteQueryCriteria criteria) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("actorUserId", criteria.actorUserId())
                .addValue("size", criteria.size())
                .addValue("offset", criteria.offset());

        String countSql;
        String pageSql;
        if (criteria.status() == null) {
            countSql = PostgisDriverSharedRouteQuerySql.COUNT_ALL;
            pageSql = PostgisDriverSharedRouteQuerySql.PAGE_ALL;
        } else {
            parameters.addValue("status", criteria.status().name());
            countSql = PostgisDriverSharedRouteQuerySql.COUNT_BY_STATUS;
            pageSql = PostgisDriverSharedRouteQuerySql.PAGE_BY_STATUS;
        }

        Long total = jdbcTemplate.queryForObject(countSql, parameters, Long.class);
        List<DriverSharedRouteSummaryRow> rows = jdbcTemplate.query(
                pageSql,
                parameters,
                (resultSet, rowNumber) -> mapSummary(resultSet));

        return new DriverSharedRoutePageSnapshot(
                rows,
                total == null ? 0L : total,
                criteria.page(),
                criteria.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DriverSharedRouteDetailRow> findDetail(Long actorUserId, Long routeId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("actorUserId", actorUserId)
                .addValue("routeId", routeId);

        return jdbcTemplate.query(
                PostgisDriverSharedRouteQuerySql.DETAIL,
                parameters,
                (resultSet, rowNumber) -> mapDetail(resultSet))
                .stream()
                .findFirst();
    }

    private static DriverSharedRouteSummaryRow mapSummary(ResultSet rs) throws SQLException {
        return new DriverSharedRouteSummaryRow(
                rs.getLong("route_id"),
                enumValue(TrangThaiLoTrinh.class, rs.getString("route_status")),
                PostgresJdbcValues.instant(rs, "created_at"),
                PostgresJdbcValues.instant(rs, "expected_departure_time"),
                rs.getBigDecimal("origin_latitude"),
                rs.getBigDecimal("origin_longitude"),
                rs.getString("origin_address"),
                rs.getBigDecimal("destination_latitude"),
                rs.getBigDecimal("destination_longitude"),
                rs.getString("destination_address"),
                rs.getInt("offered_seats"),
                rs.getInt("remaining_seats"),
                rs.getLong("vehicle_id"),
                rs.getString("license_plate"),
                rs.getString("actual_color"),
                rs.getString("brand_name"),
                rs.getString("model_name"),
                rs.getLong("total_requests"),
                rs.getLong("pending_requests"),
                rs.getLong("accepted_bookings"),
                rs.getLong("rejected_requests"),
                rs.getLong("cancelled_by_passenger"),
                rs.getLong("cancelled_by_driver"),
                rs.getBoolean("assigned_to_trip"));
    }

    private static DriverSharedRouteDetailRow mapDetail(ResultSet rs) throws SQLException {
        return new DriverSharedRouteDetailRow(
                rs.getLong("route_id"),
                enumValue(TrangThaiLoTrinh.class, rs.getString("route_status")),
                PostgresJdbcValues.instant(rs, "created_at"),
                PostgresJdbcValues.instant(rs, "expected_departure_time"),
                rs.getBigDecimal("origin_latitude"),
                rs.getBigDecimal("origin_longitude"),
                rs.getString("origin_address"),
                rs.getBigDecimal("destination_latitude"),
                rs.getBigDecimal("destination_longitude"),
                rs.getString("destination_address"),
                rs.getInt("offered_seats"),
                rs.getInt("remaining_seats"),
                rs.getBigDecimal("suggested_support_per_km"),
                rs.getString("original_route_geo_json"),
                rs.getBigDecimal("original_distance_meters"),
                rs.getLong("original_duration_seconds"),
                rs.getLong("vehicle_id"),
                rs.getString("license_plate"),
                rs.getString("actual_color"),
                rs.getString("brand_name"),
                rs.getString("model_name"),
                rs.getLong("total_requests"),
                rs.getLong("pending_requests"),
                rs.getLong("accepted_bookings"),
                rs.getLong("rejected_requests"),
                rs.getLong("cancelled_by_passenger"),
                rs.getLong("cancelled_by_driver"),
                rs.getBoolean("assigned_to_trip"),
                PostgresJdbcValues.longObject(rs, "trip_id"),
                PostgresJdbcValues.instant(rs, "cancelled_at"),
                rs.getString("cancellation_reason"));
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}
