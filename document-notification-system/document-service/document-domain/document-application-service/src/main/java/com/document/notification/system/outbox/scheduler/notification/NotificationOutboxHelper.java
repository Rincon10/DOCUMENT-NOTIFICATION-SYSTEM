package com.document.notification.system.outbox.scheduler.notification;

import com.document.notification.system.domain.valueobject.DocumentStatus;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.notification.DocumentNotificationEventPayload;
import com.document.notification.system.saga.SagaStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/02/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class NotificationOutboxHelper {

    //private final NotificationOutboxRepository notificationOutboxRepository;

    public void saveNotificationOutboxMessage(DocumentNotificationEventPayload documentNotificationEventPayload,
                                              DocumentStatus documentStatus,
                                              SagaStatus sagaStatus,
                                              OutboxStatus outboxStatus,
                                              UUID sagaId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
