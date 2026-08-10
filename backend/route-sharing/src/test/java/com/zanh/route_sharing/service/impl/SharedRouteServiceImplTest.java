package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiXe;
import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.HoSoTaiXeRepository;
import com.zanh.route_sharing.repository.NguoiDungRepository;
import com.zanh.route_sharing.repository.PhuongTienRepository;
import com.zanh.route_sharing.service.routing.RoutePlanner;
import com.zanh.route_sharing.service.routing.model.RoutePlanRequest;
import com.zanh.route_sharing.service.sharedroute.SharedRoutePublicationCommitter;
import com.zanh.route_sharing.service.sharedroute.SharedRoutePublicationPolicy;
import com.zanh.route_sharing.testfixture.CreateSharedRouteRequestTestBuilder;
import com.zanh.route_sharing.testfixture.SharedRouteMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedRouteServiceImplTest {

    private static final long ACTOR_ID = 1L;
    private static final long DRIVER_PROFILE_ID = 10L;
    private static final long VEHICLE_ID = 20L;

    @Mock private NguoiDungRepository nguoiDungRepository;
    @Mock private HoSoTaiXeRepository hoSoTaiXeRepository;
    @Mock private PhuongTienRepository phuongTienRepository;
    @Mock private RoutePlanner routePlanner;
    @Mock private SharedRoutePublicationCommitter publicationCommitter;

    private SharedRouteServiceImpl sut;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(SharedRouteMother.NOW, ZoneOffset.UTC);
        sut = new SharedRouteServiceImpl(
                nguoiDungRepository,
                hoSoTaiXeRepository,
                phuongTienRepository,
                routePlanner,
                publicationCommitter,
                new SharedRoutePublicationPolicy(),
                fixedClock);
    }

    @Test
    void givenValidData_whenCreatingSharedRoute_thenPlansThroughProviderNeutralPortAndCommits() {
        NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
        HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(DRIVER_PROFILE_ID, actor);
        PhuongTien vehicle = SharedRouteMother.activeMotorbike(VEHICLE_ID, actor, 2);
        CreateSharedRouteRequest request = CreateSharedRouteRequestTestBuilder.aValidRequest()
                .withVehicleId(VEHICLE_ID)
                .withOfferedSeats(2)
                .build();
        givenExistingContext(actor, profile, vehicle);

        var plan = SharedRouteMother.validRoutePlan();
        when(routePlanner.plan(any())).thenReturn(plan);
        when(publicationCommitter.commit(ACTOR_ID, request, plan))
                .thenReturn(SharedRouteMother.persistedOpenRoute(actor, vehicle, request, plan, 100L));

        var result = sut.createSharedRoute(ACTOR_ID, request);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.offeredSeats()).isEqualTo(2);
        assertThat(result.remainingSeats()).isEqualTo(2);
        assertThat(result.estimatedDistanceMeters()).isEqualByComparingTo("12500");
        assertThat(result.estimatedDurationSeconds()).isEqualTo(2100L);

        ArgumentCaptor<RoutePlanRequest> routeRequest = ArgumentCaptor.forClass(RoutePlanRequest.class);
        verify(routePlanner).plan(routeRequest.capture());
        assertThat(routeRequest.getValue().vehicleType()).isEqualTo(LoaiPhuongTien.XE_MAY);
        assertThat(routeRequest.getValue().waypoints()).hasSize(2);
        verify(publicationCommitter).commit(ACTOR_ID, request, plan);
    }

    @Test
    void givenMissingAccount_whenCreatingSharedRoute_thenRejectsBeforePlannerAndCommitter() {
        CreateSharedRouteRequest request = CreateSharedRouteRequestTestBuilder.aValidRequest().build();
        when(nguoiDungRepository.findById(ACTOR_ID)).thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> sut.createSharedRoute(ACTOR_ID, request), BusinessException.class);

        assertBusinessError(exception, HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND");
        verifyNoInteractions(hoSoTaiXeRepository, phuongTienRepository, routePlanner, publicationCommitter);
    }

    @Test
    void givenInactiveAccount_whenCreatingSharedRoute_thenRejectsBeforePlanner() {
        NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
        actor.setTrangThaiTaiKhoan(TrangThaiTaiKhoan.SUSPENDED);
        HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(DRIVER_PROFILE_ID, actor);
        PhuongTien vehicle = SharedRouteMother.activeMotorbike(VEHICLE_ID, actor, 1);
        givenExistingContext(actor, profile, vehicle);

        BusinessException exception = catchThrowableOfType(
                () -> sut.createSharedRoute(ACTOR_ID, CreateSharedRouteRequestTestBuilder.aValidRequest().build()),
                BusinessException.class);

        assertBusinessError(exception, HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE");
        verifyNoInteractions(routePlanner, publicationCommitter);
    }

    @Test
    void givenInactiveDriverProfile_whenCreatingSharedRoute_thenRejectsBeforePlanner() {
        NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
        HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(DRIVER_PROFILE_ID, actor);
        profile.setTrangThaiTaiXe(TrangThaiTaiXe.SUSPENDED);
        PhuongTien vehicle = SharedRouteMother.activeMotorbike(VEHICLE_ID, actor, 1);
        givenExistingContext(actor, profile, vehicle);

        BusinessException exception = catchThrowableOfType(
                () -> sut.createSharedRoute(ACTOR_ID, CreateSharedRouteRequestTestBuilder.aValidRequest().build()),
                BusinessException.class);

        assertBusinessError(exception, HttpStatus.CONFLICT, "DRIVER_NOT_ACTIVE");
        verifyNoInteractions(routePlanner, publicationCommitter);
    }

    @Test
    void givenVehicleOwnedByAnotherUser_whenCreatingSharedRoute_thenRejectsBeforePlanner() {
        NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
        NguoiDung otherUser = SharedRouteMother.activeUser(2L);
        HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(DRIVER_PROFILE_ID, actor);
        PhuongTien vehicle = SharedRouteMother.activeMotorbike(VEHICLE_ID, otherUser, 1);
        givenExistingContext(actor, profile, vehicle);

        BusinessException exception = catchThrowableOfType(
                () -> sut.createSharedRoute(ACTOR_ID, CreateSharedRouteRequestTestBuilder.aValidRequest().build()),
                BusinessException.class);

        assertBusinessError(exception, HttpStatus.FORBIDDEN, "VEHICLE_NOT_AUTHORIZED");
        verifyNoInteractions(routePlanner, publicationCommitter);
    }

    @Test
    void givenOfferedSeatsExceedCapacity_whenCreatingSharedRoute_thenRejectsBeforePlanner() {
        NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
        HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(DRIVER_PROFILE_ID, actor);
        PhuongTien vehicle = SharedRouteMother.activeMotorbike(VEHICLE_ID, actor, 1);
        givenExistingContext(actor, profile, vehicle);
        CreateSharedRouteRequest request = CreateSharedRouteRequestTestBuilder.aValidRequest()
                .withOfferedSeats(2).build();

        BusinessException exception = catchThrowableOfType(
                () -> sut.createSharedRoute(ACTOR_ID, request), BusinessException.class);

        assertBusinessError(exception, HttpStatus.CONFLICT, "SEAT_COUNT_EXCEEDS_VEHICLE_CAPACITY");
        verifyNoInteractions(routePlanner, publicationCommitter);
    }

    @ParameterizedTest
    @ValueSource(strings = { "2026-08-01T08:00:00Z", "2026-08-01T07:59:59Z" })
    void givenDepartureTimeNotAfterCurrentTime_whenCreatingSharedRoute_thenRejects(String departureTimeText) {
        NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
        HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(DRIVER_PROFILE_ID, actor);
        PhuongTien vehicle = SharedRouteMother.activeMotorbike(VEHICLE_ID, actor, 1);
        givenExistingContext(actor, profile, vehicle);
        CreateSharedRouteRequest request = CreateSharedRouteRequestTestBuilder.aValidRequest()
                .withDepartureTime(Instant.parse(departureTimeText)).build();

        BusinessException exception = catchThrowableOfType(
                () -> sut.createSharedRoute(ACTOR_ID, request), BusinessException.class);

        assertBusinessError(exception, HttpStatus.BAD_REQUEST, "DEPARTURE_TIME_MUST_BE_FUTURE");
        verifyNoInteractions(routePlanner, publicationCommitter);
    }

    @Test
    void givenSameOriginAndDestination_whenCreatingSharedRoute_thenRejectsBeforePlanner() {
        NguoiDung actor = SharedRouteMother.activeUser(ACTOR_ID);
        HoSoTaiXe profile = SharedRouteMother.activeDriverProfile(DRIVER_PROFILE_ID, actor);
        PhuongTien vehicle = SharedRouteMother.activeMotorbike(VEHICLE_ID, actor, 1);
        givenExistingContext(actor, profile, vehicle);
        var sameEndpoint = SharedRouteMother.endpoint("10.762622", "106.660172", "Cùng một điểm");
        CreateSharedRouteRequest request = CreateSharedRouteRequestTestBuilder.aValidRequest()
                .withOrigin(sameEndpoint).withDestination(sameEndpoint).build();

        BusinessException exception = catchThrowableOfType(
                () -> sut.createSharedRoute(ACTOR_ID, request), BusinessException.class);

        assertBusinessError(exception, HttpStatus.BAD_REQUEST, "ROUTE_ENDPOINTS_MUST_BE_DIFFERENT");
        verify(routePlanner, never()).plan(any());
        verifyNoInteractions(publicationCommitter);
    }

    private void givenExistingContext(NguoiDung actor, HoSoTaiXe profile, PhuongTien vehicle) {
        when(nguoiDungRepository.findById(ACTOR_ID)).thenReturn(Optional.of(actor));
        when(hoSoTaiXeRepository.findByUserIdForRouteCreation(ACTOR_ID)).thenReturn(Optional.of(profile));
        when(phuongTienRepository.findByIdForRouteCreation(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
    }

    private static void assertBusinessError(BusinessException exception, HttpStatus expectedStatus, String expectedCode) {
        assertThat(exception).isNotNull();
        assertThat(exception.getStatus()).isEqualTo(expectedStatus);
        assertThat(exception.getCode()).isEqualTo(expectedCode);
    }
}
