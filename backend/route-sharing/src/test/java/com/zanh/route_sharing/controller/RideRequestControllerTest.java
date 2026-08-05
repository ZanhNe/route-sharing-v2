package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.dto.riderequest.CreateRideRequestRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.exception.GlobalExceptionHandler;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.RideRequestCreationService;
import com.zanh.route_sharing.service.riderequest.model.RideRequestCreationResult;
import com.zanh.route_sharing.testsupport.riderequest.RideRequestMother;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class RideRequestControllerTest {

    private static final String VALID_KEY = "booking-501";
    private static final String VALID_REQUEST = """
            {
              "schoolId": 1,
              "pickup": {
                "latitude": 10.776530,
                "longitude": 106.700981,
                "address": "Điểm đón hành khách"
              },
              "passengerDestination": {
                "latitude": 10.782120,
                "longitude": 106.712450,
                "address": "Điểm đến cuối cùng"
              },
              "proposedSupportAmount": 25000.00,
              "note": "Tôi đứng tại cổng chính"
            }
            """;

    @Mock
    private RideRequestCreationService service;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        CustomUserDetails principal = new CustomUserDetails(
                RideRequestMother.ACTOR_ID,
                "passenger@university.test",
                "encoded-password",
                TrangThaiTaiKhoan.ACTIVE,
                0L,
                List.of(new SimpleGrantedAuthority("CREATE_RIDE_REQUEST")));

        mockMvc = standaloneSetup(new RideRequestController(service))
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
    void givenValidCommand_whenCreating_thenCreatedEnvelopeLocationAndReplayHeaderMatchContract()
            throws Exception {
        when(service.create(
                eq(RideRequestMother.ACTOR_ID),
                eq(RideRequestMother.ROUTE_ID),
                eq(VALID_KEY),
                any(CreateRideRequestRequest.class)))
                .thenReturn(new RideRequestCreationResult(RideRequestMother.response(), false));

        mockMvc.perform(post("/api/v1/shared-routes/{routeId}/ride-requests", RideRequestMother.ROUTE_ID)
                        .header("Idempotency-Key", VALID_KEY)
                        .contentType(APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/ride-requests/501"))
                .andExpect(header().string("Idempotency-Replayed", "false"))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value(
                        "Gửi yêu cầu đi chung thành công. "
                                + "Yêu cầu đang chờ tài xế xử lý và chưa giữ ghế."))
                .andExpect(jsonPath("$.meta").value(nullValue()))
                .andExpect(jsonPath("$.data.rideRequestId").value(501))
                .andExpect(jsonPath("$.data.routeId").value(22))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.seatReserved").value(false))
                .andExpect(jsonPath("$.data.matchType").value("TRUNG_DOAN_TUYEN"))
                .andExpect(jsonPath("$.data.dropoffType").value("DIEM_THA_TRUNG_GIAN"))
                .andExpect(jsonPath("$.data.proposedSupportAmount").value(25000.00))
                .andExpect(jsonPath("$.data.agreedSupportAmount").value(nullValue()));

        verify(service).create(
                eq(RideRequestMother.ACTOR_ID),
                eq(RideRequestMother.ROUTE_ID),
                eq(VALID_KEY),
                any(CreateRideRequestRequest.class));
    }

    @Test
    void givenIdempotentReplay_whenCreating_thenCreatedStatusAndSameLocationArePreserved()
            throws Exception {
        when(service.create(
                eq(RideRequestMother.ACTOR_ID),
                eq(RideRequestMother.ROUTE_ID),
                eq(VALID_KEY),
                any(CreateRideRequestRequest.class)))
                .thenReturn(new RideRequestCreationResult(RideRequestMother.response(), true));

        mockMvc.perform(post("/api/v1/shared-routes/{routeId}/ride-requests", RideRequestMother.ROUTE_ID)
                        .header("Idempotency-Key", VALID_KEY)
                        .contentType(APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/ride-requests/501"))
                .andExpect(header().string("Idempotency-Replayed", "true"));
    }

    @Test
    void givenInvalidSupportScale_whenBinding_thenValidationErrorIsReturnedBeforeServiceCall()
            throws Exception {
        String invalid = VALID_REQUEST.replace("25000.00", "25000.001");

        mockMvc.perform(post("/api/v1/shared-routes/{routeId}/ride-requests", RideRequestMother.ROUTE_ID)
                        .header("Idempotency-Key", VALID_KEY)
                        .contentType(APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.proposedSupportAmount").exists());

        verifyNoInteractions(service);
    }

    @Test
    void givenMissingIdempotencyKey_whenServiceRejects_thenContractErrorIsReturned()
            throws Exception {
        when(service.create(
                eq(RideRequestMother.ACTOR_ID),
                eq(RideRequestMother.ROUTE_ID),
                isNull(),
                any(CreateRideRequestRequest.class)))
                .thenThrow(new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "MISSING_IDEMPOTENCY_KEY",
                        "Thiếu header Idempotency-Key bắt buộc."));

        mockMvc.perform(post("/api/v1/shared-routes/{routeId}/ride-requests", RideRequestMother.ROUTE_ID)
                        .contentType(APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_IDEMPOTENCY_KEY"))
                .andExpect(jsonPath("$.path").value(
                        "/api/v1/shared-routes/22/ride-requests"));
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
