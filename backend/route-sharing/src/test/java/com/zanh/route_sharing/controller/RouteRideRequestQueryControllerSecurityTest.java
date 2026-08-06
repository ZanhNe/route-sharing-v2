package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.service.RouteRideRequestQueryService;
import com.zanh.route_sharing.testsupport.riderequest.query.RouteRideRequestQueryMother;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static com.zanh.route_sharing.testsupport.sharedroute.CustomUserDetailsMother.activeUser;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(RouteRideRequestQueryControllerSecurityTest.Config.class)
class RouteRideRequestQueryControllerSecurityTest {

        @Autowired
        private RouteRideRequestQueryController sut;

        @Autowired
        private RouteRideRequestQueryService service;

        @BeforeEach
        void setUp() {
                reset(service);
                var mapper = new com.zanh.route_sharing.service.riderequest.query.RouteRideRequestResponseMapper(
                                new com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter(
                                                tools.jackson.databind.json.JsonMapper.builder().build()));
                when(service.listPending(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                RouteRideRequestQueryMother.ROUTE_ID,
                                0,
                                10)).thenReturn(mapper.toPage(
                                                RouteRideRequestQueryMother.page(),
                                                RouteRideRequestQueryMother.READ_AT));
                when(service.getPendingDetail(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                RouteRideRequestQueryMother.ROUTE_ID,
                                RouteRideRequestQueryMother.REQUEST_ID)).thenReturn(mapper.toDetail(
                                                RouteRideRequestQueryMother.detailLookup(),
                                                RouteRideRequestQueryMother.READ_AT));
        }

        @AfterEach
        void clearSecurityContext() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void givenViewAuthority_whenListing_thenPrincipalIdIsDelegated() {
                var principal = activeUser(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                "VIEW_ROUTE_RIDE_REQUESTS");
                authenticate(principal);

                assertThatCode(() -> sut.listPending(
                                principal,
                                RouteRideRequestQueryMother.ROUTE_ID,
                                0,
                                10)).doesNotThrowAnyException();

                verify(service).listPending(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                RouteRideRequestQueryMother.ROUTE_ID,
                                0,
                                10);
        }

        @Test
        void givenViewAuthority_whenGettingDetail_thenPrincipalIdIsDelegated() {
                var principal = activeUser(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                "VIEW_ROUTE_RIDE_REQUESTS");
                authenticate(principal);

                assertThatCode(() -> sut.getPendingDetail(
                                principal,
                                RouteRideRequestQueryMother.ROUTE_ID,
                                RouteRideRequestQueryMother.REQUEST_ID)).doesNotThrowAnyException();

                verify(service).getPendingDetail(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                RouteRideRequestQueryMother.ROUTE_ID,
                                RouteRideRequestQueryMother.REQUEST_ID);
        }

        @Test
        void givenMissingAuthority_whenListing_thenDeniedBeforeServiceCall() {
                var principal = activeUser(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                "CREATE_RIDE_REQUEST");
                authenticate(principal);

                assertThatThrownBy(() -> sut.listPending(
                                principal,
                                RouteRideRequestQueryMother.ROUTE_ID,
                                0,
                                10)).isInstanceOf(AccessDeniedException.class);
                verifyNoInteractions(service);
        }

        @Test
        void givenNoAuthentication_whenGettingDetail_thenAuthenticationIsRequiredBeforeServiceCall() {
                var principal = activeUser(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                "VIEW_ROUTE_RIDE_REQUESTS");

                assertThatThrownBy(() -> sut.getPendingDetail(
                                principal,
                                RouteRideRequestQueryMother.ROUTE_ID,
                                RouteRideRequestQueryMother.REQUEST_ID))
                                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
                verifyNoInteractions(service);
        }

        private static void authenticate(com.zanh.route_sharing.security.CustomUserDetails principal) {
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
                RouteRideRequestQueryService routeRideRequestQueryService() {
                        return mock(RouteRideRequestQueryService.class);
                }

                @Bean
                RouteRideRequestQueryController routeRideRequestQueryController(
                                RouteRideRequestQueryService service) {
                        return new RouteRideRequestQueryController(service);
                }
        }
}
