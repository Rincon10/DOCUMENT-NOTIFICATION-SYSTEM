package com.document.notification.system.notification.service.domain.ports.output.repository;

import com.document.notification.system.notification.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.notification.service.domain.valueobject.NotificationStatus;
import com.document.notification.system.outbox.OutboxStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentOutboxRepository {
    DocumentOutboxMessage save(DocumentOutboxMessage documentOutboxMessage);

    Optional<List<DocumentOutboxMessage>> findByTypeAndOutboxStatus(String type, OutboxStatus status);

    Optional<DocumentOutboxMessage> findByTypeAndSagaIdAndNotificationStatusAndOutboxStatus(String type,
                                                                                             UUID sagaId,
                                                                                             NotificationStatus notificationStatus,
                                                                                             OutboxStatus outboxStatus);

    void deleteByTypeAndOutboxStatus(String type, OutboxStatus status);
}
