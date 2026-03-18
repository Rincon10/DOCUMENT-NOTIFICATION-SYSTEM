package com.document.notification.system.outbox.scheduler.notification;

import com.document.notification.system.document.service.domain.exception.DocumentDomainException;
import com.document.notification.system.domain.utils.JsonSerializationUtil;
import com.document.notification.system.domain.valueobject.DocumentStatus;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.outbox.model.notification.DocumentNotificationEventPayload;
import com.document.notification.system.outbox.model.notification.DocumentNotificationOutboxMessage;
import com.document.notification.system.ports.output.repository.NotificationOutboxRepository;
import com.document.notification.system.saga.SagaStatus;
import com.document.notification.system.saga.constants.SagaConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/02/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class NotificationOutboxHelper {

    private final NotificationOutboxRepository notificationOutboxRepository;

    @Transactional
    public void saveNotificationOutboxMessage(DocumentNotificationEventPayload documentNotificationEventPayload,
                                              DocumentStatus documentStatus,
                                              SagaStatus sagaStatus,
                                              OutboxStatus outboxStatus,
                                              UUID sagaId) {

        String payloadJson = JsonSerializationUtil.toJson(documentNotificationEventPayload);

        DocumentNotificationOutboxMessage documentNotificationOutboxMessage = DocumentNotificationOutboxMessage
                .builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .documentId(UUID.fromString(documentNotificationEventPayload.getDocumentId()) )
                .createdAt(documentNotificationEventPayload.getCreatedAt())
                .type(SagaConstants.SAGA_NAME)
                .payload(payloadJson)
                .documentStatus(documentStatus)
                .sagaStatus(sagaStatus)
                .outboxStatus(outboxStatus)
                .build();

        save(documentNotificationOutboxMessage);
    }

    @Transactional(readOnly = true)
    public Optional<DocumentNotificationOutboxMessage> getDocumentNotificationOutboxMessageBySagaIdAndSagaStatus(UUID sagaId, SagaStatus sagaStatus) {
        return notificationOutboxRepository.findByTypeAndSagaIdAndSagaStatus(SagaConstants.SAGA_NAME, sagaId, sagaStatus);
    }

    @Transactional(readOnly = true)
    public Optional<List<DocumentNotificationOutboxMessage>> getNotificationOutboxMessageByOutboxStatusAndSagaStatus(OutboxStatus outboxStatus, SagaStatus... sagaStatus) {
        return notificationOutboxRepository.findByTypeAndOutboxStatusAndSagaStatus(
                SagaConstants.SAGA_NAME,
                outboxStatus,
                sagaStatus
        );
    }



    @Transactional
    public void save(DocumentNotificationOutboxMessage documentNotificationOutboxMessage) {
        DocumentNotificationOutboxMessage savedNotification = notificationOutboxRepository.save(documentNotificationOutboxMessage);
        if (Objects.isNull(savedNotification)) {
            log.error("Failed to save DocumentNotificationOutboxMessage with id: {}", documentNotificationOutboxMessage.getId());
            throw new DocumentDomainException("Failed to save DocumentNotificationOutboxMessage with id: " + documentNotificationOutboxMessage.getId());
        }
        log.info("DocumentNotificationOutboxMessage with id: {} is saved successfully!", documentNotificationOutboxMessage.getId());
    }
}
