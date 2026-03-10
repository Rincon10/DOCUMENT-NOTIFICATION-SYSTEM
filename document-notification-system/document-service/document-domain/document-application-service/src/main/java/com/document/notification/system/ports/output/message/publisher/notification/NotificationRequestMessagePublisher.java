package com.document.notification.system.ports.output.message.publisher.notification;

import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.notification.DocumentNotificationOutboxMessage;

import java.util.function.BiConsumer;

public interface NotificationRequestMessagePublisher {
    void publish(DocumentNotificationOutboxMessage documentNotificationOutboxMessage,
                 BiConsumer<DocumentNotificationOutboxMessage, OutboxStatus> outboxCallback);
}
