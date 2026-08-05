package com.zanh.route_sharing.repository.sharedroute.common.postgis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class PostgresJdbcValues {

    private PostgresJdbcValues() {
    }

    public static Instant instant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    public static Long longObject(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, Long.class);
    }

    public static OffsetDateTime utc(Instant value) {
        return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
