package com.document.notification.system.generator.service.dataaccess.outbox.document.adapter;

import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.generator.service.dataaccess.outbox.document.entity.DocumentOutboxEntity;
import com.document.notification.system.generator.service.dataaccess.outbox.document.mapper.DocumentOutboxDataAccessMapper;
import com.document.notification.system.generator.service.dataaccess.outbox.document.repository.DocumentOutboxJpaRepository;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.generator.service.domain.ports.output.repository.DocumentOutboxRepository;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 7/03/2026
 */
@Component
@AllArgsConstructor
public class DocumentOutboxRepositoryImpl implements DocumentOutboxRepository {

    private final DocumentOutboxJpaRepository documentOutboxRepository;
    private final DocumentOutboxDataAccessMapper documentOutboxDataAccessMapper;


    @Override
    public DocumentOutboxMessage save(DocumentOutboxMessage documentOutboxMessage) {
        DocumentOutboxEntity documentOutboxEntity = documentOutboxDataAccessMapper.mapDocumentOutboxMessageToDocumentOutboxEntity(documentOutboxMessage);
        DocumentOutboxEntity savedEntity = documentOutboxRepository.save(documentOutboxEntity);
        return documentOutboxDataAccessMapper.mapDocumentOutboxEntityToDocumentOutboxMessage(savedEntity);
    }

    @Override
    public Optional<List<DocumentOutboxMessage>> findByTypeAndOutboxStatus(String type, OutboxStatus status) {

        return Optional.of(documentOutboxRepository.findByTypeAndOutboxStatus(type, status)
                .orElseThrow(() -> new RuntimeException("Document Outbox object was not found for type " + type))
                .stream()
                .map(documentOutboxDataAccessMapper::mapDocumentOutboxEntityToDocumentOutboxMessage)
                .toList());
    }

    @Override
    public Optional<DocumentOutboxMessage> findByTypeAndSagaIdAndGenerationStatusAndOutboxStatus(String type, UUID sagaId, GenerationStatus generationStatus, OutboxStatus outboxStatus) {
        throw new UnsupportedOperationException("Find operation is not supported for DocumentOutboxRepositoryImpl");
    }

    @Override
    public void deleteByTypeAndOutboxStatus(String type, OutboxStatus status) {
        throw new UnsupportedOperationException("Delete operation is not supported for DocumentOutboxRepositoryImpl");
    }
}
