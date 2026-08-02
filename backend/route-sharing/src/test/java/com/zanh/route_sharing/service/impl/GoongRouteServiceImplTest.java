package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.integration.goong.GoongApiGateway;
import com.zanh.route_sharing.integration.goong.GoongDirectionsResponse;
import com.zanh.route_sharing.integration.goong.RouteCoordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoongRouteServiceImplTest {

        @Mock
        private GoongApiGateway goongApiGateway;

        private GoongRouteServiceImpl sut;

        @BeforeEach
        void setUp() {
                sut = new GoongRouteServiceImpl(goongApiGateway);
        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @CsvSource({
                        "XE_MAY, bike",
                        "O_TO, car"
        })
        void givenVehicleType_whenCalculatingRoute_thenSendsCorrectGoongMode(
                        String vehicleTypeText,
                        String expectedMode) {
                // Arrange
                ArgumentCaptor<MultiValueMap<String, String>> queryCaptor = ArgumentCaptor
                                .forClass(MultiValueMap.class);

                when(goongApiGateway.get(
                                eq("/v2/direction"),
                                queryCaptor.capture(),
                                eq(GoongDirectionsResponse.class))).thenReturn(validResponse());

                // Act
                sut.calculate(
                                coordinate("10.762622", "106.660172"),
                                coordinate("10.823099", "106.629664"),
                                LoaiPhuongTien.valueOf(vehicleTypeText));

                // Assert
                MultiValueMap<String, String> query = queryCaptor.getValue();
                assertThat(query.getFirst("origin"))
                                .isEqualTo("10.762622,106.660172");
                assertThat(query.getFirst("destination"))
                                .isEqualTo("10.823099,106.629664");
                assertThat(query.getFirst("vehicle"))
                                .isEqualTo(expectedMode);
                assertThat(query.getFirst("alternatives"))
                                .isEqualTo("false");
        }

        @Test
        void givenRouteWithMultipleLegs_whenCalculatingRoute_thenSumsDistanceAndDuration() {
                // Arrange
                GoongDirectionsResponse response = new GoongDirectionsResponse(List.of(
                                new GoongDirectionsResponse.RouteDto(
                                                List.of(
                                                                leg(100L, 60L),
                                                                leg(250L, 120L)),
                                                new GoongDirectionsResponse.OverviewPolylineDto(
                                                                "_p~iF~ps|U_ulLnnqC_mqNvxq`@"))));

                when(goongApiGateway.get(
                                eq("/v2/direction"),
                                any(),
                                eq(GoongDirectionsResponse.class))).thenReturn(response);

                // Act
                var result = sut.calculate(
                                coordinate("10.0", "106.0"),
                                coordinate("10.1", "106.1"),
                                LoaiPhuongTien.O_TO);

                // Assert
                assertThat(result.distanceMeters())
                                .isEqualByComparingTo("350");
                assertThat(result.durationSeconds()).isEqualTo(180L);
                assertThat(result.path()).hasSize(3);
        }

        @Test
        void givenNoRouteFromProvider_whenCalculatingRoute_thenReturnsRouteNotFound() {
                // Arrange
                when(goongApiGateway.get(
                                eq("/v2/direction"),
                                any(),
                                eq(GoongDirectionsResponse.class))).thenReturn(new GoongDirectionsResponse(List.of()));

                // Act
                BusinessException exception = catchThrowableOfType(
                                () -> sut.calculate(
                                                coordinate("10.0", "106.0"),
                                                coordinate("10.1", "106.1"),
                                                LoaiPhuongTien.XE_MAY),
                                BusinessException.class);

                // Assert
                assertThat(exception).isNotNull();
                assertThat(exception.getStatus())
                                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                assertThat(exception.getCode()).isEqualTo("ROUTE_NOT_FOUND");
        }

        @Test
        void givenInvalidPolyline_whenCalculatingRoute_thenReturnsInvalidProviderResponse() {
                // Arrange
                GoongDirectionsResponse response = new GoongDirectionsResponse(List.of(
                                new GoongDirectionsResponse.RouteDto(
                                                List.of(leg(100L, 60L)),
                                                new GoongDirectionsResponse.OverviewPolylineDto("_"))));

                when(goongApiGateway.get(
                                eq("/v2/direction"),
                                any(),
                                eq(GoongDirectionsResponse.class))).thenReturn(response);

                // Act
                BusinessException exception = catchThrowableOfType(
                                () -> sut.calculate(
                                                coordinate("10.0", "106.0"),
                                                coordinate("10.1", "106.1"),
                                                LoaiPhuongTien.XE_MAY),
                                BusinessException.class);

                // Assert
                assertThat(exception).isNotNull();
                assertThat(exception.getStatus())
                                .isEqualTo(HttpStatus.BAD_GATEWAY);
                assertThat(exception.getCode())
                                .isEqualTo("MAP_PROVIDER_INVALID_RESPONSE");
        }

        private static GoongDirectionsResponse validResponse() {
                return new GoongDirectionsResponse(List.of(
                                new GoongDirectionsResponse.RouteDto(
                                                List.of(leg(100L, 60L)),
                                                new GoongDirectionsResponse.OverviewPolylineDto(
                                                                "_p~iF~ps|U_ulLnnqC_mqNvxq`@"))));
        }

        private static GoongDirectionsResponse.LegDto leg(
                        long distance,
                        long duration) {
                return new GoongDirectionsResponse.LegDto(
                                new GoongDirectionsResponse.ValueDto(distance),
                                new GoongDirectionsResponse.ValueDto(duration));
        }

        private static RouteCoordinate coordinate(
                        String latitude,
                        String longitude) {
                return new RouteCoordinate(
                                new BigDecimal(latitude),
                                new BigDecimal(longitude));
        }
}
