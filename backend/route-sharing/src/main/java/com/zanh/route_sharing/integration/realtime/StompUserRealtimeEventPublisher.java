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

    public StompUserRealtimeEventPublisher(SimpMessagingTemplate messagingTemplate, SimpUserRegistry userRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.userRegistry = userRegistry;
    }

    @Override
    public void publish(Long recipientUserId, RealtimeEventEnvelope<?> event) {
        publish(recipientUserId, NOTIFICATION_DESTINATION, event);
    }

    @Override
    public void publish(Long recipientUserId, String userDestination, RealtimeEventEnvelope<?> event) {
        Objects.requireNonNull(recipientUserId, "recipientUserId không được trống");
        Objects.requireNonNull(userDestination, "userDestination không được trống");
        Objects.requireNonNull(event, "realtime event không được trống");
        if (recipientUserId <= 0) {
            throw new IllegalArgumentException("recipientUserId phải là số dương");
        }
        if (!userDestination.startsWith("/queue/")) {
            throw new IllegalArgumentException("Private userDestination phải thuộc /queue/**.");
        }

        Runnable dispatch = () -> dispatchSafely(recipientUserId, userDestination, event);
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
        dispatch.run();
    }

    private void dispatchSafely(Long recipientUserId, String userDestination, RealtimeEventEnvelope<?> event) {
        userRegistry.getUsers().stream()
                .filter(user -> belongsTo(user, recipientUserId))
                .map(SimpUser::getName)
                .distinct()
                .forEach(username -> sendSafely(username, recipientUserId, userDestination, event));
    }

    private void sendSafely(String username, Long recipientUserId, String userDestination,
            RealtimeEventEnvelope<?> event) {
        try {
            messagingTemplate.convertAndSendToUser(username, userDestination, event);
        } catch (RuntimeException exception) {
            LOGGER.warn("Realtime delivery failed after commit: recipientUserId={}, destination={}, eventType={}",
                    recipientUserId, userDestination, event.eventType(), exception);
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
