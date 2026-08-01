package com.zanh.route_sharing.security;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;

public record SecurityState(TrangThaiTaiKhoan status, long securityVersion) {
}
