package com.document.notification.system.notification.service.domain.outbox.scheduler;

import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.domain.utils.JsonSerializationUtil;
import com.document.notification.system.notification.service.domain.exception.NotificationDomainException;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.notification.service.domain.ports.output.repository.DocumentOutboxRepository;
import com.document.notification.system.notification.service.domain.valueobject.NotificationStatus;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.document.notification.system.saga.constants.SagaConstants.SAGA_NAME;

@Component
@Slf4j
@AllArgsConstructor
public class DocumentOutboxHelper {
    private final DocumentOutboxRepository documentOutboxRepository;

    @Transactional(readOnly = true)
    public Optional<DocumentOutboxMessage> getCompletedDocumentOutboxMessageBySagaIdAndNotificationStatus(UUID sagaId,
                                                                                                          NotificationStatus notificationStatus) {
        return documentOutboxRepository.findByTypeAndSagaIdAndNotificationStatusAndOutboxStatus(
                SAGA_NAME, sagaId, notificationStatus, OutboxStatus.COMPLETED);
    }

    @Transactional
    public void updateOutboxMessage(DocumentOutboxMessage documentOutboxMessage, OutboxStatus outboxStatus) {
        documentOutboxMessage.setOutboxStatus(outboxStatus);
        save(documentOutboxMessage);
        log.info("Document outbox table status is updated as: {}", outboxStatus.name());
    }

    @Transactional
    public void saveDocumentOutboxMessage(DocumentEventPayload eventPayload,
                                          NotificationStatus notificationStatus,
                                          OutboxStatus outboxStatus,
                                          UUID sagaId) {
        String payload = JsonSerializationUtil.toJson(eventPayload,
                "Could not create DocumentEventPayload for notification id: " + eventPayload.getNotificationId());

        DocumentOutboxMessage outboxMessage = DocumentOutboxMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(DateUtils.getZoneDateTimeByUTCZoneId())
                .type(SAGA_NAME)
                .payload(payload)
                .notificationStatus(notificationStatus)
                .outboxStatus(outboxStatus)
                .build();

        save(outboxMessage);
        log.info("Document outbox message saved with id: {} for saga id: {}", outboxMessage.getId(), sagaId);
    }

    private void save(DocumentOutboxMessage documentOutboxMessage) {
        DocumentOutboxMessage response = documentOutboxRepository.save(documentOutboxMessage);
        if (response == null) {
            log.error("Could not save DocumentOutboxMessage!");
            throw new NotificationDomainException("Could not save DocumentOutboxMessage!");
        }
        log.info("DocumentOutboxMessage is saved with id: {}", documentOutboxMessage.getId());
    }

    public Optional<List<DocumentOutboxMessage>> getDocumentOutboxMessageByOutboxStatus(OutboxStatus outboxStatus) {
        return documentOutboxRepository.findByTypeAndOutboxStatus(SAGA_NAME, outboxStatus);
    }
}
