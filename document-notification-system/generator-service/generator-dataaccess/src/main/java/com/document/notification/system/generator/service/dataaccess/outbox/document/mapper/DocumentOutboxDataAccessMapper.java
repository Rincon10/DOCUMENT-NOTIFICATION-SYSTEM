package com.document.notification.system.generator.service.dataaccess.outbox.document.mapper;

import com.document.notification.system.generator.service.dataaccess.outbox.document.entity.DocumentOutboxEntity;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentOutboxMessage;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 7/03/2026
 */
@Component
public class DocumentOutboxDataAccessMapper {
    public DocumentOutboxEntity mapDocumentOutboxMessageToDocumentOutboxEntity(DocumentOutboxMessage documentOutboxMessage) {
        return null;
    }

    public DocumentOutboxMessage mapDocumentOutboxEntityToDocumentOutboxMessage(DocumentOutboxEntity savedEntity) {
        return DocumentOutboxMessage.builder()
                .id(savedEntity.getId())
                .sagaId(savedEntity.getSagaId())
                .createdAt(savedEntity.getCreatedAt())
                .processedAt(savedEntity.getProcessedAt())
                .type(savedEntity.getType())
                .payload(savedEntity.getPayload())
                .generationStatus(savedEntity.getGenerationStatus())
                .outboxStatus(savedEntity.getOutboxStatus())
                .version(savedEntity.getVersion())
                .build();
    }
}
