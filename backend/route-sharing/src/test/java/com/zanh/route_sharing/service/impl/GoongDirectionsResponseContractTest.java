package com.zanh.route_sharing.service.impl;


import tools.jackson.databind.json.JsonMapper;
import com.zanh.route_sharing.integration.goong.GoongDirectionsResponse;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class GoongDirectionsResponseContractTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void givenMultiStopDirectionFixture_whenDeserializing_thenRoutesLegsAndOverviewPolylineArePreserved()
            throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/goong/directions-multistop-success.json")) {
            assertThat(input).isNotNull();

            GoongDirectionsResponse response = jsonMapper.readValue(input, GoongDirectionsResponse.class);

            assertThat(response.geocodedWaypoints()).hasSize(4);
            assertThat(response.routes()).singleElement().satisfies(route -> {
                assertThat(route.legs()).hasSize(3);
                assertThat(route.legs().get(0).distance().value()).isEqualTo(1200L);
                assertThat(route.legs().get(2).duration().value()).isEqualTo(420L);
                assertThat(route.overviewPolyline().points())
                        .isEqualTo("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
                assertThat(route.bounds().northeast().lat()).isEqualTo(10.7801d);
                assertThat(route.waypointOrder()).containsExactly(0, 1);
            });
        }
    }
}
