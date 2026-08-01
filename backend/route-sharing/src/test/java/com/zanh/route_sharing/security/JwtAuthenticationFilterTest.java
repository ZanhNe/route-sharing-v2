package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAuthenticationFilterTest {
    private static JwtAccessClaims claims(TrangThaiTaiKhoan status, long version) {
        return new JwtAccessClaims(1L, "a@school.edu.vn", status, version,
                List.of("ROLE_USER"), "jti", Instant.now().plusSeconds(60));
    }

    @Test
    void acceptsMatchingActiveState() {
        assertThatCode(() -> JwtAuthenticationFilter.validateCurrentState(
                claims(TrangThaiTaiKhoan.ACTIVE, 3), new SecurityState(TrangThaiTaiKhoan.ACTIVE, 3)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInactiveCurrentAccount() {
        assertThatThrownBy(() -> JwtAuthenticationFilter.validateCurrentState(
                claims(TrangThaiTaiKhoan.ACTIVE, 3), new SecurityState(TrangThaiTaiKhoan.SUSPENDED, 3)))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void rejectsStaleSecurityVersion() {
        assertThatThrownBy(() -> JwtAuthenticationFilter.validateCurrentState(
                claims(TrangThaiTaiKhoan.ACTIVE, 2), new SecurityState(TrangThaiTaiKhoan.ACTIVE, 3)))
                .isInstanceOf(CredentialsExpiredException.class);
    }
}
