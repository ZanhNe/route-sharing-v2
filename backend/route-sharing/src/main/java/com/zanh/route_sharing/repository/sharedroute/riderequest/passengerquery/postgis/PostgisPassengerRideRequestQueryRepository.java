package com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.postgis;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.repository.sharedroute.common.postgis.PostgresJdbcValues;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.PassengerRideRequestQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestDetailRow;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestPageSnapshot;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestQueryCriteria;
import com.zanh.route_sharing.repository.sharedroute.riderequest.passengerquery.model.PassengerRideRequestSummaryRow;
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
public class PostgisPassengerRideRequestQueryRepository implements PassengerRideRequestQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgisPassengerRideRequestQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PassengerRideRequestPageSnapshot findPage(PassengerRideRequestQueryCriteria criteria) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("actorUserId", criteria.actorUserId())
                .addValue("size", criteria.size())
                .addValue("offset", criteria.offset());

        String countSql;
        String pageSql;
        if (criteria.status() == null) {
            countSql = PostgisPassengerRideRequestQuerySql.COUNT_ALL;
            pageSql = PostgisPassengerRideRequestQuerySql.PAGE_ALL;
        } else {
            parameters.addValue("status", criteria.status().name());
            countSql = PostgisPassengerRideRequestQuerySql.COUNT_BY_STATUS;
            pageSql = PostgisPassengerRideRequestQuerySql.PAGE_BY_STATUS;
        }

        Long total = jdbcTemplate.queryForObject(countSql, parameters, Long.class);
        List<PassengerRideRequestSummaryRow> rows = jdbcTemplate.query(
                pageSql,
                parameters,
                (resultSet, rowNumber) -> mapSummary(resultSet));

        return new PassengerRideRequestPageSnapshot(
                rows,
                total == null ? 0L : total,
                criteria.page(),
                criteria.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PassengerRideRequestDetailRow> findDetail(Long actorUserId, Long rideRequestId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("actorUserId", actorUserId)
                .addValue("rideRequestId", rideRequestId);

        return jdbcTemplate.query(
                        PostgisPassengerRideRequestQuerySql.DETAIL,
                        parameters,
                        (resultSet, rowNumber) -> mapDetail(resultSet))
                .stream()
                .findFirst();
    }

    private static PassengerRideRequestSummaryRow mapSummary(ResultSet resultSet) throws SQLException {
        return new PassengerRideRequestSummaryRow(
                resultSet.getLong("ride_request_id"),
                enumValue(TrangThaiYeuCau.class, resultSet.getString("request_status")),
                PostgresJdbcValues.instant(resultSet, "sent_at"),
                resultSet.getLong("route_id"),
                enumValue(TrangThaiLoTrinh.class, resultSet.getString("route_status")),
                resultSet.getString("route_origin_address"),
                resultSet.getString("route_destination_address"),
                PostgresJdbcValues.instant(resultSet, "expected_departure_time"),
                resultSet.getLong("driver_id"),
                resultSet.getString("driver_full_name"),
                resultSet.getString("driver_avatar_url"),
                resultSet.getLong("vehicle_id"),
                resultSet.getString("license_plate"),
                resultSet.getString("actual_color"),
                resultSet.getString("brand_name"),
                resultSet.getString("model_name"),
                enumValue(LoaiGhepTuyen.class, resultSet.getString("match_type")),
                enumValue(LoaiDiemTha.class, resultSet.getString("dropoff_type")),
                resultSet.getString("pickup_address"),
                resultSet.getString("passenger_destination_address"),
                resultSet.getString("proposed_dropoff_address"),
                resultSet.getBigDecimal("proposed_support_amount"),
                resultSet.getBigDecimal("agreed_support_amount"),
                PostgresJdbcValues.instant(resultSet, "accepted_at"),
                PostgresJdbcValues.instant(resultSet, "rejected_at"),
                PostgresJdbcValues.instant(resultSet, "cooldown_until"),
                PostgresJdbcValues.instant(resultSet, "cancelled_at"),
                resultSet.getString("cancellation_reason"),
                resultSet.getBoolean("assigned_to_trip"));
    }

    private static PassengerRideRequestDetailRow mapDetail(ResultSet resultSet) throws SQLException {
        PassengerRideRequestDetailRow.RouteRow route = new PassengerRideRequestDetailRow.RouteRow(
                resultSet.getLong("route_id"),
                enumValue(TrangThaiLoTrinh.class, resultSet.getString("route_status")),
                PostgresJdbcValues.instant(resultSet, "expected_departure_time"),
                resultSet.getInt("offered_seats"),
                resultSet.getInt("remaining_seats"),
                resultSet.getBigDecimal("origin_latitude"),
                resultSet.getBigDecimal("origin_longitude"),
                resultSet.getString("origin_address"),
                resultSet.getBigDecimal("destination_latitude"),
                resultSet.getBigDecimal("destination_longitude"),
                resultSet.getString("destination_address"),
                resultSet.getString("original_route_geo_json"),
                resultSet.getBigDecimal("original_distance_meters"),
                resultSet.getLong("original_duration_seconds"));

        PassengerRideRequestDetailRow.DriverRow driver = new PassengerRideRequestDetailRow.DriverRow(
                resultSet.getLong("driver_id"),
                resultSet.getString("driver_full_name"),
                resultSet.getString("driver_avatar_url"));

        PassengerRideRequestDetailRow.VehicleRow vehicle = new PassengerRideRequestDetailRow.VehicleRow(
                resultSet.getLong("vehicle_id"),
                resultSet.getString("license_plate"),
                resultSet.getString("actual_color"),
                resultSet.getString("brand_name"),
                resultSet.getString("model_name"));

        PassengerRideRequestDetailRow.BookingRow booking = new PassengerRideRequestDetailRow.BookingRow(
                enumValue(LoaiGhepTuyen.class, resultSet.getString("match_type")),
                enumValue(LoaiDiemTha.class, resultSet.getString("dropoff_type")),
                resultSet.getBigDecimal("pickup_latitude"),
                resultSet.getBigDecimal("pickup_longitude"),
                resultSet.getString("pickup_address"),
                resultSet.getBigDecimal("passenger_destination_latitude"),
                resultSet.getBigDecimal("passenger_destination_longitude"),
                resultSet.getString("passenger_destination_address"),
                resultSet.getBigDecimal("proposed_dropoff_latitude"),
                resultSet.getBigDecimal("proposed_dropoff_longitude"),
                resultSet.getString("proposed_dropoff_address"),
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
                PostgresJdbcValues.instant(resultSet, "departure_time_at_request"),
                resultSet.getString("note"));

        return new PassengerRideRequestDetailRow(
                resultSet.getLong("ride_request_id"),
                enumValue(TrangThaiYeuCau.class, resultSet.getString("request_status")),
                PostgresJdbcValues.instant(resultSet, "sent_at"),
                PostgresJdbcValues.instant(resultSet, "accepted_at"),
                PostgresJdbcValues.instant(resultSet, "rejected_at"),
                PostgresJdbcValues.instant(resultSet, "cooldown_until"),
                PostgresJdbcValues.instant(resultSet, "cancelled_at"),
                resultSet.getString("cancellation_reason"),
                resultSet.getBoolean("assigned_to_trip"),
                route,
                driver,
                vehicle,
                booking);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> enumType, String value) {
        return Enum.valueOf(enumType, value);
    }
}
