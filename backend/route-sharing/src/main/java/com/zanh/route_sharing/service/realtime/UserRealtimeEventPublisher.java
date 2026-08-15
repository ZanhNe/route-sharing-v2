package com.zanh.route_sharing.service.realtime;

import com.zanh.route_sharing.service.realtime.model.RealtimeEventEnvelope;

public interface UserRealtimeEventPublisher {
    void publish(Long recipientUserId, RealtimeEventEnvelope<?> event);

    void publish(Long recipientUserId, String userDestination, RealtimeEventEnvelope<?> event);
}
