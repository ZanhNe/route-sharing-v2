package com.zanh.route_sharing.integration.goong;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class GoongGeocodingResponseContractTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void givenOfficialShapeFixture_whenDeserializing_thenFormattedAddressIsPreserved()
            throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/goong/geocoding-success.json")) {
            assertThat(input).isNotNull();

            GoongGeocodingResponse response = jsonMapper.readValue(
                    input,
                    GoongGeocodingResponse.class);

            assertThat(response.status()).isEqualTo("OK");
            assertThat(response.results()).singleElement().satisfies(result ->
                    assertThat(result.formattedAddress())
                            .isEqualTo("123 Đường Nguyễn Huệ, Quận 1, Thành phố Hồ Chí Minh"));
        }
    }
}
