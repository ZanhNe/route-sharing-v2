package com.zanh.route_sharing.testsupport.sharedroute;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.repository.SharedRouteSearchRow;

import java.math.BigDecimal;
import java.time.Instant;

public final class SharedRouteSearchRowBuilder {

    private Long sharedRouteId = 101L;
    private LoaiGhepTuyen matchType = LoaiGhepTuyen.TRUNG_DOAN_TUYEN;
    private LoaiDiemTha dropoffType = LoaiDiemTha.DIEM_THA_TRUNG_GIAN;

    private BigDecimal proposedDropoffLatitude = new BigDecimal("10.800000");
    private BigDecimal proposedDropoffLongitude = new BigDecimal("106.720000");
    private BigDecimal pickupDeviationMeters = new BigDecimal("25.50");
    private BigDecimal destinationDeviationMeters = new BigDecimal("80.00");
    private BigDecimal sharedSegmentMeters = new BigDecimal("5000.00");

    private SharedRouteSearchRowBuilder() {
    }

    public static SharedRouteSearchRowBuilder aSearchRow() {
        return new SharedRouteSearchRowBuilder();
    }

    public SharedRouteSearchRowBuilder withSharedRouteId(Long sharedRouteId) {
        this.sharedRouteId = sharedRouteId;
        return this;
    }

    public SharedRouteSearchRowBuilder withMatchType(LoaiGhepTuyen matchType) {
        this.matchType = matchType;
        return this;
    }

    public SharedRouteSearchRowBuilder withDropoffType(LoaiDiemTha dropoffType) {
        this.dropoffType = dropoffType;
        return this;
    }

    public SharedRouteSearchRowBuilder withProposedDropoff(String latitude, String longitude) {
        this.proposedDropoffLatitude = new BigDecimal(latitude);
        this.proposedDropoffLongitude = new BigDecimal(longitude);
        return this;
    }

    public SharedRouteSearchRowBuilder withPickupDeviationMeters(String value) {
        this.pickupDeviationMeters = new BigDecimal(value);
        return this;
    }

    public SharedRouteSearchRowBuilder withDestinationDeviationMeters(String value) {
        this.destinationDeviationMeters = new BigDecimal(value);
        return this;
    }

    public SharedRouteSearchRowBuilder withSharedSegmentMeters(String value) {
        this.sharedSegmentMeters = new BigDecimal(value);
        return this;
    }

    public SharedRouteSearchRow build() {
        return new SharedRouteSearchRow(
                sharedRouteId,
                matchType,
                dropoffType,

                201L,
                "Nguyễn Văn Tài Xế",
                "avatar.png",

                301L,
                "59A-12345",
                "Đen",
                "Honda",
                "City",

                new BigDecimal("10.700000"),
                new BigDecimal("106.600000"),
                "Điểm xuất phát",

                new BigDecimal("10.810000"),
                new BigDecimal("106.730000"),
                "Đích tài xế",

                new BigDecimal("10.771000"),
                new BigDecimal("106.691000"),

                proposedDropoffLatitude,
                proposedDropoffLongitude,

                "{\"type\":\"LineString\",\"coordinates\":[]}",
                Instant.parse("2026-08-03T04:00:00Z"),
                2,

                new BigDecimal("3000"),
                pickupDeviationMeters,
                destinationDeviationMeters,
                sharedSegmentMeters);
    }
}
