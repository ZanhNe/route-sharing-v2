package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.PageMeta;
import com.zanh.route_sharing.dto.sharedroute.search.SearchSharedRoutesRequest;
import com.zanh.route_sharing.dto.sharedroute.search.SharedRouteSearchResult;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.SharedRouteSearchService;
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

import java.util.List;

import static com.zanh.route_sharing.testsupport.sharedroute.CustomUserDetailsMother.activeUser;
import static com.zanh.route_sharing.testsupport.sharedroute.SearchSharedRoutesRequestBuilder.aSearchRequest;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(SharedRouteSearchControllerSecurityTest.Config.class)
class SharedRouteSearchControllerSecurityTest {

    private static final Long ACTOR_ID = 77L;

    @Autowired
    private SharedRouteSearchController sut;

    @Autowired
    private SharedRouteSearchService searchService;

    private SearchSharedRoutesRequest request;

    @BeforeEach
    void setUp() {
        reset(searchService);
        request = aSearchRequest().build();
        when(searchService.search(ACTOR_ID, request, 0, 10))
                .thenReturn(new SharedRouteSearchResult(
                        List.of(),
                        PageMeta.of(0, 10, 0L)));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenActorWithSearchPermission_whenSearching_thenRequestIsAllowed() {
        // Arrange
        CustomUserDetails principal = activeUser(
                ACTOR_ID,
                SharedRouteSearchController.SEARCH_PERMISSION);
        authenticate(principal);

        // Act & Assert
        assertThatCode(() -> sut.search(principal, request, 0, 10))
                .doesNotThrowAnyException();
        verify(searchService).search(ACTOR_ID, request, 0, 10);
    }

    @Test
    void givenActorWithoutSearchPermission_whenSearching_thenAccessIsDeniedBeforeServiceCall() {
        // Arrange
        CustomUserDetails principal = activeUser(
                ACTOR_ID,
                "CREATE_SHARED_ROUTE");
        authenticate(principal);

        // Act & Assert
        assertThatThrownBy(() -> sut.search(principal, request, 0, 10))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(searchService);
    }

    private static void authenticate(CustomUserDetails principal) {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {

        @Bean
        SharedRouteSearchService sharedRouteSearchService() {
            return mock(SharedRouteSearchService.class);
        }

        @Bean
        SharedRouteSearchController sharedRouteSearchController(
                SharedRouteSearchService searchService) {
            return new SharedRouteSearchController(searchService);
        }
    }
}
