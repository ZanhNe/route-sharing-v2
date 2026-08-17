package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.trip.dropoffverification.PassengerDropoffCodeResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.PassengerDropoffCodeRepository;
import com.zanh.route_sharing.repository.sharedroute.dropoffverification.model.PassengerDropoffCodeCommand;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.PassengerDropoffCodeService;
import com.zanh.route_sharing.service.dropoffverification.PassengerDropoffCodeResponseMapper;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Clock;

@Service
public class PassengerDropoffCodeServiceImpl implements PassengerDropoffCodeService {
    private final PassengerDropoffCodeRepository repository;
    private final PassengerDropoffCodeResponseMapper responseMapper;
    private final Clock clock;

    public PassengerDropoffCodeServiceImpl(PassengerDropoffCodeRepository repository,
            PassengerDropoffCodeResponseMapper responseMapper, Clock clock) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.clock = clock;
    }

    @Override
    public PassengerDropoffCodeResponse requestOwnCode(Long actorId, Long tripId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0)
            throw validation("tripId phải là số dương.");
        return responseMapper.toResponse(
                repository.getOrCreate(new PassengerDropoffCodeCommand(actorId, tripId, TimePolicy.now(clock))));
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
