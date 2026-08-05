package com.zanh.route_sharing.service.riderequest.model;

public record LocationLabel(String formattedAddress) {

    public LocationLabel {
        if (formattedAddress == null || formattedAddress.isBlank()) {
            throw new IllegalArgumentException("formattedAddress không được trống");
        }
        formattedAddress = formattedAddress.trim();
        if (formattedAddress.length() > 500) {
            throw new IllegalArgumentException("formattedAddress không được vượt quá 500 ký tự");
        }
    }
}
