package com.zanh.route_sharing.security;

import com.zanh.route_sharing.exception.ResourceNotFoundException;
import com.zanh.route_sharing.repository.NguoiDungSecurityRepository;
import com.zanh.route_sharing.repository.SecurityStateProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityStateService {
    private final NguoiDungSecurityRepository repository;

    @Transactional(readOnly = true)
    public SecurityState requireCurrent(Long userId) {
        SecurityStateProjection projection = repository.findSecurityStateById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không còn tồn tại."));
        return new SecurityState(
                projection.getTrangThaiTaiKhoan(),
                projection.getSecurityVersion() == null ? 0L : projection.getSecurityVersion()
        );
    }
}
