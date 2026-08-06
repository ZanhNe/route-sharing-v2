package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.riderequest.query.RouteRideRequestDetailResponse;
import com.zanh.route_sharing.exception.GlobalExceptionHandler;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.RouteRideRequestQueryService;
import com.zanh.route_sharing.service.riderequest.query.RouteRideRequestResponseMapper;
import com.zanh.route_sharing.testsupport.riderequest.query.RouteRideRequestQueryMother;
import com.zanh.route_sharing.utils.spatial.RouteGeoJsonWriter;
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
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static com.zanh.route_sharing.testsupport.sharedroute.CustomUserDetailsMother.activeUser;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class RouteRideRequestQueryControllerTest {

        @Mock
        private RouteRideRequestQueryService service;

        private MockMvc mockMvc;
        private LocalValidatorFactoryBean validator;

        @BeforeEach
        void setUp() {
                validator = new LocalValidatorFactoryBean();
                validator.afterPropertiesSet();
                CustomUserDetails principal = activeUser(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                "VIEW_ROUTE_RIDE_REQUESTS");

                MethodValidationPostProcessor methodValidation = new MethodValidationPostProcessor();
                methodValidation.setValidator(validator);
                methodValidation.setProxyTargetClass(true);
                methodValidation.afterPropertiesSet();

                RouteRideRequestQueryController controller = (RouteRideRequestQueryController) methodValidation
                                .postProcessAfterInitialization(
                                                new RouteRideRequestQueryController(service),
                                                "routeRideRequestQueryController");

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
        void givenOwnedRoute_whenListingPending_thenEnvelopePaginationAndSummaryMatchContract()
                        throws Exception {
                RouteRideRequestResponseMapper mapper = mapper();
                when(service.listPending(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                RouteRideRequestQueryMother.ROUTE_ID,
                                0,
                                10)).thenReturn(mapper.toPage(
                                                RouteRideRequestQueryMother.page(),
                                                RouteRideRequestQueryMother.READ_AT));

                mockMvc.perform(get(
                                "/api/v1/shared-routes/{routeId}/ride-requests",
                                RouteRideRequestQueryMother.ROUTE_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(200))
                                .andExpect(jsonPath("$.message").value(
                                                "Lấy danh sách yêu cầu đi chung đang chờ xử lý thành công."))
                                .andExpect(jsonPath("$.data.route.routeId").value(22))
                                .andExpect(jsonPath("$.data.items[0].rideRequestId").value(501))
                                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
                                .andExpect(jsonPath("$.data.items[0].passenger.passengerId").value(7))
                                .andExpect(jsonPath("$.data.items[0].passenger.fullName").value("Nguyễn Văn A"))
                                .andExpect(jsonPath("$.meta.page").value(0))
                                .andExpect(jsonPath("$.meta.size").value(10))
                                .andExpect(jsonPath("$.meta.totalElements").value(1));

                verify(service).listPending(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                RouteRideRequestQueryMother.ROUTE_ID,
                                0,
                                10);
        }

        @Test
        void givenPendingRequest_whenGettingDetail_thenPassengerRequestAndStoredMapMatchContract()
                        throws Exception {
                RouteRideRequestDetailResponse detail = mapper().toDetail(
                                RouteRideRequestQueryMother.detailLookup(),
                                RouteRideRequestQueryMother.READ_AT);
                when(service.getPendingDetail(
                                RouteRideRequestQueryMother.ACTOR_ID,
                                RouteRideRequestQueryMother.ROUTE_ID,
                                RouteRideRequestQueryMother.REQUEST_ID)).thenReturn(detail);

                mockMvc.perform(get(
                                "/api/v1/shared-routes/{routeId}/ride-requests/{rideRequestId}",
                                RouteRideRequestQueryMother.ROUTE_ID,
                                RouteRideRequestQueryMother.REQUEST_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(200))
                                .andExpect(jsonPath("$.meta").value(nullValue()))
                                .andExpect(jsonPath("$.data.passenger.passengerId").value(7))
                                .andExpect(jsonPath("$.data.passenger.gender").value("NAM"))
                                .andExpect(jsonPath("$.data.request.status").value("PENDING"))
                                .andExpect(jsonPath("$.data.request.pickup.latitude").value(10.776530))
                                .andExpect(jsonPath("$.data.map.originalDriverRoute.meaning")
                                                .value("DRIVER_ORIGINAL_ROUTE"))
                                .andExpect(jsonPath("$.data.map.passengerDesiredRoute.meaning")
                                                .value("PASSENGER_DESIRED_ROUTE_VIA_DROPOFF"))
                                .andExpect(jsonPath("$.data.map.servedSegment.meaning")
                                                .value("PASSENGER_SERVED_SEGMENT"))
                                .andExpect(jsonPath("$.data.map.markers.length()").value(5))
                                .andExpect(jsonPath("$.data.passenger.emailTruong").doesNotExist())
                                .andExpect(jsonPath("$.data.passenger.soDienThoai").doesNotExist())
                                .andExpect(jsonPath("$.data.passenger.securityVersion").doesNotExist())
                                .andExpect(jsonPath("$.data.request.routeVersionLucGui").doesNotExist())
                                .andExpect(jsonPath("$.data.request.requestTtlAppliedSeconds").doesNotExist());
        }

        @Test
        void givenInvalidPageOrSize_whenListing_thenValidationStopsBeforeServiceCall()
                        throws Exception {
                mockMvc.perform(get(
                                "/api/v1/shared-routes/{routeId}/ride-requests",
                                RouteRideRequestQueryMother.ROUTE_ID)
                                .queryParam("page", "-1"))
                                .andExpect(status().isBadRequest());

                mockMvc.perform(get(
                                "/api/v1/shared-routes/{routeId}/ride-requests",
                                RouteRideRequestQueryMother.ROUTE_ID)
                                .queryParam("size", "51"))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(service);
        }

        private static RouteRideRequestResponseMapper mapper() {
                return new RouteRideRequestResponseMapper(
                                new RouteGeoJsonWriter(JsonMapper.builder().build()));
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
