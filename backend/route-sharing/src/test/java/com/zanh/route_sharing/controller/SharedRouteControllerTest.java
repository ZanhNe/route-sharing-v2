package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.RouteEndpointResponse;
import com.zanh.route_sharing.dto.sharedroute.SharedRouteResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.exception.GlobalExceptionHandler;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.SharedRouteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class SharedRouteControllerTest {

    private static final Long ACTOR_ID = 1L;

    private static final String VALID_REQUEST = """
            {
              "origin": {
                "latitude": 10.762622,
                "longitude": 106.660172,
                "address": "Điểm A"
              },
              "driverDestination": {
                "latitude": 10.823099,
                "longitude": 106.629664,
                "address": "Điểm B"
              },
              "expectedDepartureTime": "2099-08-02T09:00:00Z",
              "vehicleId": 20,
              "offeredSeats": 1,
              "suggestedSupportPerKm": 3000
            }
            """;

    @Mock
    private SharedRouteService sharedRouteService;

    private MockMvc mockMvc;

    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        CustomUserDetails principal = new CustomUserDetails(
                ACTOR_ID,
                "driver1@university.test",
                "encoded-password",
                TrangThaiTaiKhoan.ACTIVE,
                0L,
                List.of(
                        new SimpleGrantedAuthority(
                                "CREATE_SHARED_ROUTE")));

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        SharedRouteController controller = new SharedRouteController(
                sharedRouteService);

        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(
                        new FixedPrincipalResolver(
                                principal))
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.destroy();
    }

    @Test
    void givenValidRequest_whenCreatingSharedRoute_thenReturnsCreatedApiResponse() throws Exception {
        // Arrange
        SharedRouteResponse serviceResult = validServiceResult();

        when(sharedRouteService.createSharedRoute(
                eq(ACTOR_ID),
                any(CreateSharedRouteRequest.class))).thenReturn(serviceResult);

        // Act & Assert
        mockMvc.perform(
                post("/api/v1/shared-routes")
                        .contentType(APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                "/api/v1/shared-routes/100"))
                .andExpect(
                        jsonPath("$.status")
                                .value(201))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Tạo lộ trình chia sẻ thành công."))
                .andExpect(
                        jsonPath("$.meta")
                                .value(nullValue()))
                .andExpect(
                        jsonPath("$.data.id")
                                .value(100))
                .andExpect(
                        jsonPath("$.data.status")
                                .value("OPEN"))
                .andExpect(
                        jsonPath("$.data.offeredSeats")
                                .value(1))
                .andExpect(
                        jsonPath("$.data.remainingSeats")
                                .value(1))
                .andExpect(
                        jsonPath("$.data.driverId")
                                .value(ACTOR_ID))
                .andExpect(
                        jsonPath("$.data.vehicleId")
                                .value(20));

        verify(sharedRouteService)
                .createSharedRoute(
                        eq(ACTOR_ID),
                        any(CreateSharedRouteRequest.class));
    }

    @Test
    void givenInvalidOfferedSeats_whenCreatingSharedRoute_thenReturnsValidationErrorResponse() throws Exception {
        // Arrange
        String invalidRequest = """
                {
                  "origin": {
                    "latitude": 10.762622,
                    "longitude": 106.660172,
                    "address": "Điểm A"
                  },
                  "driverDestination": {
                    "latitude": 10.823099,
                    "longitude": 106.629664,
                    "address": "Điểm B"
                  },
                  "expectedDepartureTime": "2099-08-02T09:00:00Z",
                  "vehicleId": 20,
                  "offeredSeats": 0,
                  "suggestedSupportPerKm": 3000
                }
                """;

        // Act & Assert
        mockMvc.perform(
                post("/api/v1/shared-routes")
                        .contentType(APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400))
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_ERROR"))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Dữ liệu đầu vào không hợp lệ."))
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/v1/shared-routes"))
                .andExpect(
                        jsonPath("$.errors.offeredSeats")
                                .value(
                                        "Số ghế cung cấp phải lớn hơn hoặc bằng 1."));

        verifyNoInteractions(sharedRouteService);
    }

    @Test
    void givenUnauthorizedVehicle_whenServiceRejects_thenReturnsBusinessErrorResponse() throws Exception {
        // Arrange
        when(sharedRouteService.createSharedRoute(
                eq(ACTOR_ID),
                any(CreateSharedRouteRequest.class))).thenThrow(
                        new BusinessException(
                                HttpStatus.FORBIDDEN,
                                "VEHICLE_NOT_AUTHORIZED",
                                "Bạn không có quyền sử dụng "
                                        + "phương tiện này để đăng lộ trình."));

        // Act & Assert
        mockMvc.perform(
                post("/api/v1/shared-routes")
                        .contentType(APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.status")
                                .value(403))
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "VEHICLE_NOT_AUTHORIZED"))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Bạn không có quyền sử dụng "
                                                + "phương tiện này để đăng lộ trình."))
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/v1/shared-routes"))
                .andExpect(
                        jsonPath("$.errors")
                                .value(nullValue()))
                .andExpect(
                        jsonPath("$.referenceCode")
                                .value(nullValue()));
    }

    private static SharedRouteResponse validServiceResult() {
        return new SharedRouteResponse(
                100L,
                TrangThaiLoTrinh.OPEN,
                Instant.parse(
                        "2099-08-02T09:00:00Z"),
                1,
                1,
                new BigDecimal("12500.00"),
                2100L,
                new BigDecimal("3000.00"),
                new RouteEndpointResponse(
                        new BigDecimal("10.762622"),
                        new BigDecimal("106.660172"),
                        "Điểm A"),
                new RouteEndpointResponse(
                        new BigDecimal("10.823099"),
                        new BigDecimal("106.629664"),
                        "Điểm B"),
                ACTOR_ID,
                20L,
                Instant.parse(
                        "2026-08-02T02:00:00Z"));
    }

    /**
     * Controller contract test không khởi động
     * Spring Security filter chain.
     *
     * Resolver này cung cấp principal cố định cho
     * tham số @AuthenticationPrincipal.
     */
    private record FixedPrincipalResolver(
            CustomUserDetails principal) implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(
                MethodParameter parameter) {
            return parameter.hasParameterAnnotation(
                    AuthenticationPrincipal.class)
                    && CustomUserDetails.class.isAssignableFrom(
                            parameter.getParameterType());
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