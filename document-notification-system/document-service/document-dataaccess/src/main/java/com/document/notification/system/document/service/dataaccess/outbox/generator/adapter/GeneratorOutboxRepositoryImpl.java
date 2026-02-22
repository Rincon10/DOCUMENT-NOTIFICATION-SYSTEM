package com.document.notification.system.document.service.dataaccess.outbox.generator.adapter;

import com.document.notification.system.document.service.dataaccess.outbox.generator.repository.GeneratorOutboxJpaRepository;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;
import com.document.notification.system.ports.output.repository.GeneratorOutboxRepository;
import com.document.notification.system.saga.SagaStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/02/2026
 */
@Component
@AllArgsConstructor
public class GeneratorOutboxRepositoryImpl implements GeneratorOutboxRepository {

    private final GeneratorOutboxJpaRepository jpaRepository;

    @Override
    public DocumentGenerationOutboxMessage save(DocumentGenerationOutboxMessage documentGenerationOutboxMessage) {
        return null;
    }

    @Override
    public Optional<List<DocumentGenerationOutboxMessage>> findByTypeAndOutboxStatusAndSagaStatus(String type, OutboxStatus outboxStatus, SagaStatus... sagaStatus) {
        return Optional.empty();
    }

    @Override
    public Optional<DocumentGenerationOutboxMessage> findByTypeAndSagaIdAndSagaStatus(String type, UUID sagaId, SagaStatus... sagaStatus) {
        return Optional.empty();
    }

    @Override
    public void deleteByTypeAndOutboxStatusAndSagaStatus(String type, OutboxStatus outboxStatus, SagaStatus... sagaStatus) {

    }
}
