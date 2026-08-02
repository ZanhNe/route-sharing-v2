package com.zanh.route_sharing;

import com.zanh.route_sharing.integration.goong.GoongApiGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class RouteSharingApplicationContextTest {

    @MockitoBean
    private GoongApiGateway goongApiGateway;

    @Test
    void contextLoads() {

    }
}