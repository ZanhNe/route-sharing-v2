package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.CauHinhNghiepVu;
import com.zanh.route_sharing.domain.entity.DongXe;
import com.zanh.route_sharing.domain.entity.HangXe;
import com.zanh.route_sharing.domain.entity.HoSoSinhVien;
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
        public static final String VEHICLE_PLATE = "59A1-SEED01";
        public static final String SCHOOL_CODE = "SEED-UNIVERSITY";

        private static final String CREATE_ROUTE_PERMISSION = "CREATE_SHARED_ROUTE";
        private static final String SEARCH_ROUTE_PERMISSION = "SEARCH_SHARED_ROUTE";
        private static final String CREATE_RIDE_REQUEST_PERMISSION = "CREATE_RIDE_REQUEST";
        private static final String VIEW_ROUTE_RIDE_REQUESTS_PERMISSION = "VIEW_ROUTE_RIDE_REQUESTS";
        private static final String RESPOND_RIDE_REQUEST_PERMISSION = "RESPOND_RIDE_REQUEST";
        private static final String DRIVER_GROUP = "DRIVER";
        private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
        private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

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
        private final LoTrinhChiaSeSeedRepository sharedRouteRepository;
        private final PasswordEncoder passwordEncoder;
        private final Clock clock;

        @Transactional
        public SeedSummary seedSharedRouteScenario() {
                Instant now = clock.instant();
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

                NhomQuyen driverGroup = ensureDriverGroup(
                                createPermission,
                                viewRouteRideRequestsPermission,
                                respondRideRequestPermission);
                NguoiDung driver = ensureDriverUser(driverGroup, now);
                HoSoTaiXe driverProfile = ensureDriverProfile(driver, now);
                DongXe vehicleModel = ensureVehicleModel();
                PhuongTien vehicle = ensureVehicle(driver, vehicleModel, now);

                NhaTruong school = ensureSchool();
                CauHinhNghiepVu configuration = ensureBusinessConfiguration(school);
                NguoiDung passenger = ensurePassengerUser(
                                searchPermission,
                                createRideRequestPermission,
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
                return userRepository.save(user);
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
                configuration.setThoiGianMatTinHieuGiay(120L);
                configuration.setDoLechThoiGianKhoiHanhPhut(30);
                configuration.setSoNgayLuuViTri(30);
                configuration.setSoNgayLuuNhatKy(90);
                configuration.setRequestTtlSeconds(900L);
                configuration.setBookingCutoffSeconds(900L);
                configuration.setRejectionCooldownSeconds(3600L);
                configuration.setBatBuocTepXacNhanChuXeKhiKhongChinhChu(false);
                return configurationRepository.save(configuration);
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
