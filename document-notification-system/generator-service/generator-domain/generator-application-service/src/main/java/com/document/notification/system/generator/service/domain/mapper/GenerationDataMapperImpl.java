package com.document.notification.system.generator.service.domain.mapper;

import com.document.notification.system.domain.valueobject.DocumentType;
import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.generator.service.domain.dto.GenerationResponse;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.event.DocumentGeneratedEvent;
import com.document.notification.system.generator.service.domain.event.DocumentGenerationFailedEvent;
import com.document.notification.system.generator.service.domain.event.GenerationEvent;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.generator.service.domain.valueobject.GenerationId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
    public DocumentGeneration generationRequestToDocumentGeneration(GenerationRequest request, DocumentType documentType) {
        return DocumentGeneration.builder()
                .generationId(new GenerationId(UUID.fromString(request.getDocumentId())))
                .fileExtension(documentType)
                .failureMessages(new ArrayList<>())
                .build();
    }

    @Override
    public DocumentEventPayload generatedEventToDocumentEventPayload(DocumentGeneratedEvent event) {
        DocumentGeneration generation = event.getDocumentGeneration();
        return DocumentEventPayload.builder()
                .generationId(generation.getGenerationId().getValue().toString())
                .documentId(generation.getGenerationId().getValue().toString())
                .customerId("") // Will be set from request
                .createdAt(event.getCreatedAt())
                .generationStatus(generation.getGenerationStatus().name())
                .failureMessages(event.getFailureMessages())
                .build();
    }

    @Override
    public DocumentEventPayload failedEventToDocumentEventPayload(DocumentGenerationFailedEvent event) {
        DocumentGeneration generation = event.getDocumentGeneration();
        return DocumentEventPayload.builder()
                .generationId(generation.getGenerationId().getValue().toString())
                .documentId(generation.getGenerationId().getValue().toString())
                .customerId("") // Will be set from request
                .createdAt(event.getCreatedAt())
                .generationStatus(generation.getGenerationStatus().name())
                .failureMessages(event.getFailureMessages())
                .build();
    }

    @Override
    public GenerationResponse generationEventToGenerationResponse(GenerationEvent event, UUID sagaId) {
        DocumentGeneration generation = event.getDocumentGeneration();
        return GenerationResponse.builder()
                .generationId(generation.getGenerationId().getValue().toString())
                .sagaId(sagaId.toString())
                .documentId(generation.getGenerationId().getValue().toString())
                .customerId("") // Will be set from request
                .createdAt(event.getCreatedAt())
                .generationStatus(generation.getGenerationStatus())
                .failureMessages(event.getFailureMessages())
                .build();
    }
}

