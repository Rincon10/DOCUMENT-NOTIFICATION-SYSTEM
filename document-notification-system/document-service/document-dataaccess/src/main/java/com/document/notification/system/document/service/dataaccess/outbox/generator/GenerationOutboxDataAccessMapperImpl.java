package com.document.notification.system.document.service.dataaccess.outbox.generator;

import com.document.notification.system.document.service.dataaccess.outbox.generator.entity.GenerationOutboxEntity;
import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 25/02/2026
 */
@Component
public class GenerationOutboxDataAccessMapperImpl implements GenerationOutboxDataAccessMapperI {

    @Override
    public GenerationOutboxEntity mapDocumentGenerationOutboxMessageToGenerationOutboxEntity(DocumentGenerationOutboxMessage documentGenerationOutboxMessage) {
        return GenerationOutboxEntity.builder()
                .id(documentGenerationOutboxMessage.getId())
                .sagaId(documentGenerationOutboxMessage.getSagaId())
                .createdAt(documentGenerationOutboxMessage.getCreatedAt())
                .processedAt(documentGenerationOutboxMessage.getProcessedAt())
                .type(documentGenerationOutboxMessage.getType())
                .payload(documentGenerationOutboxMessage.getPayload())
                .sagaStatus(documentGenerationOutboxMessage.getSagaStatus())
                .documentStatus(documentGenerationOutboxMessage.getDocumentStatus())
                .outboxStatus(documentGenerationOutboxMessage.getOutboxStatus())
                .version(documentGenerationOutboxMessage.getVersion())
                .build();
    }

    @Override
    public DocumentGenerationOutboxMessage mapGenerationOutboxEntityToDocumentGenerationOutboxMessage(GenerationOutboxEntity generationOutboxEntity) {
        return DocumentGenerationOutboxMessage.builder()
                .id(generationOutboxEntity.getId())
                .sagaId(generationOutboxEntity.getSagaId())
                .createdAt(generationOutboxEntity.getCreatedAt())
                .processedAt(generationOutboxEntity.getProcessedAt())
                .type(generationOutboxEntity.getType())
                .payload(generationOutboxEntity.getPayload())
                .sagaStatus(generationOutboxEntity.getSagaStatus())
                .documentStatus(generationOutboxEntity.getDocumentStatus())
                .outboxStatus(generationOutboxEntity.getOutboxStatus())
                .version(generationOutboxEntity.getVersion())
                .build();
    }
}
