package com.zanh.route_sharing.devseed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("seed & !prod")
@RequiredArgsConstructor
public class DevSeedDataRunner implements ApplicationRunner {
    private final DevSeedDataService seedDataService;

    @Override
    public void run(ApplicationArguments args) {
        DevSeedDataService.SeedSummary summary = seedDataService.seedSharedRouteScenario();
        log.info("E1 seed ready: driverEmail={}, vehiclePlate={}, vehicleId={}",
                DevSeedDataService.DRIVER_EMAIL,
                DevSeedDataService.VEHICLE_PLATE,
                summary.vehicleId());
    }
}
