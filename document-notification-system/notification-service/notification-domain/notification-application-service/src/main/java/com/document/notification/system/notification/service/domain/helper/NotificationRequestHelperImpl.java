package com.document.notification.system.notification.service.domain.helper;

import com.document.notification.system.notification.service.domain.dto.NotificationRequest;
import com.document.notification.system.notification.service.domain.entity.DocumentNotification;
import com.document.notification.system.notification.service.domain.event.NotificationEvent;
import com.document.notification.system.notification.service.domain.mapper.NotificationDataMapper;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.notification.service.domain.outbox.scheduler.DocumentOutboxHelper;
import com.document.notification.system.notification.service.domain.ports.output.message.publisher.NotificationResponseMessagePublisher;
import com.document.notification.system.notification.service.domain.ports.output.repository.DocumentNotificationRepository;
import com.document.notification.system.notification.service.domain.service.INotificationDomainService;
import com.document.notification.system.notification.service.domain.valueobject.NotificationData;
import com.document.notification.system.notification.service.domain.valueobject.NotificationStatus;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class NotificationRequestHelperImpl implements NotificationRequestHelper {
    private final INotificationDomainService notificationDomainService;
    private final NotificationDataMapper notificationDataMapper;
    private final DocumentNotificationRepository documentNotificationRepository;
    private final DocumentOutboxHelper documentOutboxHelper;
    private final NotificationResponseMessagePublisher notificationResponseMessagePublisher;

    @Transactional
    @Override
    public void persistNotificationOnHistoryRecords(NotificationRequest notificationRequest) {
        if (publishIfOutboxMessageProcessedForNotification(notificationRequest, NotificationStatus.NOTIFICATION_SENT)) {
            log.info("An outbox message with saga id: {} is already saved to database!",
                    notificationRequest.getSagaId());
            return;
        }
        log.info("Received notification event for document id: {}", notificationRequest.getDocumentId());

        ArrayList<String> failureMessages = new ArrayList<>();

        DocumentNotification documentNotification = notificationDataMapper
                .notificationRequestToDocumentNotification(notificationRequest);

        NotificationData notificationData = getNotificationData(notificationRequest);

        NotificationEvent notificationEvent = notificationDomainService
                .validateAndSendNotification(documentNotification, failureMessages, notificationData);

        documentNotificationRepository.save(documentNotification);
        log.info("Document notification saved with id: {}", documentNotification.getId().getValue());

        DocumentEventPayload documentEventPayload = notificationDataMapper
                .notificationEventToDocumentEventPayload(notificationEvent);
        documentOutboxHelper.saveDocumentOutboxMessage(
                documentEventPayload,
                notificationEvent.getDocumentNotification().getNotificationStatus(),
                OutboxStatus.STARTED,
                UUID.fromString(notificationRequest.getSagaId()));

        log.info("Notification processing completed for document id: {}", notificationRequest.getDocumentId());
    }

    private NotificationData getNotificationData(NotificationRequest notificationRequest) {
        return NotificationData.builder()
                .documentId(StringUtils.trimToNull(notificationRequest.getDocumentId()))
                .customerId(StringUtils.trimToNull(notificationRequest.getCustomerId()))
                .requestId(StringUtils.trimToNull(notificationRequest.getId()))
                .sagaId(StringUtils.trimToNull(notificationRequest.getSagaId()))
                .build();
    }

    private boolean publishIfOutboxMessageProcessedForNotification(NotificationRequest notificationRequest,
                                                                    NotificationStatus notificationStatus) {
        Optional<DocumentOutboxMessage> documentOutboxMessagesOptional = documentOutboxHelper
                .getCompletedDocumentOutboxMessageBySagaIdAndNotificationStatus(
                        UUID.fromString(notificationRequest.getSagaId()),
                        notificationStatus
                );

        if (documentOutboxMessagesOptional.isPresent()) {
            DocumentOutboxMessage documentOutboxMessage = documentOutboxMessagesOptional.get();
            log.info("An outbox message with saga id: {} is already saved to database with notification status: {}!",
                    notificationRequest.getSagaId(),
                    notificationStatus);
            notificationResponseMessagePublisher.publish(documentOutboxMessage,
                    documentOutboxHelper::updateOutboxMessage);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public void persistCancelledNotificationOnHistoryRecords(NotificationRequest notificationRequest) {
        log.info("Processing cancellation for notification request: {}", notificationRequest.getDocumentId());

        if (publishIfOutboxMessageProcessedForNotification(notificationRequest, NotificationStatus.NOTIFICATION_CANCELLED)) {
            log.info("Cancellation already processed for saga id: {}", notificationRequest.getSagaId());
            return;
        }

        log.warn("Cancellation logic not fully implemented yet for document id: {}",
                notificationRequest.getDocumentId());
    }
}
