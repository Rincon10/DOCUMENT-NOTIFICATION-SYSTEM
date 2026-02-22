package com.document.notification.system.ports.output.repository;

import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.notification.DocumentNotificationOutboxMessage;
import com.document.notification.system.saga.SagaStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationOutboxRepository {
    DocumentNotificationOutboxMessage save(DocumentNotificationOutboxMessage documentNotificationOutboxMessage);


    Optional<List<DocumentNotificationOutboxMessage>> findByTypeAndOutboxStatusAndSagaStatus(String type,
                                                                                             OutboxStatus outboxStatus,
                                                                                             SagaStatus... sagaStatus);

    Optional<DocumentNotificationOutboxMessage> findByTypeAndSagaIdAndSagaStatus(String type,
                                                                                 UUID sagaId,
                                                                                 SagaStatus... sagaStatus);

    void deleteByTypeAndOutboxStatusAndSagaStatus(String type,
                                                  OutboxStatus outboxStatus,
                                                  SagaStatus... sagaStatus);
}
