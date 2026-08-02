package com.zanh.route_sharing.domain.entity;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.testfixture.SharedRouteMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoTrinhChiaSeTest {

    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    void givenValidData_whenOpeningRoute_thenInitializesRequiredState() {
        // Arrange
        NguoiDung driver = SharedRouteMother.activeUser(1L);
        PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                2L,
                driver,
                2
        );
        Point origin = point(106.66, 10.76, 4326);
        Point destination = point(106.68, 10.78, 4326);
        LineString route = line(origin, destination, 4326);

        // Act
        LoTrinhChiaSe result = LoTrinhChiaSe.open(
                driver,
                vehicle,
                origin,
                "  Điểm A  ",
                destination,
                "  Điểm B  ",
                route,
                new BigDecimal("5000"),
                600,
                SharedRouteMother.NOW.plusSeconds(3600),
                2,
                new BigDecimal("3000")
        );

        // Assert
        assertThat(result.getTrangThaiLoTrinh())
                .isEqualTo(TrangThaiLoTrinh.OPEN);
        assertThat(result.getSoGheCungCap()).isEqualTo(2);
        assertThat(result.getSoGheConLai()).isEqualTo(2);
        assertThat(result.getDiaChiXuatPhat()).isEqualTo("Điểm A");
        assertThat(result.getDiaChiDichTaiXe()).isEqualTo("Điểm B");
        assertThat(result.getTaiXe()).isSameAs(driver);
        assertThat(result.getPhuongTien()).isSameAs(vehicle);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 3})
    void givenInvalidOfferedSeats_whenOpeningRoute_thenRejects(
            int offeredSeats
    ) {
        // Arrange
        NguoiDung driver = SharedRouteMother.activeUser(1L);
        PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                2L,
                driver,
                2
        );
        Point origin = point(106.66, 10.76, 4326);
        Point destination = point(106.68, 10.78, 4326);
        LineString route = line(origin, destination, 4326);

        // Act & Assert
        assertThatThrownBy(() -> LoTrinhChiaSe.open(
                driver,
                vehicle,
                origin,
                "Điểm A",
                destination,
                "Điểm B",
                route,
                new BigDecimal("5000"),
                600,
                SharedRouteMother.NOW.plusSeconds(3600),
                offeredSeats,
                new BigDecimal("3000")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenGeometryWithWrongSrid_whenOpeningRoute_thenRejects() {
        // Arrange
        NguoiDung driver = SharedRouteMother.activeUser(1L);
        PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                2L,
                driver,
                1
        );
        Point originWithWrongSrid = point(106.66, 10.76, 0);
        Point destination = point(106.68, 10.78, 4326);
        LineString route = line(
                point(106.66, 10.76, 4326),
                destination,
                4326
        );

        // Act & Assert
        assertThatThrownBy(() -> LoTrinhChiaSe.open(
                driver,
                vehicle,
                originWithWrongSrid,
                "Điểm A",
                destination,
                "Điểm B",
                route,
                new BigDecimal("5000"),
                600,
                SharedRouteMother.NOW.plusSeconds(3600),
                1,
                new BigDecimal("3000")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SRID 4326");
    }

    @Test
    void givenNegativeSupportAmount_whenOpeningRoute_thenRejects() {
        // Arrange
        NguoiDung driver = SharedRouteMother.activeUser(1L);
        PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                2L,
                driver,
                1
        );
        Point origin = point(106.66, 10.76, 4326);
        Point destination = point(106.68, 10.78, 4326);
        LineString route = line(origin, destination, 4326);

        // Act & Assert
        assertThatThrownBy(() -> LoTrinhChiaSe.open(
                driver,
                vehicle,
                origin,
                "Điểm A",
                destination,
                "Điểm B",
                route,
                new BigDecimal("5000"),
                600,
                SharedRouteMother.NOW.plusSeconds(3600),
                1,
                new BigDecimal("-1")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không được âm");
    }

    private Point point(double x, double y, int srid) {
        Point point = geometryFactory.createPoint(new Coordinate(x, y));
        point.setSRID(srid);
        return point;
    }

    private LineString line(
            Point origin,
            Point destination,
            int srid
    ) {
        LineString lineString = geometryFactory.createLineString(
                new Coordinate[]{
                        origin.getCoordinate(),
                        destination.getCoordinate()
                }
        );
        lineString.setSRID(srid);
        return lineString;
    }
}
