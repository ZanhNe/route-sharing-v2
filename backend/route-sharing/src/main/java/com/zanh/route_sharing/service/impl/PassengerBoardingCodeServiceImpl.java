package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.dto.trip.boarding.PassengerBoardingCodeResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.boarding.PassengerBoardingCodeRepository;
import com.zanh.route_sharing.repository.sharedroute.boarding.model.PassengerBoardingCodeCommand;
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.service.PassengerBoardingCodeService;
import com.zanh.route_sharing.service.boarding.PassengerBoardingCodeResponseMapper;
import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class PassengerBoardingCodeServiceImpl implements PassengerBoardingCodeService {
    private final PassengerBoardingCodeRepository repository;
    private final PassengerBoardingCodeResponseMapper responseMapper;
    private final Clock clock;

    public PassengerBoardingCodeServiceImpl(
            PassengerBoardingCodeRepository repository,
            PassengerBoardingCodeResponseMapper responseMapper,
            Clock clock) {
        this.repository = repository;
        this.responseMapper = responseMapper;
        this.clock = clock;
    }

    @Override
    public PassengerBoardingCodeResponse requestOwnCode(Long actorId, Long tripId) {
        AuthenticatedPrincipalValidator.requireUserId(actorId);
        if (tripId == null || tripId <= 0) {
            throw validation("tripId phải là số dương.");
        }
        return responseMapper.toResponse(repository.getOrCreate(
                new PassengerBoardingCodeCommand(actorId, tripId, TimePolicy.now(clock))));
    }

    private static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
