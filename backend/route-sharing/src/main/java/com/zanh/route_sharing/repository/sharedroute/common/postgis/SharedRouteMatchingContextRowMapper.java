package com.zanh.route_sharing.repository.sharedroute.common.postgis;

import com.zanh.route_sharing.repository.sharedroute.common.model.SharedRouteMatchingContext;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class SharedRouteMatchingContextRowMapper implements RowMapper<SharedRouteMatchingContext> {

    public static final SharedRouteMatchingContextRowMapper INSTANCE = new SharedRouteMatchingContextRowMapper();

    private SharedRouteMatchingContextRowMapper() {
    }

    @Override
    public SharedRouteMatchingContext mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new SharedRouteMatchingContext(
                resultSet.getBigDecimal("ban_kinh_cung_diem_den_met"),
                resultSet.getBigDecimal("ban_kinh_diem_den_gan_tuyen_met"),
                resultSet.getBigDecimal("khoang_cach_lech_don_toi_da_met"),
                resultSet.getInt("do_lech_thoi_gian_khoi_hanh_phut"));
    }
}
