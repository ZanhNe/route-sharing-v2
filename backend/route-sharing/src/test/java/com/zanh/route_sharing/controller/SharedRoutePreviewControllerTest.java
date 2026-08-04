package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.sharedroute.preview.PreviewPointRequest;
import com.zanh.route_sharing.dto.sharedroute.preview.PreviewSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.preview.SharedRoutePreviewResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.SharedRoutePreviewService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static com.zanh.route_sharing.testsupport.sharedroute.CustomUserDetailsMother.activeUser;
import static com.zanh.route_sharing.testsupport.sharedroute.SharedRoutePreviewResponseMother.validPreview;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SharedRoutePreviewControllerTest {

    @Test
    void givenAuthenticatedActor_whenPreviewing_thenActorIdAndRouteIdAreForwarded() {
        SharedRoutePreviewService service = mock(SharedRoutePreviewService.class);
        SharedRoutePreviewController sut = new SharedRoutePreviewController(service);
        CustomUserDetails principal = activeUser(9L, "SEARCH_SHARED_ROUTE");
        PreviewSharedRouteRequest request = request();
        SharedRoutePreviewResponse result = validPreview();
        when(service.preview(9L, 12L, request)).thenReturn(result);

        var response = sut.preview(principal, 12L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(result);
        verify(service).preview(9L, 12L, request);
    }

    static PreviewSharedRouteRequest request() {
        return new PreviewSharedRouteRequest(
                1L,
                new PreviewPointRequest(
                        new BigDecimal("10.7701"),
                        new BigDecimal("106.6900"),
                        "Điểm đón"),
                new PreviewPointRequest(
                        new BigDecimal("10.7801"),
                        new BigDecimal("106.7050"),
                        "Điểm đến"));
    }
}
