package com.document.notification.system.generator.service.domain.mapper;

import com.document.notification.system.domain.utils.MapperUtils;
import com.document.notification.system.domain.valueobject.CustomerId;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.domain.valueobject.DocumentType;
import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.generator.service.domain.dto.GenerationResponse;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.event.DocumentGeneratedEvent;
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
    public DocumentGeneration generationRequestToDocumentGeneration(GenerationRequest generationRequest) {
        return DocumentGeneration.builder()
                .generationId(new GenerationId(UUID.fromString(generationRequest.getDocumentId())))
                .documentId(new DocumentId(UUID.fromString(generationRequest.getDocumentId())))
                .customerId(new CustomerId(UUID.fromString(generationRequest.getCustomerId())))
                .fileExtension(MapperUtils.safeOrDefault(() -> DocumentType.valueOf(generationRequest.getDocumentType()), null))
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

