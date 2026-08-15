package com.zanh.route_sharing.scheduler;

import com.zanh.route_sharing.service.TripSignalMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "routeshare.trip-monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TripSignalMonitoringScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripSignalMonitoringScheduler.class);

    private final TripSignalMonitoringService monitoringService;

    public TripSignalMonitoringScheduler(TripSignalMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Scheduled(fixedDelayString = "${routeshare.trip-monitoring.scan-delay-ms:5000}")
    public void runMonitoringScan() {
        try {
            monitoringService.evaluateTrackingActiveTrips();
        } catch (RuntimeException exception) {
            LOGGER.error("Shared Trip signal monitoring scan failed.", exception);
        }
    }
}
