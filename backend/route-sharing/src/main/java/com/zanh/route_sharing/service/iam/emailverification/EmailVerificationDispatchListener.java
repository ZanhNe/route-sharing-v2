package com.zanh.route_sharing.service.iam.emailverification;

import com.zanh.route_sharing.integration.mail.VerificationEmailDeliveryException;
import com.zanh.route_sharing.integration.mail.VerificationEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationDispatchListener {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationDispatchListener.class);
    private final VerificationEmailSender sender;
    private final EmailVerificationCommitCoordinator coordinator;

    public EmailVerificationDispatchListener(
            VerificationEmailSender sender,
            EmailVerificationCommitCoordinator coordinator) {
        this.sender = sender;
        this.coordinator = coordinator;
    }

    @Async("applicationTaskExecutor")
    @EventListener
    public void dispatch(EmailVerificationDispatchRequested event) {
        try {
            sender.sendVerificationCode(event.destination(), event.code());
            coordinator.markDelivered(event.accountId(), event.challengeId());
        } catch (VerificationEmailDeliveryException exception) {
            coordinator.markDeliveryFailed(event.accountId(), event.challengeId());
            log.warn("Email verification delivery failed for challengeId={}, category={}",
                    event.challengeId(), exception.getCategory());
        } catch (RuntimeException exception) {
            coordinator.markDeliveryFailed(event.accountId(), event.challengeId());
            log.error("Unexpected email verification delivery failure for challengeId={}, exceptionType={}",
                    event.challengeId(), exception.getClass().getSimpleName());
        }
    }
}
