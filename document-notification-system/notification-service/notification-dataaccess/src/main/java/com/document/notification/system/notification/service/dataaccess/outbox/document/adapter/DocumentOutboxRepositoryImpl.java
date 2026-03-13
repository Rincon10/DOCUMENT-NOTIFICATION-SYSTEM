package com.document.notification.system.notification.service.dataaccess.outbox.document.adapter;

import com.document.notification.system.notification.service.dataaccess.outbox.document.entity.DocumentOutboxEntity;
import com.document.notification.system.notification.service.dataaccess.outbox.document.mapper.DocumentOutboxDataAccessMapper;
import com.document.notification.system.notification.service.dataaccess.outbox.document.repository.DocumentOutboxJpaRepository;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.notification.service.domain.ports.output.repository.DocumentOutboxRepository;
import com.document.notification.system.notification.service.domain.valueobject.NotificationStatus;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
public class DocumentOutboxRepositoryImpl implements DocumentOutboxRepository {

    private final DocumentOutboxJpaRepository documentOutboxRepository;
    private final DocumentOutboxDataAccessMapper documentOutboxDataAccessMapper;

    @Override
    public DocumentOutboxMessage save(DocumentOutboxMessage documentOutboxMessage) {
        DocumentOutboxEntity documentOutboxEntity = documentOutboxDataAccessMapper
                .mapDocumentOutboxMessageToDocumentOutboxEntity(documentOutboxMessage);
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
    public Optional<DocumentOutboxMessage> findByTypeAndSagaIdAndNotificationStatusAndOutboxStatus(String type,
                                                                                                    UUID sagaId,
                                                                                                    NotificationStatus notificationStatus,
                                                                                                    OutboxStatus outboxStatus) {
        return documentOutboxRepository
                .findByTypeAndSagaIdAndNotificationStatusAndOutboxStatus(type, sagaId, notificationStatus, outboxStatus)
                .map(documentOutboxDataAccessMapper::mapDocumentOutboxEntityToDocumentOutboxMessage);
    }

    @Override
    public void deleteByTypeAndOutboxStatus(String type, OutboxStatus status) {
        documentOutboxRepository.deleteByTypeAndOutboxStatus(type, status);
    }
}
