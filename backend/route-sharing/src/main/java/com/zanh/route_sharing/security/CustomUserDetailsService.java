package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.entity.QuyenHan;
import com.zanh.route_sharing.repository.NguoiDungSecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final NguoiDungSecurityRepository repository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Email đăng nhập không hợp lệ");
        }
        NguoiDung user = repository.findByEmailTruongIgnoreCase(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadUserById(Long userId) {
        NguoiDung user = repository.findPrincipalById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        return toPrincipal(user);
    }

    private static CustomUserDetails toPrincipal(NguoiDung user) {
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();

        user.getDanhSachNhomQuyen().stream()
                .filter(group -> Boolean.TRUE.equals(group.getDangHoatDong()))
                .forEach(group -> {
                    String groupCode = normalizeCode(group.getMaNhom());
                    authorities.add(new SimpleGrantedAuthority(
                            groupCode.startsWith("ROLE_") ? groupCode : "ROLE_" + groupCode));
                    group.getDanhSachQuyenHan().stream()
                            .filter(permission -> Boolean.TRUE.equals(permission.getDangHoatDong()))
                            .map(QuyenHan::getMaQuyen)
                            .map(CustomUserDetailsService::normalizeCode)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                });

        user.getDanhSachQuyenTrucTiep().stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getDangHoatDong()))
                .map(QuyenHan::getMaQuyen)
                .map(CustomUserDetailsService::normalizeCode)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return new CustomUserDetails(
                user.getId(),
                user.getEmailTruong(),
                user.getMatKhauDaMaHoa(),
                user.getTrangThaiTaiKhoan(),
                user.getSecurityVersion(),
                authorities);
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("Mã quyền không được để trống");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
