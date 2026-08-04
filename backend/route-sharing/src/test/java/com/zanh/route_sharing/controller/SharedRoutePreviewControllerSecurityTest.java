package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.sharedroute.preview.PreviewSharedRouteRequest;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.SharedRoutePreviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static com.zanh.route_sharing.controller.SharedRoutePreviewControllerTest.request;
import static com.zanh.route_sharing.testsupport.sharedroute.CustomUserDetailsMother.activeUser;
import static com.zanh.route_sharing.testsupport.sharedroute.SharedRoutePreviewResponseMother.validPreview;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(SharedRoutePreviewControllerSecurityTest.Config.class)
class SharedRoutePreviewControllerSecurityTest {

    private static final Long ACTOR_ID = 77L;

    @Autowired
    private SharedRoutePreviewController sut;

    @Autowired
    private SharedRoutePreviewService service;

    private PreviewSharedRouteRequest request;

    @BeforeEach
    void setUp() {
        reset(service);
        request = request();
        when(service.preview(ACTOR_ID, 2L, request))
                .thenReturn(validPreview());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenSearchPermission_whenPreviewing_thenRequestIsAllowed() {
        CustomUserDetails principal = activeUser(ACTOR_ID, "SEARCH_SHARED_ROUTE");
        authenticate(principal);

        assertThatCode(() -> sut.preview(principal, 2L, request))
                .doesNotThrowAnyException();
        verify(service).preview(ACTOR_ID, 2L, request);
    }

    @Test
    void givenMissingSearchPermission_whenPreviewing_thenAccessIsDeniedBeforeServiceCall() {
        CustomUserDetails principal = activeUser(ACTOR_ID, "CREATE_SHARED_ROUTE");
        authenticate(principal);

        assertThatThrownBy(() -> sut.preview(principal, 2L, request))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    private static void authenticate(CustomUserDetails principal) {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean
        SharedRoutePreviewService sharedRoutePreviewService() {
            return mock(SharedRoutePreviewService.class);
        }

        @Bean
        SharedRoutePreviewController sharedRoutePreviewController(SharedRoutePreviewService service) {
            return new SharedRoutePreviewController(service);
        }
    }
}
