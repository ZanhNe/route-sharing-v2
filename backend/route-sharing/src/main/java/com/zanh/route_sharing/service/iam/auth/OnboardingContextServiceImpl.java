package com.zanh.route_sharing.service.iam.auth;

import com.zanh.route_sharing.domain.entity.NguoiDung;
import com.zanh.route_sharing.domain.enums.TrangThaiTaiKhoan;
import com.zanh.route_sharing.dto.auth.entry.OnboardingContextResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.NguoiDungSecurityRepository;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingContextServiceImpl implements OnboardingContextService {
    private final NguoiDungSecurityRepository repository;

    public OnboardingContextServiceImpl(NguoiDungSecurityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingContextResponse getCurrent(Long accountId) {
        AuthenticatedPrincipalValidator.requireUserId(accountId);
        NguoiDung account = repository.findPrincipalById(accountId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "ONBOARDING_SESSION_INVALID",
                        "Phiên onboarding không còn hợp lệ."));
        if (account.getTrangThaiTaiKhoan() != TrangThaiTaiKhoan.CHO_XAC_THUC_EMAIL
                && account.getTrangThaiTaiKhoan() != TrangThaiTaiKhoan.CHO_DUYET_HO_SO) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "ONBOARDING_SESSION_NOT_ALLOWED",
                    "Tài khoản không còn ở trạng thái onboarding.");
        }
        return OnboardingContextResponse.from(account);
    }
}
