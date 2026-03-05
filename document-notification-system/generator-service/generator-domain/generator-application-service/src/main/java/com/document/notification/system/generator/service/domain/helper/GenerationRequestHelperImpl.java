package com.document.notification.system.generator.service.domain.helper;

import com.document.notification.system.domain.valueobject.DocumentType;
import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.event.DocumentGeneratedEvent;
import com.document.notification.system.generator.service.domain.event.DocumentGenerationFailedEvent;
import com.document.notification.system.generator.service.domain.event.GenerationEvent;
import com.document.notification.system.generator.service.domain.mapper.GenerationDataMapper;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.generator.service.domain.outbox.scheduler.DocumentOutboxHelper;
import com.document.notification.system.generator.service.domain.ports.output.message.publisher.GenerationResponseMessagePublisher;
import com.document.notification.system.generator.service.domain.ports.output.repository.DocumentGenerationRepository;
import com.document.notification.system.generator.service.domain.service.IContentGenerator;
import com.document.notification.system.generator.service.domain.service.IGeneratorDomainService;
import com.document.notification.system.generator.service.domain.valueobject.GeneratedContent;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.saga.constants.SagaConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class GenerationRequestHelperImpl implements GenerationRequestHelper {
    private final IGeneratorDomainService generatorDomainService;
    private final IContentGenerator contentGenerator;
    private final GenerationDataMapper generationDataMapper;
    private final DocumentGenerationRepository documentGenerationRepository;
    private final DocumentOutboxHelper documentOutboxHelper;
    private final GenerationResponseMessagePublisher generationResponseMessagePublisher;

    @Transactional
    @Override
    public void persistGenerationOnHistoryRecords(GenerationRequest generationRequest) {
        // Check if already processed (idempotency)
        if (publishIfOutboxMessageProcessedForGeneration(generationRequest, GenerationStatus.GENERATION_COMPLETED)) {
            log.info("An outbox message with saga id: {} is already saved to database!",
                    generationRequest.getSagaId());
            return;
        }
        generationRequest

        log.info("Received generation event for document id: {}", generationRequest.getDocumentId());

        try {
            // Determine document type (default to PDF if not specified)
            DocumentType documentType = DocumentType.PDF;

            // Create domain entity
            DocumentGeneration documentGeneration = generationDataMapper
                    .generationRequestToDocumentGeneration(generationRequest, documentType);

            // Generate Base64 content
            log.info("Generating content for document id: {}", generationRequest.getDocumentId());
            Map<String, Object> generationData = new HashMap<>();
            generationData.put("customerId", generationRequest.getCustomerId());
            generationData.put("requestId", generationRequest.getId());

            GeneratedContent generatedContent = contentGenerator.generateContent(
                    documentType,
                    generationRequest.getDocumentId(),
                    generationRequest.getCustomerId(),
                    generationData
            );

            // Set generated content in entity
            documentGeneration.setGeneratedContent(generatedContent.getBase64Content());

            // Validate and initiate generation through domain service
            List<String> failureMessages = new ArrayList<>();
            GenerationEvent generationEvent = generatorDomainService
                    .validateAndInitiateDocumentGeneration(documentGeneration, failureMessages);

            // Save generation history
            documentGenerationRepository.save(documentGeneration);
            log.info("Document generation saved with id: {}", documentGeneration.getId().getValue());

            // Create and save outbox message
            DocumentEventPayload eventPayload = createEventPayload(generationEvent, generationRequest);
            saveOutboxMessage(eventPayload, generationEvent, generationRequest);

            log.info("Document generation completed for document id: {}", generationRequest.getDocumentId());

        } catch (Exception e) {
            log.error("Error processing generation request for document id: {}",
                    generationRequest.getDocumentId(), e);
            throw new RuntimeException("Failed to process generation request", e);
        }
    }

    private DocumentEventPayload createEventPayload(GenerationEvent event, GenerationRequest request) {
        DocumentEventPayload payload;

        if (event instanceof DocumentGeneratedEvent) {
            payload = generationDataMapper.generatedEventToDocumentEventPayload((DocumentGeneratedEvent) event);
        } else if (event instanceof DocumentGenerationFailedEvent) {
            payload = generationDataMapper.failedEventToDocumentEventPayload((DocumentGenerationFailedEvent) event);
        } else {
            throw new IllegalArgumentException("Unknown event type: " + event.getClass());
        }

        // Set customer and document IDs from request
        return DocumentEventPayload.builder()
                .generationId(payload.getGenerationId())
                .documentId(request.getDocumentId())
                .customerId(request.getCustomerId())
                .createdAt(payload.getCreatedAt())
                .generationStatus(payload.getGenerationStatus())
                .failureMessages(payload.getFailureMessages())
                .build();
    }

    private void saveOutboxMessage(DocumentEventPayload payload,
                                   GenerationEvent event,
                                   GenerationRequest request) {
        documentOutboxHelper.saveDocumentOutboxMessage(
                payload,
                event.getDocumentGeneration().getGenerationStatus(),
                OutboxStatus.STARTED,
                UUID.fromString(request.getSagaId())
        );
    }

    private boolean publishIfOutboxMessageProcessedForGeneration(GenerationRequest generationRequest,
                                                                 GenerationStatus generationStatus) {
        Optional<DocumentOutboxMessage> documentOutboxMessagesOptional = documentOutboxHelper
                .getCompletedDocumentOutboxMessageBySagaIdAndGenerationStatus(
                        UUID.fromString(generationRequest.getSagaId()),
                        generationStatus
                );

        if (documentOutboxMessagesOptional.isPresent()) {
            DocumentOutboxMessage documentOutboxMessage = documentOutboxMessagesOptional.get();
            log.info("An outbox message with saga id: {} is already saved to database with generation status: {}!",
                    generationRequest.getSagaId(),
                    generationStatus);
            generationResponseMessagePublisher.publish(documentOutboxMessage,
                    documentOutboxHelper::updateOutboxMessage);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public void persistCancelledGenerationOnHistoryRecords(GenerationRequest generationRequest) {
        log.info("Processing cancellation for generation request: {}", generationRequest.getDocumentId());

        // Check if already processed
        if (publishIfOutboxMessageProcessedForGeneration(generationRequest, GenerationStatus.GENERATION_CANCELLED)) {
            log.info("Cancellation already processed for saga id: {}", generationRequest.getSagaId());
            return;
        }

        // TODO: Implement cancellation logic
        // 1. Find existing generation by document ID
        // 2. Update status to CANCELLED
        // 3. Create outbox message for cancellation event

        log.warn("Cancellation logic not fully implemented yet for document id: {}",
                generationRequest.getDocumentId());
    }
}
