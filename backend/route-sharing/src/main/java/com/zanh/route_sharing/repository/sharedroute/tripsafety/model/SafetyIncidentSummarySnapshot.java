package com.zanh.route_sharing.repository.sharedroute.tripsafety.model;

import com.zanh.route_sharing.domain.enums.LoaiSuCo;
import com.zanh.route_sharing.domain.enums.MucDoSuCo;
import com.zanh.route_sharing.domain.enums.NguonPhatHienSuCo;
import com.zanh.route_sharing.domain.enums.TrangThaiXuLySuCo;

import java.time.Instant;

public record SafetyIncidentSummarySnapshot(
        Long incidentId,
        Long tripId,
        LoaiSuCo type,
        MucDoSuCo severity,
        TrangThaiXuLySuCo status,
        NguonPhatHienSuCo reporterSource,
        Instant reportedAt) {
}
