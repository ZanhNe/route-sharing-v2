package com.zanh.route_sharing.service.realtime;

import com.zanh.route_sharing.service.realtime.model.RealtimeEventEnvelope;

/**
 * Provider-neutral outbound port for private user realtime delivery.
 *
 * <p>The recipient is a domain user id. Transport-specific resolution to the
 * authenticated STOMP principal is owned by the infrastructure adapter.</p>
 */
public interface UserRealtimeEventPublisher {
    void publish(Long recipientUserId, RealtimeEventEnvelope<?> event);
}
