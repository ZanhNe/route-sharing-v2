package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.utils.time.TimePolicy;
import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.DongXe;
import com.zanh.route_sharing.domain.entity.HangXe;
import com.zanh.route_sharing.domain.entity.HoSoSinhVien;
import com.zanh.route_sharing.domain.entity.HoSoNhanSu;
import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import com.zanh.route_sharing.domain.entity.LoTrinhChiaSe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.NhaTruong;
import com.zanh.route_sharing.domain.entity.NhomQuyen;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.domain.entity.QuyenHan;
import com.zanh.route_sharing.domain.enums.CoSoSuDungPhuongTien;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiHoSoThanhVien;
import com.zanh.route_sharing.domain.enums.TrangThaiCongTac;
import com.zanh.route_sharing.domain.enums.TrangThaiHocTap;
import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.domain.enums.TrangThaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiXe;
import com.zanh.route_sharing.repository.HoSoTaiXeRepository;
import com.zanh.route_sharing.repository.NguoiDungRepository;
import com.zanh.route_sharing.repository.PhuongTienRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@Profile("seed & !prod")
@RequiredArgsConstructor
public class DevSeedDataService {

        public static final String DRIVER_EMAIL = "driver1@university.test";
        public static final String DRIVER_PASSWORD = "Dev123!";
        public static final String PASSENGER_EMAIL = "passenger1@university.test";
        public static final String PASSENGER_PASSWORD = "Dev123!";
        public static final String PASSENGER2_EMAIL = "passenger2@university.test";
        public static final String PASSENGER2_PASSWORD = "Dev123!";
        public static final String PASSENGER3_EMAIL = "passenger3@university.test";
        public static final String PASSENGER3_PASSWORD = "Dev123!";
        public static final String NO_TRIP_VIEW_EMAIL = "member-no-trip-view@university.test";
        public static final String SAFETY_EMAIL = "safety1@university.test";
        public static final String SAFETY_PASSWORD = "Dev123!";
        public static final String SAFETY2_EMAIL = "safety2@university.test";
        public static final String SAFETY2_PASSWORD = "Dev123!";
        public static final String NO_TRIP_VIEW_PASSWORD = "Dev123!";
        public static final String VEHICLE_PLATE = "59A1-SEED01";
        public static final String SCHOOL_CODE = "SEED-UNIVERSITY";

        private static final String CREATE_ROUTE_PERMISSION = "CREATE_SHARED_ROUTE";
        private static final String SEARCH_ROUTE_PERMISSION = "SEARCH_SHARED_ROUTE";
        private static final String CREATE_RIDE_REQUEST_PERMISSION = "CREATE_RIDE_REQUEST";
        private static final String VIEW_ROUTE_RIDE_REQUESTS_PERMISSION = "VIEW_ROUTE_RIDE_REQUESTS";
        private static final String RESPOND_RIDE_REQUEST_PERMISSION = "RESPOND_RIDE_REQUEST";
        private static final String CANCEL_OWN_RIDE_REQUEST_PERMISSION = "CANCEL_OWN_RIDE_REQUEST";
        private static final String VIEW_OWN_RIDE_REQUESTS_PERMISSION = "VIEW_OWN_RIDE_REQUESTS";
        private static final String VIEW_OWN_SHARED_ROUTES_PERMISSION = "VIEW_OWN_SHARED_ROUTES";
        private static final String LEGACY_CANCEL_ROUTE_RIDE_REQUEST_PERMISSION = "CANCEL_ROUTE_RIDE_REQUEST";
        private static final String CANCEL_OWN_SHARED_ROUTE_PERMISSION = "CANCEL_OWN_SHARED_ROUTE";
        private static final String LOCK_OWN_SHARED_ROUTE_PERMISSION = "LOCK_OWN_SHARED_ROUTE";
        private static final String VIEW_OWN_TRIP_PERMISSION = "VIEW_OWN_TRIP";
        private static final String START_OWN_TRIP_PERMISSION = "START_OWN_TRIP";
        private static final String CANCEL_OWN_TRIP_PERMISSION = "CANCEL_OWN_TRIP";
        private static final String CONFIRM_OWN_TRIP_PICKUP_ARRIVAL_PERMISSION = "CONFIRM_OWN_TRIP_PICKUP_ARRIVAL";
        private static final String VIEW_OWN_BOARDING_CODE_PERMISSION = "VIEW_OWN_BOARDING_CODE";
        private static final String CONFIRM_OWN_TRIP_BOARDING_PERMISSION = "CONFIRM_OWN_TRIP_BOARDING";
        private static final String CONFIRM_OWN_TRIP_NO_SHOW_PERMISSION = "CONFIRM_OWN_TRIP_NO_SHOW";
        private static final String SUBMIT_OWN_TRIP_LOCATION_PERMISSION = "SUBMIT_OWN_TRIP_LOCATION";
        private static final String REPORT_OWN_TRIP_INCIDENT_PERMISSION = "REPORT_OWN_TRIP_INCIDENT";
        private static final String MANAGE_OWN_TRIP_SAFETY_INTERVENTION_PERMISSION = "MANAGE_OWN_TRIP_SAFETY_INTERVENTION";
        private static final String HANDLE_INCIDENT_PERMISSION = "HANDLE_INCIDENT";
        private static final String INTERVENE_TRIP_SAFETY_PERMISSION = "INTERVENE_TRIP_SAFETY";
        private static final String VIEW_SAFETY_INVESTIGATION_EVIDENCE_PERMISSION = "VIEW_SAFETY_INVESTIGATION_EVIDENCE";
        private static final String REASSIGN_INCIDENT_PERMISSION = "REASSIGN_INCIDENT";
        private static final String DRIVER_GROUP = "DRIVER";
        private static final ZoneId BUSINESS_ZONE = TimePolicy.BUSINESS_ZONE;
        private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(),
                        Wgs84Coordinates.SRID);

        private static final String SAME_DESTINATION_ORIGIN_ADDRESS = "E1-02 Seed - Cùng điểm đến - Xuất phát";
        private static final String SEGMENT_ORIGIN_ADDRESS = "E1-02 Seed - Trùng đoạn tuyến - Xuất phát";

        private final QuyenHanSeedRepository permissionRepository;
        private final NhomQuyenSeedRepository groupRepository;
        private final NguoiDungRepository userRepository;
        private final HoSoTaiXeRepository driverProfileRepository;
        private final HangXeSeedRepository brandRepository;
        private final DongXeSeedRepository modelRepository;
        private final PhuongTienRepository vehicleRepository;
        private final NhaTruongSeedRepository schoolRepository;
        private final CauHinhNghiepVuSeedRepository configurationRepository;
        private final HoSoSinhVienSeedRepository membershipRepository;
        private final HoSoNhanSuSeedRepository staffMembershipRepository;
        private final LoTrinhChiaSeSeedRepository sharedRouteRepository;
        private final PasswordEncoder passwordEncoder;
        private final Clock clock;

        @Transactional
        public SeedSummary seedSharedRouteScenario() {
                Instant now = TimePolicy.now(clock);
                LocalDate currentBusinessDate = LocalDate.ofInstant(now, BUSINESS_ZONE);

                QuyenHan createPermission = ensurePermission(
                                CREATE_ROUTE_PERMISSION,
                                "Đăng lộ trình chia sẻ",
                                "Cho phép tài xế tạo một lộ trình chia sẻ mới.");
                QuyenHan searchPermission = ensurePermission(
                                SEARCH_ROUTE_PERMISSION,
                                "Tìm lộ trình chia sẻ",
                                "Cho phép thành viên tìm các lộ trình chia sẻ phù hợp.");
                QuyenHan createRideRequestPermission = ensurePermission(
                                CREATE_RIDE_REQUEST_PERMISSION,
                                "Gửi yêu cầu đi chung",
                                "Cho phép hành khách gửi yêu cầu tham gia một lộ trình chia sẻ.");
                QuyenHan viewRouteRideRequestsPermission = ensurePermission(
                                VIEW_ROUTE_RIDE_REQUESTS_PERMISSION,
                                "Xem yêu cầu đi chung của lộ trình",
                                "Cho phép tài xế xem các yêu cầu PENDING thuộc lộ trình do mình đăng.");
                QuyenHan respondRideRequestPermission = ensurePermission(
                                RESPOND_RIDE_REQUEST_PERMISSION,
                                "Phản hồi yêu cầu đi chung",
                                "Cho phép tài xế chấp nhận hoặc từ chối yêu cầu PENDING thuộc lộ trình do mình đăng.");
                QuyenHan cancelOwnRideRequestPermission = ensurePermission(
                                CANCEL_OWN_RIDE_REQUEST_PERMISSION,
                                "Hủy yêu cầu đi chung của mình",
                                "Cho phép hành khách hủy yêu cầu hoặc booking của chính mình.");
                QuyenHan viewOwnRideRequestsPermission = ensurePermission(
                                VIEW_OWN_RIDE_REQUESTS_PERMISSION,
                                "Xem yêu cầu đi chung của mình",
                                "Cho phép hành khách xem danh sách và chi tiết yêu cầu đi chung của chính mình.");
                QuyenHan cancelOwnSharedRoutePermission = ensurePermission(
                                CANCEL_OWN_SHARED_ROUTE_PERMISSION,
                                "Hủy lộ trình chia sẻ của mình",
                                "Cho phép tài xế hủy lộ trình OPEN do mình đăng và kết thúc booking liên quan.");
                QuyenHan viewOwnSharedRoutesPermission = ensurePermission(
                                VIEW_OWN_SHARED_ROUTES_PERMISSION,
                                "Xem lộ trình chia sẻ của mình",
                                "Cho phép tài xế xem danh sách và chi tiết lộ trình chia sẻ do chính mình đăng.");
                QuyenHan lockOwnSharedRoutePermission = ensurePermission(
                                LOCK_OWN_SHARED_ROUTE_PERMISSION,
                                "Khóa danh sách và hình thành chuyến đi",
                                "Cho phép tài xế khóa lộ trình OPEN của mình và hình thành chuyến đi thực tế.");
                QuyenHan viewOwnTripPermission = ensurePermission(
                                VIEW_OWN_TRIP_PERMISSION,
                                "Xem chuyến đi mình tham gia",
                                "Cho phép participant hợp lệ xem chi tiết kế hoạch chuyến đi của mình.");
                QuyenHan startOwnTripPermission = ensurePermission(
                                START_OWN_TRIP_PERMISSION,
                                "Bắt đầu chuyến đi của mình",
                                "Cho phép tài xế bắt đầu chuyến PREPARING do chính mình sở hữu khi ở đúng điểm DRIVER_START.");
                QuyenHan cancelOwnTripPermission = ensurePermission(
                                CANCEL_OWN_TRIP_PERMISSION,
                                "Hủy chuyến đã hình thành trước khi bắt đầu",
                                "Cho phép tài xế hủy toàn bộ chuyến PREPARING do chính mình sở hữu trước Start.");
                QuyenHan confirmOwnTripPickupArrivalPermission = ensurePermission(
                                CONFIRM_OWN_TRIP_PICKUP_ARRIVAL_PERMISSION,
                                "Xác nhận đã đến điểm đón",
                                "Cho phép tài xế của chuyến IN_PROGRESS xác nhận đã đến pickup kế tiếp hợp lệ.");
                QuyenHan viewOwnBoardingCodePermission = ensurePermission(
                                VIEW_OWN_BOARDING_CODE_PERMISSION,
                                "Xem boarding code của booking mình",
                                "Cho phép Passenger xem cùng boarding code active của pickup hiện tại thuộc chính mình.");
                QuyenHan confirmOwnTripBoardingPermission = ensurePermission(
                                CONFIRM_OWN_TRIP_BOARDING_PERMISSION,
                                "Xác nhận Passenger lên xe",
                                "Cho phép Driver của chuyến xác nhận Boarding bằng code cho pickup hiện tại.");
                QuyenHan confirmOwnTripNoShowPermission = ensurePermission(
                                CONFIRM_OWN_TRIP_NO_SHOW_PERMISSION,
                                "Xác nhận Passenger no-show",
                                "Cho phép Driver của chuyến xác nhận Passenger hiện tại no-show sau waiting deadline.");
                QuyenHan submitOwnTripLocationPermission = ensurePermission(
                                SUBMIT_OWN_TRIP_LOCATION_PERMISSION,
                                "Gửi vị trí chuyến đi của mình",
                                "Cho phép Driver gửi browser geolocation định kỳ cho chuyến đang vận hành của chính mình.");
                QuyenHan reportOwnTripIncidentPermission = ensurePermission(
                                REPORT_OWN_TRIP_INCIDENT_PERMISSION,
                                "Báo sự cố/SOS trên chuyến mình tham gia",
                                "Cho phép Driver hoặc active Passenger báo sự cố/SOS cho chuyến đang vận hành mà mình tham gia.");
                QuyenHan manageOwnTripSafetyInterventionPermission = ensurePermission(
                                MANAGE_OWN_TRIP_SAFETY_INTERVENTION_PERMISSION,
                                "Xử lý can thiệp an toàn chuyến của mình",
                                "Cho phép Driver xác nhận safe-exit hoặc escalates active Safety hold của chính Trip mình.");
                QuyenHan handleIncidentPermission = ensurePermission(
                                HANDLE_INCIDENT_PERMISSION,
                                "Tiếp nhận safety incident",
                                "Cho phép nhân sự Safety trong đúng phạm vi trường xem và xử lý incident/SOS.");
                QuyenHan interveneTripSafetyPermission = ensurePermission(
                                INTERVENE_TRIP_SAFETY_PERMISSION,
                                "Can thiệp an toàn vào Trip",
                                "Cho phép current Safety handler emergency-abort Trip trong đúng phạm vi trường.");
                QuyenHan viewSafetyEvidencePermission = ensurePermission(
                                VIEW_SAFETY_INVESTIGATION_EVIDENCE_PERMISSION,
                                "Xem bằng chứng điều tra Safety",
                                "Cho phép người phụ trách incident xem route/location/history nhạy cảm phục vụ điều tra.");
                QuyenHan reassignIncidentPermission = ensurePermission(
                                REASSIGN_INCIDENT_PERMISSION,
                                "Chuyển người xử lý Safety incident",
                                "Cho phép nhân sự được ủy quyền chuyển primary handler của incident.");

                NhomQuyen driverGroup = ensureDriverGroup(
                                createPermission,
                                viewRouteRideRequestsPermission,
                                respondRideRequestPermission,
                                cancelOwnSharedRoutePermission,
                                viewOwnSharedRoutesPermission,
                                lockOwnSharedRoutePermission,
                                viewOwnTripPermission,
                                startOwnTripPermission,
                                cancelOwnTripPermission,
                                confirmOwnTripPickupArrivalPermission,
                                confirmOwnTripBoardingPermission,
                                confirmOwnTripNoShowPermission,
                                submitOwnTripLocationPermission,
                                reportOwnTripIncidentPermission,
                                manageOwnTripSafetyInterventionPermission);
                retireLegacyPermission(driverGroup, LEGACY_CANCEL_ROUTE_RIDE_REQUEST_PERMISSION);
                NguoiDung driver = ensureDriverUser(driverGroup, now);
                HoSoTaiXe driverProfile = ensureDriverProfile(driver, now);
                DongXe vehicleModel = ensureVehicleModel();
                PhuongTien vehicle = ensureVehicle(driver, vehicleModel, now);

                NhaTruong school = ensureSchool();
                CauHinhNghiepVu configuration = ensureBusinessConfiguration(school);
                NguoiDung passenger = ensurePassengerUser(
                                searchPermission,
                                createRideRequestPermission,
                                cancelOwnRideRequestPermission,
                                viewOwnRideRequestsPermission,
                                viewOwnTripPermission,
                                viewOwnBoardingCodePermission,
                                now);
                NguoiDung passenger2 = ensureAdditionalPassengerUser(
                                PASSENGER2_EMAIL,
                                PASSENGER2_PASSWORD,
                                "Hành khách Seed E4-S01 B",
                                searchPermission,
                                createRideRequestPermission,
                                cancelOwnRideRequestPermission,
                                viewOwnRideRequestsPermission,
                                viewOwnTripPermission,
                                viewOwnBoardingCodePermission,
                                now);
                NguoiDung passenger3 = ensureAdditionalPassengerUser(
                                PASSENGER3_EMAIL,
                                PASSENGER3_PASSWORD,
                                "Hành khách Seed E4-S01 Foreign",
                                searchPermission,
                                createRideRequestPermission,
                                cancelOwnRideRequestPermission,
                                viewOwnRideRequestsPermission,
                                viewOwnTripPermission,
                                viewOwnBoardingCodePermission,
                                now);
                grantDirectPermission(passenger, reportOwnTripIncidentPermission);
                grantDirectPermission(passenger2, reportOwnTripIncidentPermission);
                grantDirectPermission(passenger3, reportOwnTripIncidentPermission);
                passenger = userRepository.save(passenger);
                passenger2 = userRepository.save(passenger2);
                passenger3 = userRepository.save(passenger3);
                NguoiDung safety = ensureSafetyUser(
                                SAFETY_EMAIL, SAFETY_PASSWORD, "Nhân sự Safety Seed E6-05 Lead", now,
                                handleIncidentPermission, viewSafetyEvidencePermission, reassignIncidentPermission,
                                interveneTripSafetyPermission);
                NguoiDung safety2 = ensureSafetyUser(
                                SAFETY2_EMAIL, SAFETY2_PASSWORD, "Nhân sự Safety Seed E6-05 Handler", now,
                                handleIncidentPermission, viewSafetyEvidencePermission, interveneTripSafetyPermission);
                ensureLoginOnlyUser(
                                NO_TRIP_VIEW_EMAIL,
                                NO_TRIP_VIEW_PASSWORD,
                                "Thành viên Seed không có VIEW_OWN_TRIP",
                                now);

                HoSoSinhVien passengerMembership = ensureMembership(
                                passenger,
                                school,
                                "SEED-PASSENGER-001",
                                currentBusinessDate,
                                now);
                HoSoSinhVien driverMembership = ensureMembership(
                                driver,
                                school,
                                "SEED-DRIVER-001",
                                currentBusinessDate,
                                now);
                ensureMembership(
                                passenger2,
                                school,
                                "SEED-PASSENGER-002",
                                currentBusinessDate,
                                now);
                ensureMembership(
                                passenger3,
                                school,
                                "SEED-PASSENGER-003",
                                currentBusinessDate,
                                now);
                ensureStaffMembership(
                                safety,
                                school,
                                "SEED-SAFETY-001",
                                currentBusinessDate,
                                now);
                ensureStaffMembership(
                                safety2,
                                school,
                                "SEED-SAFETY-002",
                                currentBusinessDate,
                                now);

                LoTrinhChiaSe sameDestinationRoute = ensureRoute(
                                driver,
                                vehicle,
                                SAME_DESTINATION_ORIGIN_ADDRESS,
                                "E1-02 Seed - Cùng điểm đến - Đích tài xế",
                                line(
                                                106.6800, 10.7700,
                                                106.7000, 10.7700,
                                                106.7200, 10.7700),
                                point(106.6800, 10.7700),
                                point(106.7200, 10.7700),
                                now.plusSeconds(7_200));

                LoTrinhChiaSe segmentRoute = ensureRoute(
                                driver,
                                vehicle,
                                SEGMENT_ORIGIN_ADDRESS,
                                "E1-02 Seed - Trùng đoạn tuyến - Đích tài xế",
                                line(
                                                106.6800, 10.7800,
                                                106.7050, 10.7800,
                                                106.7300, 10.7800),
                                point(106.6800, 10.7800),
                                point(106.7300, 10.7800),
                                now.plusSeconds(10_800));

                return new SeedSummary(
                                school.getId(),
                                configuration.getId(),
                                passenger.getId(),
                                passengerMembership.getId(),
                                driver.getId(),
                                driverProfile.getId(),
                                driverMembership.getId(),
                                vehicle.getId(),
                                sameDestinationRoute.getId(),
                                segmentRoute.getId(),
                                sameDestinationRoute.getThoiGianKhoiHanhDuKien(),
                                segmentRoute.getThoiGianKhoiHanhDuKien());
        }

        private QuyenHan ensurePermission(
                        String code,
                        String name,
                        String description) {
                QuyenHan permission = permissionRepository.findByMaQuyenIgnoreCase(code)
                                .orElseGet(() -> QuyenHan.builder()
                                                .maQuyen(code)
                                                .build());

                permission.setTenQuyen(name);
                permission.setMoTa(description);
                permission.setNhomChucNang("SHARED_ROUTE");
                permission.setDangHoatDong(true);
                return permissionRepository.save(permission);
        }

        private NhomQuyen ensureDriverGroup(QuyenHan... permissions) {
                NhomQuyen group = groupRepository.findByMaNhomIgnoreCase(DRIVER_GROUP)
                                .orElseGet(() -> NhomQuyen.builder()
                                                .maNhom(DRIVER_GROUP)
                                                .tenNhom("Tài xế")
                                                .moTa("Nhóm quyền cho tài xế đã được phê duyệt.")
                                                .dangHoatDong(true)
                                                .build());

                group.setDangHoatDong(true);
                for (QuyenHan permission : permissions) {
                        if (group.getDanhSachQuyenHan().stream()
                                        .noneMatch(item -> item.getMaQuyen()
                                                        .equalsIgnoreCase(permission.getMaQuyen()))) {
                                group.getDanhSachQuyenHan().add(permission);
                        }
                }
                return groupRepository.save(group);
        }

        private void retireLegacyPermission(NhomQuyen group, String permissionCode) {
                boolean removed = group.getDanhSachQuyenHan()
                                .removeIf(permission -> permission.getMaQuyen().equalsIgnoreCase(permissionCode));
                if (removed) {
                        groupRepository.save(group);
                }

                permissionRepository.findByMaQuyenIgnoreCase(permissionCode)
                                .ifPresent(permission -> {
                                        if (Boolean.TRUE.equals(permission.getDangHoatDong())) {
                                                permission.setDangHoatDong(false);
                                                permissionRepository.save(permission);
                                        }
                                });
        }

        private NguoiDung ensureDriverUser(
                        NhomQuyen driverGroup,
                        Instant now) {
                NguoiDung user = userRepository.findByEmailTruongIgnoreCase(DRIVER_EMAIL)
                                .orElseGet(() -> NguoiDung.builder()
                                                .hoTen("Tài xế Seed E1")
                                                .emailTruong(DRIVER_EMAIL)
                                                .build());

                normalizeSeedUser(user, DRIVER_PASSWORD, now);
                if (user.getDanhSachNhomQuyen().stream()
                                .noneMatch(item -> item.getMaNhom()
                                                .equalsIgnoreCase(driverGroup.getMaNhom()))) {
                        user.getDanhSachNhomQuyen().add(driverGroup);
                }
                return userRepository.save(user);
        }

        private NguoiDung ensurePassengerUser(
                        QuyenHan searchPermission,
                        QuyenHan createRideRequestPermission,
                        QuyenHan cancelOwnRideRequestPermission,
                        QuyenHan viewOwnRideRequestsPermission,
                        QuyenHan viewOwnTripPermission,
                        QuyenHan viewOwnBoardingCodePermission,
                        Instant now) {
                NguoiDung user = userRepository.findByEmailTruongIgnoreCase(PASSENGER_EMAIL)
                                .orElseGet(() -> NguoiDung.builder()
                                                .hoTen("Hành khách Seed E1-02")
                                                .emailTruong(PASSENGER_EMAIL)
                                                .build());

                normalizeSeedUser(user, PASSENGER_PASSWORD, now);
                if (user.getDanhSachQuyenTrucTiep().stream()
                                .noneMatch(item -> item.getMaQuyen()
                                                .equalsIgnoreCase(searchPermission.getMaQuyen()))) {
                        user.getDanhSachQuyenTrucTiep().add(searchPermission);
                }
                if (user.getDanhSachQuyenTrucTiep().stream()
                                .noneMatch(item -> item.getMaQuyen()
                                                .equalsIgnoreCase(createRideRequestPermission.getMaQuyen()))) {
                        user.getDanhSachQuyenTrucTiep().add(createRideRequestPermission);
                }
                if (user.getDanhSachQuyenTrucTiep().stream()
                                .noneMatch(item -> item.getMaQuyen()
                                                .equalsIgnoreCase(cancelOwnRideRequestPermission.getMaQuyen()))) {
                        user.getDanhSachQuyenTrucTiep().add(cancelOwnRideRequestPermission);
                }
                if (user.getDanhSachQuyenTrucTiep().stream()
                                .noneMatch(item -> item.getMaQuyen()
                                                .equalsIgnoreCase(viewOwnRideRequestsPermission.getMaQuyen()))) {
                        user.getDanhSachQuyenTrucTiep().add(viewOwnRideRequestsPermission);
                }
                if (user.getDanhSachQuyenTrucTiep().stream()
                                .noneMatch(item -> item.getMaQuyen()
                                                .equalsIgnoreCase(viewOwnTripPermission.getMaQuyen()))) {
                        user.getDanhSachQuyenTrucTiep().add(viewOwnTripPermission);
                }
                grantDirectPermission(user, viewOwnBoardingCodePermission);
                return userRepository.save(user);
        }

        private NguoiDung ensureAdditionalPassengerUser(
                        String email,
                        String password,
                        String fullName,
                        QuyenHan searchPermission,
                        QuyenHan createRideRequestPermission,
                        QuyenHan cancelOwnRideRequestPermission,
                        QuyenHan viewOwnRideRequestsPermission,
                        QuyenHan viewOwnTripPermission,
                        QuyenHan viewOwnBoardingCodePermission,
                        Instant now) {
                NguoiDung user = userRepository.findByEmailTruongIgnoreCase(email)
                                .orElseGet(() -> NguoiDung.builder()
                                                .hoTen(fullName)
                                                .emailTruong(email)
                                                .build());

                normalizeSeedUser(user, password, now);
                grantDirectPermission(user, searchPermission);
                grantDirectPermission(user, createRideRequestPermission);
                grantDirectPermission(user, cancelOwnRideRequestPermission);
                grantDirectPermission(user, viewOwnRideRequestsPermission);
                grantDirectPermission(user, viewOwnTripPermission);
                grantDirectPermission(user, viewOwnBoardingCodePermission);
                return userRepository.save(user);
        }

        private NguoiDung ensureSafetyUser(
                        String email,
                        String password,
                        String fullName,
                        Instant now,
                        QuyenHan... permissions) {
                NguoiDung user = userRepository.findByEmailTruongIgnoreCase(email)
                                .orElseGet(() -> NguoiDung.builder()
                                                .hoTen(fullName)
                                                .emailTruong(email)
                                                .build());
                normalizeSeedUser(user, password, now);
                for (QuyenHan permission : permissions) {
                        grantDirectPermission(user, permission);
                }
                return userRepository.save(user);
        }

        private NguoiDung ensureLoginOnlyUser(
                        String email,
                        String password,
                        String fullName,
                        Instant now) {
                NguoiDung user = userRepository.findByEmailTruongIgnoreCase(email)
                                .orElseGet(() -> NguoiDung.builder()
                                                .hoTen(fullName)
                                                .emailTruong(email)
                                                .build());
                normalizeSeedUser(user, password, now);
                return userRepository.save(user);
        }

        private void grantDirectPermission(NguoiDung user, QuyenHan permission) {
                if (user.getDanhSachQuyenTrucTiep().stream()
                                .noneMatch(item -> item.getMaQuyen()
                                                .equalsIgnoreCase(permission.getMaQuyen()))) {
                        user.getDanhSachQuyenTrucTiep().add(permission);
                }
        }

        private void normalizeSeedUser(
                        NguoiDung user,
                        String rawPassword,
                        Instant now) {
                user.setTrangThaiTaiKhoan(TrangThaiTaiKhoan.ACTIVE);
                if (user.getEmailDaXacThucLuc() == null) {
                        user.setEmailDaXacThucLuc(now);
                }
                if (user.getMatKhauDaMaHoa() == null
                                || user.getMatKhauDaMaHoa().isBlank()
                                || !passwordEncoder.matches(rawPassword, user.getMatKhauDaMaHoa())) {
                        user.setMatKhauDaMaHoa(passwordEncoder.encode(rawPassword));
                }
        }

        private HoSoTaiXe ensureDriverProfile(
                        NguoiDung driver,
                        Instant now) {
                HoSoTaiXe profile = driverProfileRepository
                                .findByUserIdForRouteCreation(driver.getId())
                                .orElseGet(() -> HoSoTaiXe.builder()
                                                .nguoiDung(driver)
                                                .ngayDangKy(now)
                                                .build());

                profile.setTrangThaiTaiXe(TrangThaiTaiXe.ACTIVE);
                if (profile.getNgayDuocDuyet() == null) {
                        profile.setNgayDuocDuyet(now);
                }
                return driverProfileRepository.save(profile);
        }

        private DongXe ensureVehicleModel() {
                HangXe brand = brandRepository.findByMaHangIgnoreCase("HONDA")
                                .orElseGet(() -> HangXe.builder()
                                                .maHang("HONDA")
                                                .build());
                brand.setTenHang("Honda");
                brand.setDangHoatDong(true);
                HangXe brand2 = brandRepository.save(brand);

                DongXe model = modelRepository
                                .findByHangXe_IdAndTenDongXeIgnoreCase(
                                                brand2.getId(),
                                                "Air Blade Seed")
                                .orElseGet(() -> DongXe.builder()
                                                .hangXe(brand2)
                                                .tenDongXe("Air Blade Seed")
                                                .build());

                model.setHangXe(brand2);
                model.setTenDongXe("Air Blade Seed");
                model.setLoaiPhuongTien(LoaiPhuongTien.XE_MAY);
                model.setSoChoHanhKhachMacDinh(2);
                model.setDangHoatDong(true);
                return modelRepository.save(model);
        }

        private PhuongTien ensureVehicle(
                        NguoiDung driver,
                        DongXe vehicleModel,
                        Instant now) {
                PhuongTien vehicle = vehicleRepository
                                .findByBienSoXeIgnoreCase(VEHICLE_PLATE)
                                .orElseGet(() -> PhuongTien.builder()
                                                .bienSoXe(VEHICLE_PLATE)
                                                .build());

                vehicle.setMauSacThucTe("Đen");
                vehicle.setSoChoHanhKhachDuocDuyet(2);
                vehicle.setCoSoSuDung(CoSoSuDungPhuongTien.CHINH_CHU);
                vehicle.setDaCamKetDuocChuXeChoPhep(false);
                vehicle.setTrangThaiPhuongTien(TrangThaiPhuongTien.ACTIVE);
                if (vehicle.getNgayDuocDuyet() == null) {
                        vehicle.setNgayDuocDuyet(now);
                }
                vehicle.setNguoiDangKySuDung(driver);
                vehicle.setDongXe(vehicleModel);
                return vehicleRepository.save(vehicle);
        }

        private NhaTruong ensureSchool() {
                NhaTruong school = schoolRepository.findByMaTruongIgnoreCase(SCHOOL_CODE)
                                .orElseGet(() -> NhaTruong.builder()
                                                .maTruong(SCHOOL_CODE)
                                                .build());

                school.setTenTruong("Trường Đại học Seed Route Sharing");
                school.setTenVietTat("SEED-U");
                school.setDiaChi("Địa chỉ trường seed");
                school.setDangHoatDong(true);
                school.getTenMienEmailChoPhep().clear();
                school.getTenMienEmailChoPhep().add("university.test");
                return schoolRepository.save(school);
        }

        private CauHinhNghiepVu ensureBusinessConfiguration(NhaTruong school) {
                CauHinhNghiepVu configuration = configurationRepository
                                .findByNhaTruong_Id(school.getId())
                                .orElseGet(() -> CauHinhNghiepVu.builder()
                                                .nhaTruong(school)
                                                .build());

                configuration.setNhaTruong(school);
                configuration.setBanKinhCungDiemDenMet(new BigDecimal("200.00"));
                configuration.setBanKinhDiemDenGanTuyenMet(new BigDecimal("150.00"));
                configuration.setTyLeTienDuongToiThieu(new BigDecimal("60.00"));
                configuration.setKhoangCachLechDonToiDaMet(new BigDecimal("150.00"));
                configuration.setThoiGianLechDonToiDaGiay(900L);
                configuration.setBanKinhXacDinhDaDenMet(new BigDecimal("50.00"));
                configuration.setThoiGianChoKhachGiay(300L);
                configuration.setThoiGianTreTinHieuGiay(15L);
                configuration.setThoiGianMatTinHieuGiay(30L);
                configuration.setDoLechThoiGianKhoiHanhPhut(30);
                configuration.setSoNgayLuuViTri(30);
                configuration.setChuKyGuiViTriGiay(5L);
                configuration.setSoNgayLuuNhatKy(90);
                configuration.setBookingCutoffSeconds(900L);
                configuration.setRejectionCooldownSeconds(3600L);
                configuration.setBatBuocTepXacNhanChuXeKhiKhongChinhChu(false);
                return configurationRepository.save(configuration);
        }

        private HoSoNhanSu ensureStaffMembership(
                        NguoiDung user,
                        NhaTruong school,
                        String internalCode,
                        LocalDate currentBusinessDate,
                        Instant now) {
                HoSoNhanSu membership = staffMembershipRepository
                                .findFirstByNguoiDung_IdAndNhaTruong_Id(user.getId(), school.getId())
                                .orElseGet(() -> HoSoNhanSu.builder()
                                                .nguoiDung(user)
                                                .nhaTruong(school)
                                                .maDinhDanhNoiBo(internalCode)
                                                .build());
                membership.setNguoiDung(user);
                membership.setNhaTruong(school);
                membership.setMaDinhDanhNoiBo(internalCode);
                membership.setTrangThaiHoSo(TrangThaiHoSoThanhVien.APPROVED);
                membership.setNgayBatDauHieuLuc(currentBusinessDate.minusYears(1));
                membership.setNgayKetThucHieuLuc(currentBusinessDate.plusYears(1));
                membership.setNgayDuocDuyet(now);
                membership.setNgayBatDauCongTac(currentBusinessDate.minusYears(1));
                membership.setNgayKetThucCongTac(null);
                membership.setTrangThaiCongTac(TrangThaiCongTac.DANG_CONG_TAC);
                return staffMembershipRepository.save(membership);
        }

        private HoSoSinhVien ensureMembership(
                        NguoiDung user,
                        NhaTruong school,
                        String internalCode,
                        LocalDate currentBusinessDate,
                        Instant now) {
                HoSoSinhVien membership = membershipRepository
                                .findFirstByNguoiDung_IdAndNhaTruong_Id(
                                                user.getId(),
                                                school.getId())
                                .orElseGet(() -> HoSoSinhVien.builder()
                                                .nguoiDung(user)
                                                .nhaTruong(school)
                                                .maDinhDanhNoiBo(internalCode)
                                                .build());

                membership.setNguoiDung(user);
                membership.setNhaTruong(school);
                membership.setMaDinhDanhNoiBo(internalCode);
                membership.setTrangThaiHoSo(TrangThaiHoSoThanhVien.APPROVED);
                membership.setNgayBatDauHieuLuc(currentBusinessDate.minusYears(1));
                membership.setNgayKetThucHieuLuc(currentBusinessDate.plusYears(1));
                membership.setNgayDuocDuyet(now);
                membership.setNgayNhapHoc(currentBusinessDate.minusYears(1));
                membership.setTrangThaiHocTap(TrangThaiHocTap.DANG_HOC);
                return membershipRepository.save(membership);
        }

        private LoTrinhChiaSe ensureRoute(
                        NguoiDung driver,
                        PhuongTien vehicle,
                        String originAddress,
                        String destinationAddress,
                        LineString routeLine,
                        Point origin,
                        Point driverDestination,
                        Instant departureTime) {
                LoTrinhChiaSe route = sharedRouteRepository
                                .findFirstByTaiXe_IdAndDiaChiXuatPhat(
                                                driver.getId(),
                                                originAddress)
                                .orElse(null);

                if (route == null) {
                        route = LoTrinhChiaSe.open(
                                        driver,
                                        vehicle,
                                        origin,
                                        originAddress,
                                        driverDestination,
                                        destinationAddress,
                                        routeLine,
                                        new BigDecimal("4500.00"),
                                        900L,
                                        departureTime,
                                        2,
                                        new BigDecimal("3000.00"));
                } else {
                        route.setTaiXe(driver);
                        route.setPhuongTien(vehicle);
                        route.setDiemXuatPhat(origin);
                        route.setDiaChiXuatPhat(originAddress);
                        route.setDiemDichTaiXe(driverDestination);
                        route.setDiaChiDichTaiXe(destinationAddress);
                        route.setTuyenDuongGoc(routeLine);
                        route.setKhoangCachDuKienMet(new BigDecimal("4500.00"));
                        route.setThoiLuongDuKienGiay(900L);
                        route.setThoiGianKhoiHanhDuKien(departureTime);
                        route.setSoGheCungCap(2);
                        route.setSoGheConLai(2);
                        route.setMucHoTroGoiYMoiKm(new BigDecimal("3000.00"));
                        route.setTrangThaiLoTrinh(TrangThaiLoTrinh.OPEN);
                        route.setChotDanhSachLuc(null);
                        route.setHuyLuc(null);
                        route.setLyDoHuy(null);
                }
                return sharedRouteRepository.save(route);
        }

        private static Point point(double longitude, double latitude) {
                return GEOMETRY_FACTORY.createPoint(
                                new Coordinate(longitude, latitude));
        }

        private static LineString line(double... longitudeLatitudePairs) {
                Coordinate[] coordinates = new Coordinate[longitudeLatitudePairs.length / 2];
                for (int index = 0; index < longitudeLatitudePairs.length; index += 2) {
                        coordinates[index / 2] = new Coordinate(
                                        longitudeLatitudePairs[index],
                                        longitudeLatitudePairs[index + 1]);
                }
                return GEOMETRY_FACTORY.createLineString(coordinates);
        }

        public record SeedSummary(
                        Long schoolId,
                        Long configurationId,
                        Long passengerUserId,
                        Long passengerMembershipId,
                        Long driverUserId,
                        Long driverProfileId,
                        Long driverMembershipId,
                        Long vehicleId,
                        Long sameDestinationRouteId,
                        Long segmentRouteId,
                        Instant sameDestinationDepartureTime,
                        Instant segmentDepartureTime) {
        }
}
