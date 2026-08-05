package com.zanh.route_sharing.integration.goong;

import com.zanh.route_sharing.config.properties.GoongProperties;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.service.riderequest.model.LocationLabel;
import com.zanh.route_sharing.service.routing.model.GeoCoordinate;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoongLocationLabelResolverTest {

    @Test
    void givenValidProviderResponse_whenResolving_thenFirstNonBlankAddressIsReturned() {
        RecordingGateway gateway = new RecordingGateway(
                new GoongGeocodingResponse(
                        "OK",
                        List.of(
                                new GoongGeocodingResponse.ResultDto("   "),
                                new GoongGeocodingResponse.ResultDto("  Địa chỉ điểm thả  "))));
        GoongLocationLabelResolver sut = new GoongLocationLabelResolver(
                gateway,
                new GoongProperties());

        LocationLabel result = sut.resolve(new GeoCoordinate(
                new BigDecimal("10.7818"),
                new BigDecimal("106.7119")));

        assertThat(result.formattedAddress()).isEqualTo("Địa chỉ điểm thả");
        assertThat(gateway.path).isEqualTo("/Geocode");
        assertThat(gateway.query.getFirst("latlng"))
                .isEqualTo("10.7818,106.7119");
        assertThat(gateway.responseType).isEqualTo(GoongGeocodingResponse.class);
    }

    @Test
    void givenOkWithoutUsableAddress_whenResolving_thenInvalidProviderResponseIsReturned() {
        RecordingGateway gateway = new RecordingGateway(
                new GoongGeocodingResponse("OK", List.of()));
        GoongLocationLabelResolver sut = new GoongLocationLabelResolver(
                gateway,
                new GoongProperties());

        assertThatThrownBy(() -> sut.resolve(new GeoCoordinate(
                new BigDecimal("10.7818"),
                new BigDecimal("106.7119"))))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(502);
                    assertThat(exception.getCode()).isEqualTo("MAP_PROVIDER_INVALID_RESPONSE");
                });
    }

    private static final class RecordingGateway implements GoongApiGateway {
        private final Object response;
        private String path;
        private MultiValueMap<String, String> query;
        private Class<?> responseType;

        private RecordingGateway(Object response) {
            this.response = response;
        }

        @Override
        public <T> T get(
                String relativePath,
                MultiValueMap<String, String> queryParameters,
                Class<T> requestedType) {
            path = relativePath;
            query = queryParameters;
            responseType = requestedType;
            return requestedType.cast(response);
        }
    }
}
