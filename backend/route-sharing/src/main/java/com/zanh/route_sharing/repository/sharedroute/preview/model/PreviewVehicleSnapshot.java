package com.zanh.route_sharing.repository.sharedroute.preview.model;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;

import java.util.Objects;

public record PreviewVehicleSnapshot(
        Long id,
        String licensePlate,
        String actualColor,
        String brandName,
        String modelName,
        LoaiPhuongTien vehicleType) {

    public PreviewVehicleSnapshot {
        Objects.requireNonNull(id, "id không được trống");
        Objects.requireNonNull(licensePlate, "licensePlate không được trống");
        Objects.requireNonNull(actualColor, "actualColor không được trống");
        Objects.requireNonNull(brandName, "brandName không được trống");
        Objects.requireNonNull(modelName, "modelName không được trống");
        Objects.requireNonNull(vehicleType, "vehicleType không được trống");
    }
}
