package com.zanh.route_sharing.testsupport.riderequest;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.domain.riderequest.RideRequestPointSnapshot;
import com.zanh.route_sharing.domain.riderequest.RideRequestPolicySnapshot;
import com.zanh.route_sharing.dto.riderequest.RideRequestPointResponse;
import com.zanh.route_sharing.dto.riderequest.RideRequestResponse;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewConsistencyToken;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestGeoPoint;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPersistedView;
import com.zanh.route_sharing.repository.sharedroute.riderequest.model.RideRequestPreparation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public final class RideRequestMother {

    public static final Instant NOW = Instant.parse("2026-08-05T12:00:00Z");
    public static final Instant DEPARTURE = NOW.plusSeconds(7200);
    public static final Long ACTOR_ID = 7L;
    public static final Long ROUTE_ID = 22L;
    public static final Long DRIVER_ID = 9L;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private RideRequestMother() {
    }

    public static RideRequestPolicySnapshot policy() {
        return new RideRequestPolicySnapshot(
                80L,
                0L,
                new BigDecimal("200.00"),
                new BigDecimal("150.00"),
                new BigDecimal("150.00"),
                900L,
                new BigDecimal("60.00"),
                Duration.ofMinutes(15),
                Duration.ofMinutes(15),
                Duration.ofHours(1));
    }

    public static PreviewConsistencyToken token() {
        return new PreviewConsistencyToken(
                ROUTE_ID, 1L, 0L,
                ACTOR_ID, 0L, 0L,
                DRIVER_ID, 0L, 0L,
                30L, 0L,
                40L, 0L,
                50L, 0L,
                60L, 0L,
                70L, 0L,
                71L, 0L,
                0L,
                80L, 0L,
                new BigDecimal("200.00"),
                new BigDecimal("150.00"),
                new BigDecimal("150.00"),
                900L,
                new BigDecimal("60.00"),
                900L,
                900L,
                3600L,
                DEPARTURE,
                2);
    }

    public static RideRequestPreparation segmentPreparation() {
        return new RideRequestPreparation(
                ROUTE_ID,
                0L,
                DRIVER_ID,
                LoaiPhuongTien.XE_MAY,
                DEPARTURE,
                2,
                new BigDecimal("5000.00"),
                LoaiGhepTuyen.TRUNG_DOAN_TUYEN,
                LoaiDiemTha.DIEM_THA_TRUNG_GIAN,
                new RideRequestGeoPoint(
                        new BigDecimal("10.776500"),
                        new BigDecimal("106.700900")),
                new RideRequestGeoPoint(
                        new BigDecimal("10.781800"),
                        new BigDecimal("106.711900")),
                policy(),
                token());
    }

    public static RideRequestPersistedView persistedView() {
        return new RideRequestPersistedView(
                501L,
                ROUTE_ID,
                TrangThaiYeuCau.PENDING,
                NOW,
                NOW.plusSeconds(900),
                LoaiGhepTuyen.TRUNG_DOAN_TUYEN,
                LoaiDiemTha.DIEM_THA_TRUNG_GIAN,
                point("106.700981", "10.776530", "Điểm đón"),
                point("106.712450", "10.782120", "Điểm đến"),
                point("106.711900", "10.781800", "Điểm thả"),
                new BigDecimal("180.50"),
                65L,
                new BigDecimal("4200.00"),
                new BigDecimal("3900.00"),
                new BigDecimal("300.00"),
                new BigDecimal("92.86"),
                new BigDecimal("5000.00"),
                new BigDecimal("25000.00"),
                null);
    }

    public static RideRequestResponse response() {
        return new RideRequestResponse(
                501L,
                ROUTE_ID,
                TrangThaiYeuCau.PENDING,
                NOW,
                NOW.plusSeconds(900),
                false,
                LoaiGhepTuyen.TRUNG_DOAN_TUYEN,
                LoaiDiemTha.DIEM_THA_TRUNG_GIAN,
                new RideRequestPointResponse(
                        new BigDecimal("10.776530"),
                        new BigDecimal("106.700981"),
                        "Điểm đón"),
                new RideRequestPointResponse(
                        new BigDecimal("10.782120"),
                        new BigDecimal("106.712450"),
                        "Điểm đến"),
                new RideRequestPointResponse(
                        new BigDecimal("10.781800"),
                        new BigDecimal("106.711900"),
                        "Điểm thả"),
                new BigDecimal("180.50"),
                65L,
                new BigDecimal("4200.00"),
                new BigDecimal("3900.00"),
                new BigDecimal("300.00"),
                new BigDecimal("92.86"),
                new BigDecimal("5000.00"),
                new BigDecimal("25000.00"),
                null);
    }

    private static RideRequestPointSnapshot point(
            String longitude,
            String latitude,
            String address) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(
                Double.parseDouble(longitude),
                Double.parseDouble(latitude)));
        point.setSRID(4326);
        return new RideRequestPointSnapshot(point, address);
    }
}
