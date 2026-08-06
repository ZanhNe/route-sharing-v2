package com.zanh.route_sharing.testsupport.riderequest.query;

import com.zanh.route_sharing.domain.enums.GioiTinh;
import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.OwnedRouteSnapshot;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestDetailLookup;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestDetailRow;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestPageSnapshot;
import com.zanh.route_sharing.repository.sharedroute.riderequest.query.model.PendingRideRequestSummaryRow;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class RouteRideRequestQueryMother {

    public static final Long ACTOR_ID = 9L;
    public static final Long ROUTE_ID = 22L;
    public static final Long REQUEST_ID = 501L;
    public static final Instant READ_AT = Instant.parse("2026-08-06T00:10:00Z");
    public static final Instant SENT_AT = Instant.parse("2026-08-06T00:00:00Z");
    public static final Instant EXPIRES_AT = Instant.parse("2026-08-06T00:15:00Z");

    private RouteRideRequestQueryMother() {
    }

    public static OwnedRouteSnapshot route() {
        return new OwnedRouteSnapshot(
                ROUTE_ID,
                TrangThaiLoTrinh.OPEN,
                Instant.parse("2026-08-06T02:00:00Z"),
                2,
                2,
                new BigDecimal("10.770000"),
                new BigDecimal("106.690000"),
                "Điểm xuất phát tài xế",
                new BigDecimal("10.790000"),
                new BigDecimal("106.720000"),
                "Điểm kết thúc tài xế",
                lineString("106.690000", "10.770000", "106.720000", "10.790000"),
                new BigDecimal("10000.00"),
                1200L);
    }

    public static PendingRideRequestSummaryRow summary() {
        return new PendingRideRequestSummaryRow(
                REQUEST_ID,
                TrangThaiYeuCau.PENDING,
                SENT_AT,
                EXPIRES_AT,
                7L,
                "Nguyễn Văn A",
                "https://example.test/avatar.png",
                "Cổng chính",
                "Ký túc xá",
                LoaiGhepTuyen.TRUNG_DOAN_TUYEN,
                LoaiDiemTha.DIEM_THA_TRUNG_GIAN,
                new BigDecimal("25000.00"));
    }

    public static PendingRideRequestPageSnapshot page() {
        return new PendingRideRequestPageSnapshot(route(), List.of(summary()), 1L, 0, 10);
    }

    public static PendingRideRequestDetailLookup detailLookup() {
        return PendingRideRequestDetailLookup.found(route(), detailRow());
    }

    public static PendingRideRequestDetailRow detailRow() {
        return new PendingRideRequestDetailRow(
                REQUEST_ID,
                TrangThaiYeuCau.PENDING,
                SENT_AT,
                EXPIRES_AT,
                "Tôi đứng tại cổng chính",
                7L,
                "Nguyễn Văn A",
                "https://example.test/avatar.png",
                GioiTinh.NAM,
                LocalDate.of(2003, 3, 15),
                new BigDecimal("10.776530"),
                new BigDecimal("106.700981"),
                "Cổng chính",
                new BigDecimal("10.782120"),
                new BigDecimal("106.712450"),
                "Ký túc xá",
                new BigDecimal("10.781800"),
                new BigDecimal("106.711900"),
                "Điểm thả đề xuất",
                LoaiGhepTuyen.TRUNG_DOAN_TUYEN,
                LoaiDiemTha.DIEM_THA_TRUNG_GIAN,
                lineString(
                        "106.700981", "10.776530",
                        "106.711900", "10.781800",
                        "106.712450", "10.782120"),
                lineString(
                        "106.700981", "10.776530",
                        "106.711900", "10.781800"),
                new BigDecimal("180.50"),
                65L,
                new BigDecimal("4200.00"),
                new BigDecimal("3900.00"),
                new BigDecimal("300.00"),
                new BigDecimal("92.86"),
                new BigDecimal("5000.00"),
                new BigDecimal("25000.00"),
                null,
                Instant.parse("2026-08-06T02:00:00Z"));
    }

    public static PendingRideRequestDetailLookup sameDestinationDetailLookup() {
        return PendingRideRequestDetailLookup.found(route(), new PendingRideRequestDetailRow(
                REQUEST_ID,
                TrangThaiYeuCau.PENDING,
                SENT_AT,
                EXPIRES_AT,
                "Tôi đứng tại cổng chính",
                7L,
                "Nguyễn Văn A",
                "https://example.test/avatar.png",
                GioiTinh.NAM,
                LocalDate.of(2003, 3, 15),
                new BigDecimal("10.776530"),
                new BigDecimal("106.700981"),
                "Cổng chính",
                new BigDecimal("10.782120"),
                new BigDecimal("106.712450"),
                "Ký túc xá",
                new BigDecimal("10.782120"),
                new BigDecimal("106.712450"),
                "Ký túc xá",
                LoaiGhepTuyen.CUNG_DIEM_DEN,
                LoaiDiemTha.DIEM_DICH_CUOI_CUNG,
                lineString(
                        "106.700981", "10.776530",
                        "106.712450", "10.782120"),
                lineString(
                        "106.700981", "10.776530",
                        "106.712450", "10.782120"),
                new BigDecimal("180.50"),
                65L,
                new BigDecimal("4200.00"),
                new BigDecimal("4200.00"),
                BigDecimal.ZERO.setScale(2),
                new BigDecimal("100.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("25000.00"),
                null,
                Instant.parse("2026-08-06T02:00:00Z")));
    }

    private static String lineString(String... longitudeLatitude) {
        StringBuilder builder = new StringBuilder("{\"type\":\"LineString\",\"coordinates\":[");
        for (int index = 0; index < longitudeLatitude.length; index += 2) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append('[')
                    .append(longitudeLatitude[index])
                    .append(',')
                    .append(longitudeLatitude[index + 1])
                    .append(']');
        }
        return builder.append("]}").toString();
    }
}
