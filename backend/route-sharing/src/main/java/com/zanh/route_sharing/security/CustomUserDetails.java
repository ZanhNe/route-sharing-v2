package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@Getter
@EqualsAndHashCode(of = "id")
public final class CustomUserDetails implements UserDetails, CredentialsContainer, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String emailTruong;
    private String password;
    private final TrangThaiTaiKhoan trangThaiTaiKhoan;
    private final Long securityVersion;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(Long id,
                             String emailTruong,
                             String password,
                             TrangThaiTaiKhoan trangThaiTaiKhoan,
                             Long securityVersion,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.emailTruong = emailTruong;
        this.password = password;
        this.trangThaiTaiKhoan = trangThaiTaiKhoan;
        this.securityVersion = securityVersion == null ? 0L : securityVersion;
        this.authorities = List.copyOf(authorities);
    }

    @Override
    public String getUsername() {
        return emailTruong;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return trangThaiTaiKhoan != TrangThaiTaiKhoan.DEACTIVATED;
    }

    @Override
    public boolean isAccountNonLocked() {
        return trangThaiTaiKhoan != TrangThaiTaiKhoan.SUSPENDED
                && trangThaiTaiKhoan != TrangThaiTaiKhoan.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return trangThaiTaiKhoan == TrangThaiTaiKhoan.ACTIVE;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
