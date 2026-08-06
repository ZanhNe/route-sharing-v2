package com.zanh.route_sharing.repository.sharedroute.riderequest.query.postgis;

import com.zanh.route_sharing.domain.enums.GioiTinh;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.repository.sharedroute.common.postgis.PostgresJdbcValues;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.RouteRideRequestQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.OwnedRouteSnapshot;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestDetailLookup;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestDetailRow;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestPageSnapshot;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestSummaryRow;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class PostgisRouteRideRequestQueryRepository implements RouteRideRequestQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgisRouteRideRequestQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PendingRideRequestPageSnapshot> findPendingPage(
            Long actorUserId,
            Long routeId,
            int page,
            int size) {
        Optional<OwnedRouteSnapshot> route = findOwnedRoute(actorUserId, routeId);
        if (route.isEmpty()) {
            return Optional.empty();
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("routeId", routeId)
                .addValue("size", size)
                .addValue("offset", Math.multiplyExact((long) page, size));

        Long total = jdbcTemplate.queryForObject(
                PostgisRouteRideRequestQuerySql.COUNT_PENDING,
                parameters,
                Long.class);
        List<PendingRideRequestSummaryRow> rows = jdbcTemplate.query(
                PostgisRouteRideRequestQuerySql.PENDING_PAGE,
                parameters,
                (resultSet, rowNumber) -> mapSummary(resultSet));

        return Optional.of(new PendingRideRequestPageSnapshot(
                route.orElseThrow(),
                rows,
                total == null ? 0L : total,
                page,
                size));
    }

    @Override
    @Transactional(readOnly = true)
    public PendingRideRequestDetailLookup findPendingDetail(
            Long actorUserId,
            Long routeId,
            Long rideRequestId) {
        Optional<OwnedRouteSnapshot> route = findOwnedRoute(actorUserId, routeId);
        if (route.isEmpty()) {
            return PendingRideRequestDetailLookup.routeNotFound();
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("routeId", routeId)
                .addValue("rideRequestId", rideRequestId);
        List<PendingRideRequestDetailRow> requests = jdbcTemplate.query(
                PostgisRouteRideRequestQuerySql.PENDING_DETAIL,
                parameters,
                (resultSet, rowNumber) -> mapDetail(resultSet));

        if (requests.isEmpty()) {
            return PendingRideRequestDetailLookup.requestNotFound(route.orElseThrow());
        }
        return PendingRideRequestDetailLookup.found(route.orElseThrow(), requests.get(0));
    }

    private Optional<OwnedRouteSnapshot> findOwnedRoute(Long actorUserId, Long routeId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("actorUserId", actorUserId)
                .addValue("routeId", routeId);
        List<OwnedRouteSnapshot> routes = jdbcTemplate.query(
                PostgisRouteRideRequestQuerySql.OWNED_ROUTE,
                parameters,
                (resultSet, rowNumber) -> mapRoute(resultSet));
        return routes.stream().findFirst();
    }

    private static OwnedRouteSnapshot mapRoute(ResultSet resultSet) throws SQLException {
        return new OwnedRouteSnapshot(
                resultSet.getLong("route_id"),
                enumValue(TrangThaiLoTrinh.class, resultSet.getString("route_status")),
                PostgresJdbcValues.instant(resultSet, "expected_departure_time"),
                resultSet.getInt("offered_seats"),
                resultSet.getInt("remaining_seats"),
                resultSet.getBigDecimal("origin_latitude"),
                resultSet.getBigDecimal("origin_longitude"),
                resultSet.getString("origin_address"),
                resultSet.getBigDecimal("driver_destination_latitude"),
                resultSet.getBigDecimal("driver_destination_longitude"),
                resultSet.getString("driver_destination_address"),
                resultSet.getString("original_route_geo_json"),
                resultSet.getBigDecimal("original_distance_meters"),
                resultSet.getLong("original_duration_seconds"));
    }

    private static PendingRideRequestSummaryRow mapSummary(ResultSet resultSet) throws SQLException {
        return new PendingRideRequestSummaryRow(
                resultSet.getLong("ride_request_id"),
                enumValue(TrangThaiYeuCau.class, resultSet.getString("request_status")),
                PostgresJdbcValues.instant(resultSet, "sent_at"),
                PostgresJdbcValues.instant(resultSet, "expires_at"),
                resultSet.getLong("passenger_id"),
                resultSet.getString("passenger_full_name"),
                resultSet.getString("passenger_avatar_url"),
                resultSet.getString("pickup_address"),
                resultSet.getString("passenger_destination_address"),
                enumValue(LoaiGhepTuyen.class, resultSet.getString("match_type")),
                enumValue(LoaiDiemTha.class, resultSet.getString("dropoff_type")),
                resultSet.getBigDecimal("proposed_support_amount"));
    }

    private static PendingRideRequestDetailRow mapDetail(ResultSet resultSet) throws SQLException {
        return new PendingRideRequestDetailRow(
                resultSet.getLong("ride_request_id"),
                enumValue(TrangThaiYeuCau.class, resultSet.getString("request_status")),
                PostgresJdbcValues.instant(resultSet, "sent_at"),
                PostgresJdbcValues.instant(resultSet, "expires_at"),
                resultSet.getString("note"),
                resultSet.getLong("passenger_id"),
                resultSet.getString("passenger_full_name"),
                resultSet.getString("passenger_avatar_url"),
                nullableEnum(GioiTinh.class, resultSet.getString("passenger_gender")),
                resultSet.getObject("passenger_date_of_birth", LocalDate.class),
                resultSet.getBigDecimal("pickup_latitude"),
                resultSet.getBigDecimal("pickup_longitude"),
                resultSet.getString("pickup_address"),
                resultSet.getBigDecimal("passenger_destination_latitude"),
                resultSet.getBigDecimal("passenger_destination_longitude"),
                resultSet.getString("passenger_destination_address"),
                resultSet.getBigDecimal("proposed_dropoff_latitude"),
                resultSet.getBigDecimal("proposed_dropoff_longitude"),
                resultSet.getString("proposed_dropoff_address"),
                enumValue(LoaiGhepTuyen.class, resultSet.getString("match_type")),
                enumValue(LoaiDiemTha.class, resultSet.getString("dropoff_type")),
                resultSet.getString("passenger_desired_route_geo_json"),
                resultSet.getString("served_segment_geo_json"),
                resultSet.getBigDecimal("pickup_deviation_meters"),
                resultSet.getLong("pickup_deviation_seconds"),
                resultSet.getBigDecimal("passenger_desired_distance_meters"),
                resultSet.getBigDecimal("served_distance_meters"),
                resultSet.getBigDecimal("remaining_distance_meters"),
                resultSet.getBigDecimal("convenience_ratio_percent"),
                resultSet.getBigDecimal("suggested_support_per_km_at_request"),
                resultSet.getBigDecimal("proposed_support_amount"),
                resultSet.getBigDecimal("agreed_support_amount"),
                PostgresJdbcValues.instant(resultSet, "departure_time_at_request"));
    }

    private static <E extends Enum<E>> E enumValue(Class<E> enumType, String value) {
        return Enum.valueOf(enumType, value);
    }

    private static <E extends Enum<E>> E nullableEnum(Class<E> enumType, String value) {
        return value == null ? null : enumValue(enumType, value);
    }
}
