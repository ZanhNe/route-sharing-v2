package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.RideRequestCreationService;
import com.zanh.route_sharing.testsupport.riderequest.CreateRideRequestRequestBuilder;
import com.zanh.route_sharing.testsupport.riderequest.RideRequestMother;
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

@SpringJUnitConfig(RideRequestControllerSecurityTest.Config.class)
class RideRequestControllerSecurityTest {

    @Autowired
    private RideRequestController sut;

    @Autowired
    private RideRequestCreationService service;

    private CreateRideRequestRequest request;

    @BeforeEach
    void setUp() {
        reset(service);
        request = new CreateRideRequestRequestBuilder().build();
        when(service.create(RideRequestMother.ACTOR_ID, RideRequestMother.ROUTE_ID, request))
                .thenReturn(RideRequestMother.response());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenCreateRideRequestAuthority_whenCreating_thenRequestIsAllowed() {
        CustomUserDetails principal = activeUser(
                RideRequestMother.ACTOR_ID,
                "CREATE_RIDE_REQUEST");
        authenticate(principal);

        assertThatCode(() -> sut.create(
                principal,
                RideRequestMother.ROUTE_ID,
                request)).doesNotThrowAnyException();
        verify(service).create(
                RideRequestMother.ACTOR_ID,
                RideRequestMother.ROUTE_ID,
                request);
    }

    @Test
    void givenMissingCreateRideRequestAuthority_whenCreating_thenDeniedBeforeServiceCall() {
        CustomUserDetails principal = activeUser(
                RideRequestMother.ACTOR_ID,
                "SEARCH_SHARED_ROUTE");
        authenticate(principal);

        assertThatThrownBy(() -> sut.create(
                principal,
                RideRequestMother.ROUTE_ID,
                request)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Test
    void givenNoAuthentication_whenCreating_thenAuthenticationIsRequiredBeforeServiceCall() {
        CustomUserDetails principal = activeUser(
                RideRequestMother.ACTOR_ID,
                "CREATE_RIDE_REQUEST");

        assertThatThrownBy(() -> sut.create(
                principal,
                RideRequestMother.ROUTE_ID,
                request)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verifyNoInteractions(service);
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
        RideRequestCreationService rideRequestCreationService() {
            return mock(RideRequestCreationService.class);
        }

        @Bean
        RideRequestController rideRequestController(RideRequestCreationService service) {
            return new RideRequestController(service);
        }
    }
}
