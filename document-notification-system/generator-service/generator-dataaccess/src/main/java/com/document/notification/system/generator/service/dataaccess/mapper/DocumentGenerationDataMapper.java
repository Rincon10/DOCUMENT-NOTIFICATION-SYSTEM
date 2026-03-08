package com.document.notification.system.generator.service.dataaccess.mapper;

import com.document.notification.system.generator.service.dataaccess.entity.DocumentGenerationEntity;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 7/03/2026
 */
@Component
public class DocumentGenerationDataMapper implements IDocumentGenerationDataMapper {
    @Override
    public DocumentGenerationEntity documentGenerationToDocumentGenerationEntity(DocumentGeneration documentGeneration) {
        return DocumentGenerationEntity.builder()
                .id(documentGeneration.getGenerationId().getValue())
                .customerId(documentGeneration.getCustomerId().getValue())
                .documentId(documentGeneration.getDocumentId().getValue())
                .documentName(documentGeneration.getDocumentType() != null
                        ? documentGeneration.getDocumentType().name()
                        : null)
                .status(documentGeneration.getGenerationStatus())
                .createdAt(documentGeneration.getCreatedAt() != null
                        ? documentGeneration.getCreatedAt().atZone(ZoneOffset.UTC)
                        : null)
                .build();
    }
}
