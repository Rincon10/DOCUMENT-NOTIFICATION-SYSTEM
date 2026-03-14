package com.document.notification.system.saga;

import com.document.notification.system.document.service.domain.entity.Document;
import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.document.service.domain.exception.DocumentNotFoundException;
import com.document.notification.system.document.service.domain.service.IDocumentDomainService;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.domain.valueobject.DocumentStatus;
import com.document.notification.system.dto.message.NotificationResponse;
import com.document.notification.system.mapper.IDocumentDataMapper;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.notification.DocumentNotificationOutboxMessage;
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
 * @since 9/03/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class DocumentNotificationSaga implements SagaStep<NotificationResponse> {

    private final IDocumentDomainService documentDomainService;
    private final GeneratorOutboxHelper generatorOutboxHelper;
    private final NotificationOutboxHelper notificationOutboxHelper;
    private final IDocumentSagaHelper documentSagaHelper;
    private final IDocumentDataMapper documentDataMapper;

    @Override
    @Transactional
    public void execute(NotificationResponse notificationResponse) {

        // If Outbox table is on started means that the message is being processed for the first time, if it is not present or if it is on completed means that the message was already processed and we can skip it to avoid processing the same message multiple times
        Optional<DocumentNotificationOutboxMessage> optionalDocumentNotificationOutboxMessage = notificationOutboxHelper.getDocumentNotificationOutboxMessageBySagaIdAndSagaStatus(UUID.fromString(notificationResponse.getSagaId()), SagaStatus.PROCESSING);
        if (optionalDocumentNotificationOutboxMessage.isEmpty()) {
            log.info("Document Notification Outbox Message was already processed! for saga id: {} and saga status: {}", notificationResponse.getSagaId(), SagaStatus.STARTED);
            return;
        }

        DocumentNotificationOutboxMessage documentNotificationOutboxMessage = optionalDocumentNotificationOutboxMessage.get();



        Document document  = completeNotificationForDocument(notificationResponse);

        // Saga orchestrator handling the following steps of the saga, if any exception is thrown here the transaction will be rolled back and the message will be retried later by the outbox scheduler
        SagaStatus sagaStatus = documentSagaHelper.documentStatusToSagaStatus(documentCreatedEvent.getDocument().getDocumentStatus());

        DocumentNotificationOutboxMessage notificationOutboxMessageUpdated = getUpdatedNotificationOutboxMessage(documentNotificationOutboxMessage, documentCreatedEvent.getDocument().getDocumentStatus(), sagaStatus);

        // updating states for notification outbox
        notificationOutboxHelper.save(notificationOutboxMessageUpdated);

        log.info("Document with id: {} notification is completed", documentCreatedEvent.getDocument().getId().getValue());
    }

    private DocumentNotificationOutboxMessage getUpdatedNotificationOutboxMessage(DocumentNotificationOutboxMessage documentNotificationOutboxMessage,
                                                                                  DocumentStatus documentStatus,
                                                                                  SagaStatus sagaStatus) {
        documentNotificationOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(UTC.getId())));
        documentNotificationOutboxMessage.setDocumentStatus(documentStatus);
        documentNotificationOutboxMessage.setSagaStatus(sagaStatus);
        return documentNotificationOutboxMessage;
    }

    private Document completeNotificationForDocument(NotificationResponse notificationResponse) {
        log.info("Completing notification for document with id: {}", notificationResponse.getDocumentId());

        Document document = documentSagaHelper.findDocument(notificationResponse.getDocumentId());


        DocumentCreatedEvent documentCreatedEvent = documentDomainService.validateAndInitiateDocument(document);
        documentRepository.save(documentCreatedEvent.getDocument());

        return documentCreatedEvent;
    }

    @Override
    @Transactional
    public void compensate(NotificationResponse notificationResponse) {
        log.info("Compensating notification for document with id: {}", notificationResponse.getDocumentId());

        Optional<DocumentNotificationOutboxMessage> optionalDocumentNotificationOutboxMessage = notificationOutboxHelper.getDocumentNotificationOutboxMessageBySagaIdAndSagaStatus(UUID.fromString(notificationResponse.getSagaId()), SagaStatus.STARTED);
        if (optionalDocumentNotificationOutboxMessage.isEmpty()) {
            log.info("Document Notification Outbox Message was already processed or compensated! for saga id: {}", notificationResponse.getSagaId());
            return;
        }

        DocumentNotificationOutboxMessage documentNotificationOutboxMessage = optionalDocumentNotificationOutboxMessage.get();

        // Update saga status to COMPENSATING
        documentNotificationOutboxMessage.setSagaStatus(SagaStatus.COMPENSATING);
        documentNotificationOutboxMessage.setDocumentStatus(DocumentStatus.CANCELLING);
        notificationOutboxHelper.save(documentNotificationOutboxMessage);

        log.info("Document with id: {} notification is being compensated", notificationResponse.getDocumentId());
    }
}
