package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.auth.registration.AccountRegistrationRequest;
import com.zanh.route_sharing.dto.auth.registration.AccountRegistrationResponse;
import com.zanh.route_sharing.dto.auth.registration.RegistrationLegalContextResponse;
import com.zanh.route_sharing.dto.auth.registration.RegistrationSchoolResponse;

import java.util.List;

public interface AccountRegistrationService {
    List<RegistrationSchoolResponse> listRegistrationSchools();

    RegistrationLegalContextResponse getRegistrationLegalContext(Long schoolId);

    AccountRegistrationResponse register(AccountRegistrationRequest request,
            String remoteAddress,
            String userAgentEvidence);
}
