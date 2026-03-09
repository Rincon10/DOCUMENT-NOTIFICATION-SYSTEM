package com.document.notification.system.generator.service.domain.mapper;

import com.document.notification.system.domain.utils.MapperUtils;
import com.document.notification.system.domain.valueobject.CustomerId;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.domain.valueobject.DocumentType;
import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.event.GenerationEvent;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.generator.service.domain.valueobject.GenerationId;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementation of GenerationDataMapper
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 3/03/2026
 */
@Component
public class GenerationDataMapperImpl implements GenerationDataMapper {

    @Override
    public DocumentGeneration generationRequestToDocumentGeneration(GenerationRequest generationRequest) {
        return DocumentGeneration.builder()
                .generationId(new GenerationId(UUID.fromString(generationRequest.getDocumentId())))
                .documentId(new DocumentId(UUID.fromString(generationRequest.getDocumentId())))
                .customerId(new CustomerId(UUID.fromString(generationRequest.getCustomerId())))
                .fileExtension(MapperUtils.safeOrDefault(() -> DocumentType.valueOf(generationRequest.getDocumentType()), null))
                .build();
    }

    @Override
    public DocumentEventPayload generatedEventToDocumentEventPayload(GenerationEvent generationEvent) {
        DocumentGeneration documentGeneration = generationEvent.getDocumentGeneration();

        return DocumentEventPayload.builder()
                .generationId(documentGeneration.getGenerationId().getValue().toString())
                .customerId(documentGeneration.getCustomerId().getValue().toString())
                .documentId(documentGeneration.getDocumentId().getValue().toString())
                .createdAt(generationEvent.getCreatedAt())
                .generationStatus(documentGeneration.getGenerationStatus().name())
                .failureMessages(generationEvent.getFailureMessages())
                .build();
    }


}

