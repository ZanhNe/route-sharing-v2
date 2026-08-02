package com.zanh.route_sharing.devseed;

import com.zanh.route_sharing.domain.entity.DongXe;
import com.zanh.route_sharing.domain.entity.HangXe;
import com.zanh.route_sharing.domain.entity.HoSoTaiXe;
import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.NhomQuyen;
import com.zanh.route_sharing.domain.entity.PhuongTien;
import com.zanh.route_sharing.domain.entity.QuyenHan;
import com.zanh.route_sharing.domain.enums.CoSoSuDungPhuongTien;
import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiPhuongTien;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiXe;
import com.zanh.route_sharing.repository.HoSoTaiXeRepository;
import com.zanh.route_sharing.repository.NguoiDungRepository;
import com.zanh.route_sharing.repository.PhuongTienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Profile("seed & !prod")
@RequiredArgsConstructor
public class DevSeedDataService {
    public static final String DRIVER_EMAIL = "driver1@university.test";
    public static final String DRIVER_PASSWORD = "Dev123!";
    public static final String VEHICLE_PLATE = "59A1-SEED01";

    private final QuyenHanSeedRepository permissionRepository;
    private final NhomQuyenSeedRepository groupRepository;
    private final NguoiDungRepository userRepository;
    private final HoSoTaiXeRepository driverProfileRepository;
    private final HangXeSeedRepository brandRepository;
    private final DongXeSeedRepository modelRepository;
    private final PhuongTienRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public SeedSummary seedSharedRouteScenario() {
        Instant now = clock.instant();
        QuyenHan createRoutePermission = ensureCreateRoutePermission();
        NhomQuyen driverGroup = ensureDriverGroup(createRoutePermission);
        NguoiDung driver = ensureDriverUser(driverGroup, now);
        HoSoTaiXe driverProfile = ensureDriverProfile(driver, now);
        DongXe vehicleModel = ensureVehicleModel();
        PhuongTien vehicle = ensureVehicle(driver, vehicleModel, now);

        return new SeedSummary(driver.getId(), driverProfile.getId(), vehicle.getId());
    }

    private QuyenHan ensureCreateRoutePermission() {
        return permissionRepository.findByMaQuyenIgnoreCase("CREATE_SHARED_ROUTE")
                .map(permission -> {
                    boolean changed = false;
                    if (!Boolean.TRUE.equals(permission.getDangHoatDong())) {
                        permission.setDangHoatDong(true);
                        changed = true;
                    }
                    return changed ? permissionRepository.save(permission) : permission;
                })
                .orElseGet(() -> permissionRepository.save(QuyenHan.builder()
                        .maQuyen("CREATE_SHARED_ROUTE")
                        .tenQuyen("Đăng lộ trình chia sẻ")
                        .moTa("Cho phép tài xế tạo một lộ trình chia sẻ mới.")
                        .nhomChucNang("SHARED_ROUTE")
                        .dangHoatDong(true)
                        .build()));
    }

    private NhomQuyen ensureDriverGroup(QuyenHan permission) {
        NhomQuyen group = groupRepository.findByMaNhomIgnoreCase("DRIVER")
                .orElseGet(() -> NhomQuyen.builder()
                        .maNhom("DRIVER")
                        .tenNhom("Tài xế")
                        .moTa("Nhóm quyền cho tài xế đã được phê duyệt.")
                        .dangHoatDong(true)
                        .build());

        boolean changed = false;
        if (!Boolean.TRUE.equals(group.getDangHoatDong())) {
            group.setDangHoatDong(true);
            changed = true;
        }
        if (group.getDanhSachQuyenHan().stream()
                .noneMatch(item -> item.getMaQuyen().equalsIgnoreCase(permission.getMaQuyen()))) {
            group.getDanhSachQuyenHan().add(permission);
            changed = true;
        }
        return group.getId() == null || changed ? groupRepository.save(group) : group;
    }

    private NguoiDung ensureDriverUser(NhomQuyen driverGroup, Instant now) {
        NguoiDung user = userRepository.findByEmailTruongIgnoreCase(DRIVER_EMAIL)
                .orElseGet(() -> NguoiDung.builder()
                        .hoTen("Tài xế Seed E1")
                        .emailTruong(DRIVER_EMAIL)
                        .matKhauDaMaHoa(passwordEncoder.encode(DRIVER_PASSWORD))
                        .trangThaiTaiKhoan(TrangThaiTaiKhoan.ACTIVE)
                        .emailDaXacThucLuc(now)
                        .build());

        boolean changed = false;
        if (user.getTrangThaiTaiKhoan() != TrangThaiTaiKhoan.ACTIVE) {
            user.setTrangThaiTaiKhoan(TrangThaiTaiKhoan.ACTIVE);
            changed = true;
        }
        if (user.getEmailDaXacThucLuc() == null) {
            user.setEmailDaXacThucLuc(now);
            changed = true;
        }
        if (user.getMatKhauDaMaHoa() == null
                || user.getMatKhauDaMaHoa().isBlank()
                || !passwordEncoder.matches(DRIVER_PASSWORD, user.getMatKhauDaMaHoa())) {
            user.setMatKhauDaMaHoa(passwordEncoder.encode(DRIVER_PASSWORD));
            changed = true;
        }
        if (user.getDanhSachNhomQuyen().stream()
                .noneMatch(item -> item.getMaNhom().equalsIgnoreCase(driverGroup.getMaNhom()))) {
            user.getDanhSachNhomQuyen().add(driverGroup);
            changed = true;
        }
        return user.getId() == null || changed ? userRepository.save(user) : user;
    }

    private HoSoTaiXe ensureDriverProfile(NguoiDung driver, Instant now) {
        HoSoTaiXe profile = driverProfileRepository.findByUserIdForRouteCreation(driver.getId())
                .orElseGet(() -> HoSoTaiXe.builder()
                        .nguoiDung(driver)
                        .ngayDangKy(now)
                        .build());

        boolean changed = false;
        if (profile.getTrangThaiTaiXe() != TrangThaiTaiXe.ACTIVE) {
            profile.setTrangThaiTaiXe(TrangThaiTaiXe.ACTIVE);
            changed = true;
        }
        if (profile.getNgayDuocDuyet() == null) {
            profile.setNgayDuocDuyet(now);
            changed = true;
        }
        return profile.getId() == null || changed ? driverProfileRepository.save(profile) : profile;
    }

    private DongXe ensureVehicleModel() {
        HangXe brand = brandRepository.findByMaHangIgnoreCase("HONDA")
                .orElse(null);

        if (brand == null) {
            brand = HangXe.builder()
                    .maHang("HONDA")
                    .tenHang("Honda")
                    .dangHoatDong(true)
                    .build();
        } else {
            brand.setTenHang("Honda");
            brand.setDangHoatDong(true);
        }
        brand = brandRepository.save(brand);

        DongXe model = modelRepository
                .findByHangXe_IdAndTenDongXeIgnoreCase(
                        brand.getId(),
                        "Air Blade Seed")
                .orElse(null);

        if (model == null) {
            model = DongXe.builder()
                    .hangXe(brand)
                    .tenDongXe("Air Blade Seed")
                    .loaiPhuongTien(LoaiPhuongTien.XE_MAY)
                    .soChoHanhKhachMacDinh(1)
                    .dangHoatDong(true)
                    .build();
        } else {
            model.setHangXe(brand);
            model.setTenDongXe("Air Blade Seed");
            model.setLoaiPhuongTien(LoaiPhuongTien.XE_MAY);
            model.setSoChoHanhKhachMacDinh(1);
            model.setDangHoatDong(true);
        }

        return modelRepository.save(model);
    }

    private PhuongTien ensureVehicle(NguoiDung driver, DongXe vehicleModel, Instant now) {
        PhuongTien vehicle = vehicleRepository.findByBienSoXeIgnoreCase(VEHICLE_PLATE)
                .orElseGet(() -> PhuongTien.builder()
                        .bienSoXe(VEHICLE_PLATE)
                        .mauSacThucTe("Đen")
                        .soChoHanhKhachDuocDuyet(1)
                        .coSoSuDung(CoSoSuDungPhuongTien.CHINH_CHU)
                        .nguoiDangKySuDung(driver)
                        .dongXe(vehicleModel)
                        .build());

        boolean changed = false;
        if (vehicle.getTrangThaiPhuongTien() != TrangThaiPhuongTien.ACTIVE) {
            vehicle.setTrangThaiPhuongTien(TrangThaiPhuongTien.ACTIVE);
            changed = true;
        }
        if (vehicle.getNgayDuocDuyet() == null) {
            vehicle.setNgayDuocDuyet(now);
            changed = true;
        }
        if (!driver.equals(vehicle.getNguoiDangKySuDung())) {
            vehicle.setNguoiDangKySuDung(driver);
            changed = true;
        }
        if (!vehicleModel.equals(vehicle.getDongXe())) {
            vehicle.setDongXe(vehicleModel);
            changed = true;
        }
        if (!Integer.valueOf(1).equals(vehicle.getSoChoHanhKhachDuocDuyet())) {
            vehicle.setSoChoHanhKhachDuocDuyet(1);
            changed = true;
        }
        if (vehicle.getCoSoSuDung() != CoSoSuDungPhuongTien.CHINH_CHU) {
            vehicle.setCoSoSuDung(CoSoSuDungPhuongTien.CHINH_CHU);
            changed = true;
        }
        return vehicle.getId() == null || changed ? vehicleRepository.save(vehicle) : vehicle;
    }

    public record SeedSummary(Long driverUserId, Long driverProfileId, Long vehicleId) {
    }
}
