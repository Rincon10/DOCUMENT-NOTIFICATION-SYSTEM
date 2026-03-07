package com.document.notification.system.generator.service.domain.helper;

import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.generator.service.domain.dto.GenerationRequest;
import com.document.notification.system.generator.service.domain.entity.DocumentGeneration;
import com.document.notification.system.generator.service.domain.event.GenerationEvent;
import com.document.notification.system.generator.service.domain.mapper.GenerationDataMapper;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.generator.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.generator.service.domain.outbox.scheduler.DocumentOutboxHelper;
import com.document.notification.system.generator.service.domain.ports.output.message.publisher.GenerationResponseMessagePublisher;
import com.document.notification.system.generator.service.domain.ports.output.repository.DocumentGenerationRepository;
import com.document.notification.system.generator.service.domain.service.IGeneratorDomainService;
import com.document.notification.system.generator.service.domain.valueobject.GenerationContentData;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        log.info("Received generation event for document id: {}", generationRequest.getDocumentId());

        try {
            List<String> failureMessages = new ArrayList<>();

            DocumentGeneration documentGeneration = generationDataMapper
                    .generationRequestToDocumentGeneration(generationRequest);

            GenerationContentData generationData = getGenerationData(generationRequest);

            GenerationEvent generationEvent = generatorDomainService
                    .validateInitiateGenerateAndComplete(documentGeneration, failureMessages, generationData);

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

    private GenerationContentData getGenerationData(GenerationRequest generationRequest) {
        return GenerationContentData.builder()
                .documentId(StringUtils.trimToNull(generationRequest.getDocumentId()))
                .customerId(StringUtils.trimToNull(generationRequest.getCustomerId()))
                .requestId(StringUtils.trimToNull(generationRequest.getId()))
                .sagaId(StringUtils.trimToNull(generationRequest.getSagaId()))
                .build();
    }


    private DocumentEventPayload createEventPayload(GenerationEvent event, GenerationRequest request) {

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
