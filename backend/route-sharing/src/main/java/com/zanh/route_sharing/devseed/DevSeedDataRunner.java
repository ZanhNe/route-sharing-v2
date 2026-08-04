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
        DevSeedDataService.SeedSummary summary =
                seedDataService.seedSharedRouteScenario();

        log.info(
                "E1 seed ready: schoolId={}, passengerEmail={}, passengerId={}, "
                        + "driverEmail={}, driverId={}, vehiclePlate={}, vehicleId={}, "
                        + "sameDestinationRouteId={}, sameDeparture={}, "
                        + "segmentRouteId={}, segmentDeparture={}",
                summary.schoolId(),
                DevSeedDataService.PASSENGER_EMAIL,
                summary.passengerUserId(),
                DevSeedDataService.DRIVER_EMAIL,
                summary.driverUserId(),
                DevSeedDataService.VEHICLE_PLATE,
                summary.vehicleId(),
                summary.sameDestinationRouteId(),
                summary.sameDestinationDepartureTime(),
                summary.segmentRouteId(),
                summary.segmentDepartureTime());
    }
}
