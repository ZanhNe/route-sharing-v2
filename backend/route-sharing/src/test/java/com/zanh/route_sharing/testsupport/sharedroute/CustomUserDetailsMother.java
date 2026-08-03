package com.zanh.route_sharing.testsupport.sharedroute;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.security.CustomUserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.List;

public final class CustomUserDetailsMother {

    private CustomUserDetailsMother() {
    }

    public static CustomUserDetails activeUser(Long userId, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new CustomUserDetails(
                userId,
                "user-" + userId + "@school.edu.vn",
                "test-password-hash",
                TrangThaiTaiKhoan.ACTIVE,
                1L,
                grantedAuthorities);
    }
}
