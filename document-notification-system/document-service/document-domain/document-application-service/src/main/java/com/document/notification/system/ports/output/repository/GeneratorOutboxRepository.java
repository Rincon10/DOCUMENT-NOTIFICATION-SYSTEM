package com.document.notification.system.ports.output.repository;

import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;
import com.document.notification.system.saga.SagaStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeneratorOutboxRepository {


    DocumentGenerationOutboxMessage save(DocumentGenerationOutboxMessage documentGenerationOutboxMessage);


    Optional<List<DocumentGenerationOutboxMessage>> findByTypeAndOutboxStatusAndSagaStatus(String type,
                                                                                           OutboxStatus outboxStatus,
                                                                                           SagaStatus... sagaStatus);
    Optional<DocumentGenerationOutboxMessage> findByTypeAndSagaIdAndSagaStatus(String type,
                                                                         UUID sagaId,
                                                                         SagaStatus... sagaStatus);
    void deleteByTypeAndOutboxStatusAndSagaStatus(String type,
                                                  OutboxStatus outboxStatus,
                                                  SagaStatus... sagaStatus);
}
