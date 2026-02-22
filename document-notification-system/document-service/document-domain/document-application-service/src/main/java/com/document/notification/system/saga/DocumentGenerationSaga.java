package com.document.notification.system.saga;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.document.service.domain.exception.DocumentNotFoundException;
import com.document.notification.system.document.service.domain.service.IDocumentDomainService;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.domain.valueobject.DocumentStatus;
import com.document.notification.system.dto.message.GenerationResponse;
import com.document.notification.system.mapper.IDocumentDataMapper;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.generator.DocumentGenerationOutboxMessage;
import com.document.notification.system.outbox.model.notification.DocumentNotificationEventPayload;
import com.document.notification.system.outbox.scheduler.generator.GeneratorOutboxHelper;
import com.document.notification.system.outbox.scheduler.notification.NotificationOutboxHelper;
import com.document.notification.system.ports.output.repository.IDocumentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/02/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class DocumentGenerationSaga implements SagaStep<GenerationResponse> {

    private final IDocumentDomainService documentDomainService;
    private final IDocumentRepository documentRepository;
    private final GeneratorOutboxHelper generatorOutboxHelper;
    private final NotificationOutboxHelper notificationOutboxHelper;
    private final IDocumentSagaHelper documentSagaHelper;
    private final IDocumentDataMapper documentDataMapper;


    @Override
    @Transactional
    public void execute(GenerationResponse generationResponse) {

        // If Outbox table is on started means that the message is being processed for the first time, if it is not present or if it is on completed means that the message was already processed and we can skip it to avoid processing the same message multiple times
        Optional<DocumentGenerationOutboxMessage> optionalDocumentGenerationOutboxMessage = generatorOutboxHelper.getDocumentGenerationOutboxMessageBySagaIdAndSagaStatus(generationResponse.getSagaId(), SagaStatus.STARTED);
        if (optionalDocumentGenerationOutboxMessage.isEmpty()) {
            log.info("Document Generation Outbox Message was already processed! for saga id: {} and saga status: {}", generationResponse.getSagaId(), SagaStatus.STARTED);
            return;
        }

        DocumentGenerationOutboxMessage documentGenerationOutboxMessage = optionalDocumentGenerationOutboxMessage.get();

        // Calling document-domain-core to update the document state and saving all on the repository, if any exception is thrown here the transaction will be rolled back and the message will be retried later by the outbox scheduler
        DocumentCreatedEvent documentCreatedEvent = completeGenerationForDocument(generationResponse);

        // Saga orchestor handling the following steps of the saga, if any exception is thrown here the transaction will be rolled back and the message will be retried later by the outbox scheduler
        SagaStatus sagaStatus = documentSagaHelper.documentStatusToSagaStatus(documentCreatedEvent.getDocument().getDocumentStatus());

        DocumentGenerationOutboxMessage generationOutboxMessageUpdated = getUpdatedGenerationOutboxMessage(documentGenerationOutboxMessage, documentCreatedEvent.getDocument().getDocumentStatus(), sagaStatus);

        // updating states for generator outbox
        generatorOutboxHelper.save(generationOutboxMessageUpdated);

        DocumentNotificationEventPayload documentNotificationEventPayload = documentDataMapper.documentCreatedEventToDocumentNotificationEventPayload(documentCreatedEvent);

        notificationOutboxHelper.saveNotificationOutboxMessage(documentNotificationEventPayload,
                documentCreatedEvent.getDocument().getDocumentStatus(),
                sagaStatus,
                OutboxStatus.STARTED,
                generationResponse.getSagaId());

        log.info("Document with id: {} is generated", documentCreatedEvent.getDocument().getId().getValue());

    }

    private DocumentGenerationOutboxMessage getUpdatedGenerationOutboxMessage(DocumentGenerationOutboxMessage
                                                                                      documentGenerationOutboxMessage,
                                                                              DocumentStatus
                                                                                      documentStatus,
                                                                              SagaStatus
                                                                                      sagaStatus) {
        documentGenerationOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(UTC.getId())));
        documentGenerationOutboxMessage.setDocumentStatus(documentStatus);
        documentGenerationOutboxMessage.setSagaStatus(sagaStatus);
        return documentGenerationOutboxMessage;
    }

    private Document findDocument(UUID documentId) {
        return documentRepository.findById(new DocumentId(documentId)).orElseThrow(() -> {
            log.error("Document with id: {} was not found in the database", documentId);
            return new DocumentNotFoundException("Document with id: " + documentId + " was not found in the database");
        });
    }

    private DocumentCreatedEvent completeGenerationForDocument(GenerationResponse generationResponse) {
        log.info("Completing generation for document with id: {}", generationResponse.getDocumentId());

        Document document = findDocument(generationResponse.getDocumentId());
        DocumentCreatedEvent documentCreatedEvent = documentDomainService.validateAndInitiateDocument(document);
        documentRepository.save(documentCreatedEvent.getDocument());

        return documentCreatedEvent;
    }

    @Override
    @Transactional
    public void compensate(GenerationResponse generationResponse) {

    }
}
