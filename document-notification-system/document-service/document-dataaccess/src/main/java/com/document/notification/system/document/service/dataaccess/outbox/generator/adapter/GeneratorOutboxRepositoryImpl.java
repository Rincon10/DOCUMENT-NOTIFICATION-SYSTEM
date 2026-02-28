package com.document.notification.system.document.service.dataaccess.outbox.generator.adapter;

import com.document.notification.system.document.service.dataaccess.outbox.generator.GenerationOutboxDataAccessMapperI;
import com.document.notification.system.document.service.dataaccess.outbox.generator.entity.GenerationOutboxEntity;
import com.document.notification.system.document.service.dataaccess.outbox.generator.repository.GeneratorOutboxJpaRepository;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.exception.GenerationOutboxNotFoundException;
import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;
import com.document.notification.system.ports.output.repository.GeneratorOutboxRepository;
import com.document.notification.system.saga.SagaStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/02/2026
 */
@Component
@AllArgsConstructor
public class GeneratorOutboxRepositoryImpl implements GeneratorOutboxRepository {

    private final GeneratorOutboxJpaRepository jpaRepository;
    private final GenerationOutboxDataAccessMapperI generationOutboxDataAccessMapper;

    @Override
    public DocumentGenerationOutboxMessage save(DocumentGenerationOutboxMessage documentGenerationOutboxMessage) {

        GenerationOutboxEntity outboxEntity = generationOutboxDataAccessMapper.mapDocumentGenerationOutboxMessageToGenerationOutboxEntity(documentGenerationOutboxMessage);
        GenerationOutboxEntity savedOutboxEntity = jpaRepository.save(outboxEntity);
        return generationOutboxDataAccessMapper.mapGenerationOutboxEntityToDocumentGenerationOutboxMessage(savedOutboxEntity);
    }

    @Override
    public Optional<List<DocumentGenerationOutboxMessage>> findByTypeAndOutboxStatusAndSagaStatus(String sagaType, OutboxStatus outboxStatus, SagaStatus... sagaStatus) {
        List<GenerationOutboxEntity> generationOutboxEntities = jpaRepository.findByTypeAndOutboxStatusAndSagaStatusIn(sagaType, outboxStatus, Arrays.asList(sagaStatus))
                .orElseThrow(() -> new GenerationOutboxNotFoundException("Generation Outbox object was not found for saga " + sagaType));

        List<DocumentGenerationOutboxMessage> documentGenerationOutboxMessages =
                generationOutboxEntities.stream().map(generationOutboxDataAccessMapper::mapGenerationOutboxEntityToDocumentGenerationOutboxMessage).collect(Collectors.toList());
        return Optional.of(documentGenerationOutboxMessages);
    }

    @Override
    public Optional<DocumentGenerationOutboxMessage> findByTypeAndSagaIdAndSagaStatus(String type, UUID sagaId, SagaStatus... sagaStatus) {
        return Optional.empty();
    }

    @Override
    public void deleteByTypeAndOutboxStatusAndSagaStatus(String type, OutboxStatus outboxStatus, SagaStatus... sagaStatus) {

    }
}
