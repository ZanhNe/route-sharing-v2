package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.NhomQuyen;
import com.zanh.route_sharing.domain.entity.QuyenHan;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.repository.NguoiDungSecurityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {
    @Test
    void mapsOnlyActiveGroupsAndPermissionsToStableAuthorityCodes() {
        QuyenHan groupPermission = QuyenHan.builder()
                .maQuyen("review_vehicle")
                .tenQuyen("Review vehicle")
                .dangHoatDong(true)
                .build();
        QuyenHan directPermission = QuyenHan.builder()
                .maQuyen("handle_incident")
                .tenQuyen("Handle incident")
                .dangHoatDong(true)
                .build();
        QuyenHan inactivePermission = QuyenHan.builder()
                .maQuyen("unused")
                .tenQuyen("Unused")
                .dangHoatDong(false)
                .build();
        NhomQuyen group = NhomQuyen.builder()
                .maNhom("school_admin")
                .tenNhom("School Admin")
                .dangHoatDong(true)
                .danhSachQuyenHan(new LinkedHashSet<>(Set.of(groupPermission, inactivePermission)))
                .build();
        NguoiDung user = NguoiDung.builder()
                .id(7L)
                .hoTen("User")
                .emailTruong("USER@SCHOOL.EDU.VN")
                .matKhauDaMaHoa("hash")
                .trangThaiTaiKhoan(TrangThaiTaiKhoan.ACTIVE)
                .securityVersion(4L)
                .danhSachNhomQuyen(new LinkedHashSet<>(Set.of(group)))
                .danhSachQuyenTrucTiep(new LinkedHashSet<>(Set.of(directPermission)))
                .build();

        NguoiDungSecurityRepository repository = mock(NguoiDungSecurityRepository.class);
        when(repository.findByEmailTruongIgnoreCase("user@school.edu.vn")).thenReturn(Optional.of(user));
        CustomUserDetails principal = (CustomUserDetails) new CustomUserDetailsService(repository)
                .loadUserByUsername("user@school.edu.vn");

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_SCHOOL_ADMIN", "REVIEW_VEHICLE", "HANDLE_INCIDENT")
                .doesNotContain("UNUSED");
        assertThat(principal.getSecurityVersion()).isEqualTo(4L);
    }
}
