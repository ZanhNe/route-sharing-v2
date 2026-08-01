package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {
    @Test
    void eraseCredentialsRemovesPasswordHashFromSessionPrincipal() {
        CustomUserDetails principal = new CustomUserDetails(
                1L,
                "user@school.edu.vn",
                "$2a$12$secret-hash",
                TrangThaiTaiKhoan.ACTIVE,
                3L,
                List.of()
        );

        principal.eraseCredentials();

        assertThat(principal.getPassword()).isNull();
    }
}
