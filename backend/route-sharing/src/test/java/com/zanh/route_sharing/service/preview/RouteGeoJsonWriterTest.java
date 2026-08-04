package com.zanh.route_sharing.service.preview;

import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteGeoJsonWriterTest {

        private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

        private final RouteGeoJsonWriter sut = new RouteGeoJsonWriter(JsonMapper.builder().build());

        @Test
        void givenStoredLineString_whenReading_thenLongitudeLatitudeOrderIsPreserved() {
                var result = sut.readStoredLineString(
                                "{\"type\":\"LineString\",\"coordinates\":[[106.68,10.77],[106.72,10.78]]}");

                assertThat(result.type()).isEqualTo("LineString");
                assertThat(result.coordinates()).hasSize(2);
                assertThat(result.coordinates().get(0).get(0)).isEqualByComparingTo("106.68");
                assertThat(result.coordinates().get(0).get(1)).isEqualByComparingTo("10.77");
        }

        @Test
        void givenJtsLineString_whenWriting_thenGeoJsonUsesLongitudeLatitudeOrder() {
                LineString line = GEOMETRY_FACTORY.createLineString(new Coordinate[] {
                                new Coordinate(106.68, 10.77),
                                new Coordinate(106.72, 10.78)
                });
                line.setSRID(4326);

                var result = sut.writePreviewLineString(line);

                assertThat(result.coordinates().get(1).get(0)).isEqualByComparingTo("106.72");
                assertThat(result.coordinates().get(1).get(1)).isEqualByComparingTo("10.78");
        }

        @Test
        void givenMalformedStoredGeoJson_whenReading_thenStoredGeometryErrorIsReturned() {
                assertThatThrownBy(() -> sut.readStoredLineString("{not-json}"))
                                .isInstanceOf(BusinessException.class)
                                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                                                .isEqualTo("INVALID_STORED_ROUTE_GEOMETRY"));
        }

        @Test
        void givenOutOfRangeStoredCoordinate_whenReading_thenStoredGeometryErrorIsReturned() {
                assertThatThrownBy(() -> sut.readStoredLineString(
                                "{\"type\":\"LineString\",\"coordinates\":[[206.68,10.77],[106.72,10.78]]}"))
                                .isInstanceOf(BusinessException.class)
                                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                                                .isEqualTo("INVALID_STORED_ROUTE_GEOMETRY"));
        }
}
