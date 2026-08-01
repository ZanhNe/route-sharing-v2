package com.zanh.route_sharing.repository;

import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;

public interface SecurityStateProjection {
    TrangThaiTaiKhoan getTrangThaiTaiKhoan();
    Long getSecurityVersion();
}
