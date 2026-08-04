package com.zanh.route_sharing.dto.sharedroute.preview;

import com.zanh.route_sharing.domain.enums.LoaiPhuongTien;

public record PreviewVehicleResponse(
                Long id,
                String licensePlate,
                String actualColor,
                String brandName,
                String modelName,
                LoaiPhuongTien vehicleType) {
}
