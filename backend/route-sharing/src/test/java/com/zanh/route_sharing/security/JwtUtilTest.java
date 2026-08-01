package com.zanh.route_sharing.security;

import com.zanh.route_sharing.config.properties.JwtProperties;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {
    private static JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setBase64Secret(Base64.getEncoder().encodeToString(new byte[64]));
        properties.setIssuer("test-issuer");
        properties.setAudience("test-audience");
        properties.setAccessTokenTtl(Duration.ofMinutes(5));
        properties.setRefreshTokenTtl(Duration.ofDays(1));
        properties.setClockSkew(Duration.ZERO);
        return properties;
    }

    @Test
    void accessTokenRoundTripPreservesSecurityClaims() {
        JwtUtil jwt = new JwtUtil(properties(), java.time.Clock.systemUTC());
        CustomUserDetails principal = new CustomUserDetails(
                10L, "user@school.edu.vn", "hash", TrangThaiTaiKhoan.ACTIVE, 7L,
                List.of(new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("CREATE_ROUTE")));

        IssuedToken issued = jwt.issueAccessToken(principal);
        JwtAccessClaims claims = jwt.parseAccessToken(issued.value());

        assertThat(claims.userId()).isEqualTo(10L);
        assertThat(claims.securityVersion()).isEqualTo(7L);
        assertThat(claims.accountStatus()).isEqualTo(TrangThaiTaiKhoan.ACTIVE);
        assertThat(claims.authorities()).containsExactly("CREATE_ROUTE", "ROLE_USER");
    }

    @Test
    void refreshTokenCannotBeParsedAsAccessToken() {
        JwtUtil jwt = new JwtUtil(properties(), java.time.Clock.systemUTC());
        CustomUserDetails principal = new CustomUserDetails(
                10L, "user@school.edu.vn", "hash", TrangThaiTaiKhoan.ACTIVE, 1L, List.of());
        IssuedToken refresh = jwt.issueRefreshToken(principal);

        assertThatThrownBy(() -> jwt.parseAccessToken(refresh.value()))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}
