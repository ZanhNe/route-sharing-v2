package com.zanh.route_sharing.service.iam.emailverification;

import com.zanh.route_sharing.dto.auth.emailverification.EmailVerificationRequestResponse;
import com.zanh.route_sharing.dto.auth.emailverification.EmailVerificationResponse;
import com.zanh.route_sharing.dto.auth.emailverification.EmailVerificationTokenResponse;
import com.zanh.route_sharing.security.IssuedToken;
import com.zanh.route_sharing.security.OnboardingAccessTokenService;
import com.zanh.route_sharing.security.OnboardingStep;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private final EmailVerificationCodeGenerator codeGenerator;
    private final EmailVerificationAbuseGuard abuseGuard;
    private final EmailVerificationCommitCoordinator coordinator;
    private final ApplicationEventPublisher eventPublisher;
    private final OnboardingAccessTokenService onboardingTokenService;

    public EmailVerificationServiceImpl(
            EmailVerificationCodeGenerator codeGenerator,
            EmailVerificationAbuseGuard abuseGuard,
            EmailVerificationCommitCoordinator coordinator,
            ApplicationEventPublisher eventPublisher,
            OnboardingAccessTokenService onboardingTokenService) {
        this.codeGenerator = codeGenerator;
        this.abuseGuard = abuseGuard;
        this.coordinator = coordinator;
        this.eventPublisher = eventPublisher;
        this.onboardingTokenService = onboardingTokenService;
    }

    @Override
    public EmailVerificationRequestResponse requestCode(Long accountId, String remoteAddress) {
        abuseGuard.checkRequest(accountId, remoteAddress);
        String code = codeGenerator.generate();
        PreparedEmailVerification prepared = coordinator.prepareCandidate(accountId, code);
        eventPublisher.publishEvent(new EmailVerificationDispatchRequested(
                prepared.accountId(), prepared.challengeId(), prepared.destination(), prepared.code()));
        return new EmailVerificationRequestResponse(true);
    }

    @Override
    public EmailVerificationResponse verifyCode(Long accountId, String code, String remoteAddress) {
        abuseGuard.checkVerify(accountId, remoteAddress);
        VerifiedEmailAccount verified = coordinator.verify(accountId, code);
        IssuedToken replacementToken = onboardingTokenService.issue(
                verified.accountId(),
                verified.email(),
                verified.accountStatus(),
                verified.securityVersion(),
                OnboardingStep.COMPLETE_PROFILE);
        return new EmailVerificationResponse(
                verified.accountStatus().name(),
                true,
                verified.verifiedAt(),
                "COMPLETE_PROFILE",
                new EmailVerificationTokenResponse(
                        "Bearer",
                        replacementToken.value(),
                        replacementToken.expiresAt()));
    }
}
