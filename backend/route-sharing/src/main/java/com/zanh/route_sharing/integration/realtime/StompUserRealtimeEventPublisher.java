package com.zanh.route_sharing.integration.realtime;

import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.realtime.UserRealtimeEventPublisher;
import com.zanh.route_sharing.service.realtime.model.RealtimeEventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.Principal;
import java.util.Objects;

@Component
public class StompUserRealtimeEventPublisher implements UserRealtimeEventPublisher {
    public static final String NOTIFICATION_DESTINATION = "/queue/notifications";

    private static final Logger LOGGER = LoggerFactory.getLogger(StompUserRealtimeEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    public StompUserRealtimeEventPublisher(
            SimpMessagingTemplate messagingTemplate,
            SimpUserRegistry userRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.userRegistry = userRegistry;
    }

    @Override
    public void publish(Long recipientUserId, RealtimeEventEnvelope<?> event) {
        Objects.requireNonNull(recipientUserId, "recipientUserId không được trống");
        Objects.requireNonNull(event, "realtime event không được trống");
        if (recipientUserId <= 0) {
            throw new IllegalArgumentException("recipientUserId phải là số dương");
        }

        Runnable dispatch = () -> dispatchSafely(recipientUserId, event);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
            return;
        }

        // Some use-cases (E2-01) deliberately commit in a short repository transaction
        // after provider work. When control returns here, the business transaction is
        // already committed, so immediate best-effort dispatch is still AFTER_COMMIT.
        dispatch.run();
    }

    private void dispatchSafely(Long recipientUserId, RealtimeEventEnvelope<?> event) {
        userRegistry.getUsers().stream()
                .filter(user -> belongsTo(user, recipientUserId))
                .map(SimpUser::getName)
                .distinct()
                .forEach(username -> sendSafely(username, recipientUserId, event));
    }

    private void sendSafely(
            String username,
            Long recipientUserId,
            RealtimeEventEnvelope<?> event) {
        try {
            messagingTemplate.convertAndSendToUser(username, NOTIFICATION_DESTINATION, event);
        } catch (RuntimeException exception) {
            // Business state and durable ThongBao are already committed. Realtime is a
            // best-effort signal; transport failure must never turn a committed command
            // into an apparent business rollback to the REST caller.
            LOGGER.warn(
                    "Realtime delivery failed after commit: recipientUserId={}, eventType={}",
                    recipientUserId,
                    event.eventType(),
                    exception);
        }
    }

    private static boolean belongsTo(SimpUser user, Long recipientUserId) {
        Principal principal = user.getPrincipal();
        if (!(principal instanceof Authentication authentication)) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            return false;
        }
        return recipientUserId.equals(details.getId());
    }
}
