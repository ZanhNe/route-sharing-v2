package com.zanh.route_sharing.exception;

import lombok.Getter;

@Getter
public class SecurityAlertException extends RuntimeException {
    private final Object metaInfo;

    public SecurityAlertException(String message, Object metaInfo) {
        super(message);
        this.metaInfo = metaInfo;
    }
}