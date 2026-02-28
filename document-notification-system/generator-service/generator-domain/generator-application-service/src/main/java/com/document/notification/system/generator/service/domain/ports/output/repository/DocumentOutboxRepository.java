package com.document.notification.system.generator.service.domain.ports.output.repository;

import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.outbox.OutboxStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentOutboxRepository {
    DocumentOutboxMessage save(DocumentOutboxMessage documentOutboxMessage);

    Optional<List<DocumentOutboxMessage>> findByTypeAndOutboxStatus(String type, OutboxStatus status);

    Optional<DocumentOutboxMessage> findByTypeAndSagaIdAndGenerationStatusAndOutboxStatus(String type,
                                                                                          UUID sagaId,
                                                                                          GenerationStatus generationStatus,
                                                                                          OutboxStatus outboxStatus);

    void deleteByTypeAndOutboxStatus(String type, OutboxStatus status);
}

