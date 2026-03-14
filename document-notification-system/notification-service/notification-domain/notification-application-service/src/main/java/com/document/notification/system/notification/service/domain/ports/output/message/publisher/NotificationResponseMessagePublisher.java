package com.document.notification.system.notification.service.domain.ports.output.message.publisher;

import com.document.notification.system.notification.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.outbox.OutboxStatus;

import java.util.function.BiConsumer;

public interface NotificationResponseMessagePublisher {
    void publish(DocumentOutboxMessage documentOutboxMessage, BiConsumer<DocumentOutboxMessage, OutboxStatus> outboxCallback);
}
