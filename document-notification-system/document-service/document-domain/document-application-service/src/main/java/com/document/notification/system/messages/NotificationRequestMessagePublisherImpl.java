package com.document.notification.system.messages;

import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.notification.DocumentNotificationOutboxMessage;
import com.document.notification.system.ports.output.message.publisher.notification.NotificationRequestMessagePublisher;
import com.document.notification.system.saga.DocumentNotificationSaga;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.function.BiConsumer;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 9/03/2026
 */

@Slf4j
@Service
@Validated
@AllArgsConstructor
public class NotificationRequestMessagePublisherImpl implements NotificationRequestMessagePublisher {

    private final DocumentNotificationSaga documentNotificationSaga;

    @Override
    public void publish(DocumentNotificationOutboxMessage documentNotificationOutboxMessage, BiConsumer<DocumentNotificationOutboxMessage, OutboxStatus> outboxCallback) {

    }
}
