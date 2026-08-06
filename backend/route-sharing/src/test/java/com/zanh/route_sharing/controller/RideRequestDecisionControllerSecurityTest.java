package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.service.RideRequestDecisionService;
import com.zanh.route_sharing.service.riderequest.decision.RideRequestDecisionResponseMapper;
import com.zanh.route_sharing.service.riderequest.decision.model.RideRequestDecisionResult;
import com.zanh.route_sharing.testsupport.riderequest.decision.RideRequestDecisionMother;
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

@SpringJUnitConfig(RideRequestDecisionControllerSecurityTest.Config.class)
class RideRequestDecisionControllerSecurityTest {

    @Autowired
    private RideRequestDecisionController sut;
    @Autowired
    private RideRequestDecisionService service;

    @BeforeEach
    void setUp() {
        reset(service);
        RideRequestDecisionResult result = new RideRequestDecisionResult(
                RideRequestDecisionMother.ROUTE_ID,
                RideRequestDecisionMother.REQUEST_ID,
                com.zanh.route_sharing.domain.enums.TrangThaiYeuCau.ACCEPTED,
                RideRequestDecisionMother.DECISION_AT,
                1,
                RideRequestDecisionMother.PROPOSED_SUPPORT,
                null);
        when(service.accept(
                RideRequestDecisionMother.ACTOR_ID,
                RideRequestDecisionMother.ROUTE_ID,
                RideRequestDecisionMother.REQUEST_ID)).thenReturn(result);
        when(service.reject(
                RideRequestDecisionMother.ACTOR_ID,
                RideRequestDecisionMother.ROUTE_ID,
                RideRequestDecisionMother.REQUEST_ID)).thenReturn(result);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenRespondAuthority_whenAccepting_thenPrincipalIdIsDelegated() {
        var principal = activeUser(RideRequestDecisionMother.ACTOR_ID, "RESPOND_RIDE_REQUEST");
        authenticate(principal);

        assertThatCode(() -> sut.accept(
                principal,
                RideRequestDecisionMother.ROUTE_ID,
                RideRequestDecisionMother.REQUEST_ID)).doesNotThrowAnyException();

        verify(service).accept(
                RideRequestDecisionMother.ACTOR_ID,
                RideRequestDecisionMother.ROUTE_ID,
                RideRequestDecisionMother.REQUEST_ID);
    }

    @Test
    void givenRespondAuthority_whenRejecting_thenPrincipalIdIsDelegated() {
        var principal = activeUser(RideRequestDecisionMother.ACTOR_ID, "RESPOND_RIDE_REQUEST");
        authenticate(principal);

        assertThatCode(() -> sut.reject(
                principal,
                RideRequestDecisionMother.ROUTE_ID,
                RideRequestDecisionMother.REQUEST_ID)).doesNotThrowAnyException();

        verify(service).reject(
                RideRequestDecisionMother.ACTOR_ID,
                RideRequestDecisionMother.ROUTE_ID,
                RideRequestDecisionMother.REQUEST_ID);
    }

    @Test
    void givenMissingAuthority_whenAccepting_thenDeniedBeforeServiceCall() {
        var principal = activeUser(RideRequestDecisionMother.ACTOR_ID, "VIEW_ROUTE_RIDE_REQUESTS");
        authenticate(principal);

        assertThatThrownBy(() -> sut.accept(
                principal,
                RideRequestDecisionMother.ROUTE_ID,
                RideRequestDecisionMother.REQUEST_ID)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Test
    void givenNoAuthentication_whenRejecting_thenAuthenticationRequiredBeforeServiceCall() {
        var principal = activeUser(RideRequestDecisionMother.ACTOR_ID, "RESPOND_RIDE_REQUEST");

        assertThatThrownBy(() -> sut.reject(
                principal,
                RideRequestDecisionMother.ROUTE_ID,
                RideRequestDecisionMother.REQUEST_ID))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verifyNoInteractions(service);
    }

    private static void authenticate(com.zanh.route_sharing.security.CustomUserDetails principal) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean
        RideRequestDecisionService rideRequestDecisionService() {
            return mock(RideRequestDecisionService.class);
        }

        @Bean
        RideRequestDecisionResponseMapper rideRequestDecisionResponseMapper() {
            return new RideRequestDecisionResponseMapper();
        }

        @Bean
        RideRequestDecisionController rideRequestDecisionController(
                RideRequestDecisionService service,
                RideRequestDecisionResponseMapper mapper) {
            return new RideRequestDecisionController(service, mapper);
        }
    }
}
