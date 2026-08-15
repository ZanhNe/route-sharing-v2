package com.zanh.route_sharing.repository.sharedroute.triplocation.postgis;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.repository.sharedroute.triplocation.TripLocationObserverAccessRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public class PostgisTripLocationObserverAccessRepository implements TripLocationObserverAccessRepository {

    private static final String ELIGIBLE_PASSENGERS = """
            SELECT DISTINCT request.hanh_khach_id
            FROM yeu_cau_di_chung request
            JOIN chuyen_di trip ON trip.id = request.chuyen_di_id
            WHERE trip.id = :tripId
              AND trip.bat_dau_luc IS NOT NULL
              AND trip.ket_thuc_luc IS NULL
              AND trip.trang_thai_van_hanh IN ('IN_PROGRESS', 'SECURITY_FROZEN')
              AND request.trang_thai_yeu_cau IN (:activeStates)
            ORDER BY request.hanh_khach_id ASC
            """;

    private static final String ELIGIBLE_PASSENGER = """
            SELECT COUNT(*)
            FROM yeu_cau_di_chung request
            JOIN chuyen_di trip ON trip.id = request.chuyen_di_id
            WHERE trip.id = :tripId
              AND request.hanh_khach_id = :actorUserId
              AND trip.bat_dau_luc IS NOT NULL
              AND trip.ket_thuc_luc IS NULL
              AND trip.trang_thai_van_hanh IN ('IN_PROGRESS', 'SECURITY_FROZEN')
              AND request.trang_thai_yeu_cau IN (:activeStates)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgisTripLocationObserverAccessRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findEligiblePassengerUserIds(Long tripId, Set<TrangThaiYeuCau> activeStates) {
        requireTripId(tripId);
        MapSqlParameterSource params = params(tripId, activeStates);
        return jdbcTemplate.query(ELIGIBLE_PASSENGERS, params, (rs, rowNum) -> rs.getLong(1));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEligiblePassenger(Long actorUserId, Long tripId, Set<TrangThaiYeuCau> activeStates) {
        if (actorUserId == null || actorUserId <= 0) {
            return false;
        }
        requireTripId(tripId);
        MapSqlParameterSource params = params(tripId, activeStates).addValue("actorUserId", actorUserId);
        Long count = jdbcTemplate.queryForObject(ELIGIBLE_PASSENGER, params, Long.class);
        return count != null && count > 0;
    }

    private static MapSqlParameterSource params(Long tripId, Set<TrangThaiYeuCau> activeStates) {
        requireTripId(tripId);
        if (activeStates == null || activeStates.isEmpty()) {
            throw new IllegalArgumentException("activeStates không được trống.");
        }
        return new MapSqlParameterSource()
                .addValue("tripId", tripId)
                .addValue("activeStates", activeStates.stream().map(Enum::name).toList());
    }

    private static void requireTripId(Long tripId) {
        if (tripId == null || tripId <= 0) {
            throw new IllegalArgumentException("tripId phải là số dương.");
        }
    }
}
