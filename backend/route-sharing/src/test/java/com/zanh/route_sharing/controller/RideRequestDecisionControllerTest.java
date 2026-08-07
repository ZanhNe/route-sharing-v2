package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.domain.enums.TrangThaiYeuCau;
import com.zanh.route_sharing.exception.GlobalExceptionHandler;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.RideRequestDecisionService;
import com.zanh.route_sharing.dto.riderequest.decision.RideRequestDecisionResponse;
import com.zanh.route_sharing.testsupport.riderequest.decision.RideRequestDecisionMother;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;

import static com.zanh.route_sharing.testsupport.sharedroute.CustomUserDetailsMother.activeUser;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class RideRequestDecisionControllerTest {

        @Mock
        private RideRequestDecisionService service;

        private MockMvc mockMvc;
        private LocalValidatorFactoryBean validator;

        @BeforeEach
        void setUp() {
                validator = new LocalValidatorFactoryBean();
                validator.afterPropertiesSet();
                CustomUserDetails principal = activeUser(
                                RideRequestDecisionMother.ACTOR_ID,
                                "RESPOND_RIDE_REQUEST");
                MethodValidationPostProcessor methodValidation = new MethodValidationPostProcessor();
                methodValidation.setValidator(validator);
                methodValidation.setProxyTargetClass(true);
                methodValidation.afterPropertiesSet();
                RideRequestDecisionController controller = (RideRequestDecisionController) methodValidation
                                .postProcessAfterInitialization(
                                                new RideRequestDecisionController(service),
                                                "rideRequestDecisionController");
                mockMvc = standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .setValidator(validator)
                                .setCustomArgumentResolvers(new FixedPrincipalResolver(principal))
                                .build();
        }

        @AfterEach
        void tearDown() {
                validator.destroy();
        }

        @Test
        void givenAcceptSuccess_whenPosting_thenDecisionEnvelopeMatchesContract() throws Exception {
                when(service.accept(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID)).thenReturn(new RideRequestDecisionResponse(
                                                RideRequestDecisionMother.ROUTE_ID,
                                                RideRequestDecisionMother.REQUEST_ID,
                                                TrangThaiYeuCau.ACCEPTED,
                                                RideRequestDecisionMother.DECISION_AT,
                                                1,
                                                new BigDecimal("25000.00"),
                                                null));

                mockMvc.perform(post(
                                "/api/v1/shared-routes/{routeId}/ride-requests/{rideRequestId}/accept",
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(200))
                                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                                .andExpect(jsonPath("$.data.remainingSeats").value(1))
                                .andExpect(jsonPath("$.data.agreedSupportAmount").value(25000.00))
                                .andExpect(jsonPath("$.data.cooldownUntil").value(nullValue()));
        }

        @Test
        void givenRejectSuccess_whenPosting_thenCooldownAndUnchangedSeatMatchContract() throws Exception {
                Instant cooldownUntil = RideRequestDecisionMother.DECISION_AT.plusSeconds(3600);
                when(service.reject(
                                RideRequestDecisionMother.ACTOR_ID,
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID)).thenReturn(new RideRequestDecisionResponse(
                                                RideRequestDecisionMother.ROUTE_ID,
                                                RideRequestDecisionMother.REQUEST_ID,
                                                TrangThaiYeuCau.REJECTED,
                                                RideRequestDecisionMother.DECISION_AT,
                                                2,
                                                null,
                                                cooldownUntil));

                mockMvc.perform(post(
                                "/api/v1/shared-routes/{routeId}/ride-requests/{rideRequestId}/reject",
                                RideRequestDecisionMother.ROUTE_ID,
                                RideRequestDecisionMother.REQUEST_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                                .andExpect(jsonPath("$.data.remainingSeats").value(2))
                                .andExpect(jsonPath("$.data.agreedSupportAmount").value(nullValue()))
                                .andExpect(jsonPath("$.data.cooldownUntil").value(cooldownUntil.toString()));
        }

        @Test
        void givenInvalidPathIds_whenPosting_thenValidationStopsBeforeService() throws Exception {
                mockMvc.perform(post(
                                "/api/v1/shared-routes/{routeId}/ride-requests/{rideRequestId}/accept",
                                0,
                                1))
                                .andExpect(status().isBadRequest());
                mockMvc.perform(post(
                                "/api/v1/shared-routes/{routeId}/ride-requests/{rideRequestId}/reject",
                                1,
                                0))
                                .andExpect(status().isBadRequest());
                verifyNoInteractions(service);
        }

        private record FixedPrincipalResolver(
                        CustomUserDetails principal) implements HandlerMethodArgumentResolver {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                                        && CustomUserDetails.class.isAssignableFrom(parameter.getParameterType());
                }

                @Override
                public Object resolveArgument(
                                MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
                        return principal;
                }
        }
}
