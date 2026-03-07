package com.document.notification.system.generator.service.domain.mapper;

import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.generator.service.domain.dto.GenerationResponse;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.event.DocumentGeneratedEvent;
import com.document.notification.system.generator.service.domain.event.GenerationEvent;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentEventPayload;

import java.util.UUID;

/**
 * Mapper for converting between different representations of generation data
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 3/03/2026
 */
public interface GenerationDataMapper {

    /**
     * Converts GenerationRequest DTO to DocumentGeneration domain entity
     */
    DocumentGeneration generationRequestToDocumentGeneration(GenerationRequest request);

    /**
     * Converts DocumentGeneratedEvent to DocumentEventPayload for outbox pattern
     */
    DocumentEventPayload generatedEventToDocumentEventPayload(DocumentGeneratedEvent event);

    /**
     * Converts GenerationEvent to GenerationResponse DTO
     */
    GenerationResponse generationEventToGenerationResponse(GenerationEvent event, UUID sagaId);
}


