package com.zanh.route_sharing.service.realtime.model;

public record TripSafetyIncidentStatusChangedRealtimeData(
        Long incidentId,
        Long tripId,
        String status) {}
