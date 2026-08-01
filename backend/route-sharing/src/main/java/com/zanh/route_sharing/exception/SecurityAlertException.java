package com.zanh.route_sharing.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SecurityAlertException extends BusinessException {
    private final String referenceCode;

    public SecurityAlertException(String message, String referenceCode) {
        super(HttpStatus.LOCKED, "SECURITY_ALERT", message);
        this.referenceCode = referenceCode;
    }
}
