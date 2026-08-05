package com.zanh.route_sharing.security;

import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Shared guard for application operations that require an authenticated user id. */
public final class AuthenticatedPrincipalValidator {

    private AuthenticatedPrincipalValidator() {
    }

    public static void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTHENTICATED_USER_REQUIRED",
                    "Không xác định được người dùng đang đăng nhập.");
        }
    }
}
