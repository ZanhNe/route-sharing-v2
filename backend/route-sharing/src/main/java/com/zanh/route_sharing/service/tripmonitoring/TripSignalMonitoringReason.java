package com.zanh.route_sharing.service.tripmonitoring;

public enum TripSignalMonitoringReason {
    SIGNAL_DELAY_THRESHOLD_EXCEEDED,
    SIGNAL_LOST_THRESHOLD_EXCEEDED,
    FRESH_SIGNAL_RECLASSIFIED,
    FRESH_SIGNAL_RECOVERED
}
