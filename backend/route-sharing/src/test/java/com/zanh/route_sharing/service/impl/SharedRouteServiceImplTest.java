package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiXe;
import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.integration.goong.RouteCoordinate;
import com.zanh.route_sharing.repository.HoSoTaiXeRepository;
import com.zanh.route_sharing.repository.LoTrinhChiaSeRepository;
import com.zanh.route_sharing.repository.NguoiDungRepository;
import com.zanh.route_sharing.repository.PhuongTienRepository;
import com.zanh.route_sharing.service.GoongRouteService;
import com.zanh.route_sharing.service.impl.SharedRouteServiceImpl;
import com.zanh.route_sharing.testfixture.CreateSharedRouteRequestTestBuilder;
import com.zanh.route_sharing.testfixture.SharedRouteMother;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedRouteServiceImplTest {

        private static final long ACTOR_ID = 1L;
        private static final long DRIVER_PROFILE_ID = 10L;
        private static final long VEHICLE_ID = 20L;

        @Mock
        private NguoiDungRepository nguoiDungRepository;

        @Mock
        private HoSoTaiXeRepository hoSoTaiXeRepository;

        @Mock
        private PhuongTienRepository phuongTienRepository;

        @Mock
        private LoTrinhChiaSeRepository loTrinhChiaSeRepository;

        @Mock
        private GoongRouteService goongRouteService;

        @Mock
        private EntityManager entityManager;

        private SharedRouteServiceImpl sut;

        @BeforeEach
        void setUp() {
                GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

                Clock fixedClock = Clock.fixed(
                                SharedRouteMother.NOW,
                                ZoneOffset.UTC);

                sut = new SharedRouteServiceImpl(
                                nguoiDungRepository,
                                hoSoTaiXeRepository,
                                phuongTienRepository,
                                loTrinhChiaSeRepository,
                                goongRouteService,
                                geometryFactory,
                                fixedClock,
                                entityManager);
        }

        @Test
        void givenValidData_whenCreatingSharedRoute_thenReturnsOpenRouteAndPersistsCorrectGeometry() {
                // Arrange
                NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
                HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(
                                DRIVER_PROFILE_ID,
                                actor);
                PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                                VEHICLE_ID,
                                actor,
                                2);
                CreateSharedRouteRequest request = CreateSharedRouteRequestTestBuilder.aValidRequest()
                                .withVehicleId(VEHICLE_ID)
                                .withOfferedSeats(2)
                                .build();

                givenExistingContext(actor, profile, vehicle);

                when(goongRouteService.calculate(
                                any(RouteCoordinate.class),
                                any(RouteCoordinate.class),
                                eq(LoaiPhuongTien.XE_MAY))).thenReturn(SharedRouteMother.validCalculation());

                when(loTrinhChiaSeRepository.saveAndFlush(any()))
                                .thenAnswer(invocation -> {
                                        LoTrinhChiaSe entity = invocation.getArgument(0);
                                        entity.setId(100L);
                                        entity.setCreatedAt(SharedRouteMother.NOW);
                                        return entity;
                                });

                // Act
                var result = sut.createSharedRoute(ACTOR_ID, request);

                // Assert - state/output based
                assertThat(result.id()).isEqualTo(100L);
                assertThat(result.status()).isEqualTo(TrangThaiLoTrinh.OPEN);
                assertThat(result.offeredSeats()).isEqualTo(2);
                assertThat(result.remainingSeats()).isEqualTo(2);
                assertThat(result.driverId()).isEqualTo(ACTOR_ID);
                assertThat(result.vehicleId()).isEqualTo(VEHICLE_ID);
                assertThat(result.estimatedDistanceMeters())
                                .isEqualByComparingTo("12500");
                assertThat(result.estimatedDurationSeconds()).isEqualTo(2100L);

                ArgumentCaptor<LoTrinhChiaSe> routeCaptor = ArgumentCaptor.forClass(LoTrinhChiaSe.class);

                verify(loTrinhChiaSeRepository)
                                .saveAndFlush(routeCaptor.capture());

                LoTrinhChiaSe persistedRoute = routeCaptor.getValue();

                assertThat(persistedRoute.getTrangThaiLoTrinh())
                                .isEqualTo(TrangThaiLoTrinh.OPEN);
                assertThat(persistedRoute.getSoGheConLai()).isEqualTo(2);
                assertThat(persistedRoute.getDiemXuatPhat().getSRID())
                                .isEqualTo(4326);
                assertThat(persistedRoute.getDiemXuatPhat().getX())
                                .isEqualTo(106.660172);
                assertThat(persistedRoute.getDiemXuatPhat().getY())
                                .isEqualTo(10.762622);
                assertThat(persistedRoute.getTuyenDuongGoc().getSRID())
                                .isEqualTo(4326);
                assertThat(persistedRoute.getTuyenDuongGoc().getNumPoints())
                                .isEqualTo(2);

                // Assert
                verify(goongRouteService).calculate(
                                new RouteCoordinate(
                                                new BigDecimal("10.762622"),
                                                new BigDecimal("106.660172")),
                                new RouteCoordinate(
                                                new BigDecimal("10.823099"),
                                                new BigDecimal("106.629664")),
                                LoaiPhuongTien.XE_MAY);
        }

        @Test
        void givenMissingAccount_whenCreatingSharedRoute_thenReturnsAccountNotFoundWithoutCallingDependencies() {
                // Arrange
                CreateSharedRouteRequest request = CreateSharedRouteRequestTestBuilder.aValidRequest().build();

                when(nguoiDungRepository.findById(ACTOR_ID))
                                .thenReturn(Optional.empty());

                // Act
                BusinessException exception = catchThrowableOfType(
                                () -> sut.createSharedRoute(ACTOR_ID, request),
                                BusinessException.class);

                // Assert
                assertBusinessError(
                                exception,
                                HttpStatus.NOT_FOUND,
                                "ACCOUNT_NOT_FOUND");
                verifyNoInteractions(
                                hoSoTaiXeRepository,
                                phuongTienRepository,
                                goongRouteService,
                                loTrinhChiaSeRepository);
        }

        @Test
        void givenInactiveAccount_whenCreatingSharedRoute_thenRejectsBeforeCallingGoong() {
                // Arrange
                NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
                actor.setTrangThaiTaiKhoan(TrangThaiTaiKhoan.SUSPENDED);

                HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(
                                DRIVER_PROFILE_ID,
                                actor);
                PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                                VEHICLE_ID,
                                actor,
                                1);

                givenExistingContext(actor, profile, vehicle);

                // Act
                BusinessException exception = catchThrowableOfType(
                                () -> sut.createSharedRoute(
                                                ACTOR_ID,
                                                CreateSharedRouteRequestTestBuilder
                                                                .aValidRequest()
                                                                .build()),
                                BusinessException.class);

                // Assert
                assertBusinessError(
                                exception,
                                HttpStatus.FORBIDDEN,
                                "ACCOUNT_NOT_ACTIVE");
                verifyNoInteractions(goongRouteService);
                verify(loTrinhChiaSeRepository, never())
                                .saveAndFlush(any());
        }

        @Test
        void givenInactiveDriverProfile_whenCreatingSharedRoute_thenRejectsBeforeCallingGoong() {
                // Arrange
                NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
                HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(
                                DRIVER_PROFILE_ID,
                                actor);
                profile.setTrangThaiTaiXe(TrangThaiTaiXe.SUSPENDED);

                PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                                VEHICLE_ID,
                                actor,
                                1);

                givenExistingContext(actor, profile, vehicle);

                // Act
                BusinessException exception = catchThrowableOfType(
                                () -> sut.createSharedRoute(
                                                ACTOR_ID,
                                                CreateSharedRouteRequestTestBuilder
                                                                .aValidRequest()
                                                                .build()),
                                BusinessException.class);

                // Assert
                assertBusinessError(
                                exception,
                                HttpStatus.CONFLICT,
                                "DRIVER_NOT_ACTIVE");
                verifyNoInteractions(goongRouteService);
                verify(loTrinhChiaSeRepository, never())
                                .saveAndFlush(any());
        }

        @Test
        void givenVehicleOwnedByAnotherUser_whenCreatingSharedRoute_thenRejectsBeforeCallingGoong() {
                // Arrange
                NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
                NguoiDung otherUser = SharedRouteMother.activeUser(2L);
                HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(
                                DRIVER_PROFILE_ID,
                                actor);
                PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                                VEHICLE_ID,
                                otherUser,
                                1);

                givenExistingContext(actor, profile, vehicle);

                // Act
                BusinessException exception = catchThrowableOfType(
                                () -> sut.createSharedRoute(
                                                ACTOR_ID,
                                                CreateSharedRouteRequestTestBuilder
                                                                .aValidRequest()
                                                                .build()),
                                BusinessException.class);

                // Assert
                assertBusinessError(
                                exception,
                                HttpStatus.FORBIDDEN,
                                "VEHICLE_NOT_AUTHORIZED");
                verifyNoInteractions(goongRouteService);
                verify(loTrinhChiaSeRepository, never())
                                .saveAndFlush(any());
        }

        @Test
        void givenOfferedSeatsExceedCapacity_whenCreatingSharedRoute_thenRejectsBeforeCallingGoong() {
                // Arrange
                NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
                HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(
                                DRIVER_PROFILE_ID,
                                actor);
                PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                                VEHICLE_ID,
                                actor,
                                1);

                givenExistingContext(actor, profile, vehicle);

                CreateSharedRouteRequest request = CreateSharedRouteRequestTestBuilder.aValidRequest()
                                .withOfferedSeats(2)
                                .build();

                // Act
                BusinessException exception = catchThrowableOfType(
                                () -> sut.createSharedRoute(ACTOR_ID, request),
                                BusinessException.class);

                // Assert
                assertBusinessError(
                                exception,
                                HttpStatus.CONFLICT,
                                "SEAT_COUNT_EXCEEDS_VEHICLE_CAPACITY");
                verifyNoInteractions(goongRouteService);
                verify(loTrinhChiaSeRepository, never())
                                .saveAndFlush(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                        "2026-08-01T08:00:00Z",
                        "2026-08-01T07:59:59Z"
        })
        void givenDepartureTimeNotAfterCurrentTime_whenCreatingSharedRoute_thenRejects(
                        String departureTimeText) {
                // Arrange
                NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
                HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(
                                DRIVER_PROFILE_ID,
                                actor);
                PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                                VEHICLE_ID,
                                actor,
                                1);

                givenExistingContext(actor, profile, vehicle);

                CreateSharedRouteRequest request = CreateSharedRouteRequestTestBuilder.aValidRequest()
                                .withDepartureTime(Instant.parse(departureTimeText))
                                .build();

                // Act
                BusinessException exception = catchThrowableOfType(
                                () -> sut.createSharedRoute(ACTOR_ID, request),
                                BusinessException.class);

                // Assert
                assertBusinessError(
                                exception,
                                HttpStatus.BAD_REQUEST,
                                "DEPARTURE_TIME_MUST_BE_FUTURE");
                verifyNoInteractions(goongRouteService);
                verify(loTrinhChiaSeRepository, never())
                                .saveAndFlush(any());
        }

        @Test
        void givenSameOriginAndDestination_whenCreatingSharedRoute_thenRejectsBeforeCallingGoong() {
                // Arrange
                NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
                HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(
                                DRIVER_PROFILE_ID,
                                actor);
                PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                                VEHICLE_ID,
                                actor,
                                1);

                givenExistingContext(actor, profile, vehicle);

                var sameEndpoint = SharedRouteMother.endpoint(
                                "10.762622",
                                "106.660172",
                                "Cùng một điểm");

                CreateSharedRouteRequest request = CreateSharedRouteRequestTestBuilder.aValidRequest()
                                .withOrigin(sameEndpoint)
                                .withDestination(sameEndpoint)
                                .build();

                // Act
                BusinessException exception = catchThrowableOfType(
                                () -> sut.createSharedRoute(ACTOR_ID, request),
                                BusinessException.class);

                // Assert
                assertBusinessError(
                                exception,
                                HttpStatus.BAD_REQUEST,
                                "ROUTE_ENDPOINTS_MUST_BE_DIFFERENT");
                verifyNoInteractions(goongRouteService);
                verify(loTrinhChiaSeRepository, never())
                                .saveAndFlush(any());
        }

        @Test
        void givenVehicleSuspendedWhileWaitingForMapProvider_whenCommitting_thenRejectsAndDoesNotPersist() {
                // Arrange
                NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);

                HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(
                                DRIVER_PROFILE_ID,
                                actor);

                PhuongTien vehicle = SharedRouteMother.activeMotorbike(
                                VEHICLE_ID,
                                actor,
                                1);

                givenExistingContext(
                                actor,
                                profile,
                                vehicle);

                when(goongRouteService.calculate(
                                any(),
                                any(),
                                any())).thenReturn(
                                                SharedRouteMother.validCalculation());

                doAnswer(invocation -> {
                        Object refreshedEntity = invocation.getArgument(0);

                        if (refreshedEntity == vehicle) {
                                vehicle.setTrangThaiPhuongTien(
                                                TrangThaiPhuongTien.SUSPENDED);
                        }

                        return null;
                }).when(entityManager).refresh(
                                any(Object.class));

                // Act
                BusinessException exception = catchThrowableOfType(
                                () -> sut.createSharedRoute(
                                                ACTOR_ID,
                                                CreateSharedRouteRequestTestBuilder
                                                                .aValidRequest()
                                                                .build()),
                                BusinessException.class);

                // Assert
                assertBusinessError(
                                exception,
                                HttpStatus.CONFLICT,
                                "VEHICLE_NOT_ACTIVE");

                verify(goongRouteService)
                                .calculate(
                                                any(),
                                                any(),
                                                any());

                verify(
                                loTrinhChiaSeRepository,
                                never()).saveAndFlush(any());

        }

        private void givenExistingContext(
                        NguoiDung actor,
                        HoSoTaiXe profile,
                        PhuongTien vehicle) {
                when(nguoiDungRepository.findById(ACTOR_ID))
                                .thenReturn(Optional.of(actor));
                when(hoSoTaiXeRepository.findByUserIdForRouteCreation(ACTOR_ID))
                                .thenReturn(Optional.of(profile));
                when(phuongTienRepository.findByIdForRouteCreation(VEHICLE_ID))
                                .thenReturn(Optional.of(vehicle));
        }

        private static void assertBusinessError(
                        BusinessException exception,
                        HttpStatus expectedStatus,
                        String expectedCode) {
                assertThat(exception).isNotNull();
                assertThat(exception.getStatus()).isEqualTo(expectedStatus);
                assertThat(exception.getCode()).isEqualTo(expectedCode);
        }
}
