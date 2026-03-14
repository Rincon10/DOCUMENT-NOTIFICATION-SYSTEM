package com.document.notification.system.notification.service.domain.service;

import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.notification.service.domain.entity.DocumentNotification;
import com.document.notification.system.notification.service.domain.event.NotificationEvent;
import com.document.notification.system.notification.service.domain.event.NotificationFailedEvent;
import com.document.notification.system.notification.service.domain.event.NotificationSentEvent;
import com.document.notification.system.notification.service.domain.valueobject.NotificationData;
import com.document.notification.system.notification.service.domain.valueobject.NotificationResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Domain service that orchestrates notification sending business logic.
 * Acts as an orchestrator between domain logic and secondary ports (adapters).
 * Analogous to GeneratorDomainServiceImpl in generator-service.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 */
@Slf4j
@AllArgsConstructor
public class NotificationDomainServiceImpl implements INotificationDomainService {

    private final INotificationSender notificationSender;

    @Override
    public NotificationEvent validateAndSendNotification(DocumentNotification documentNotification,
                                                          List<String> failureMessages,
                                                          NotificationData additionalNotificationData) {
        validateNotification(documentNotification, failureMessages);

        if (!failureMessages.isEmpty()) {
            log.error("Notification validation failed for documentId: {} with errors: {}",
                    documentNotification.getDocumentId(), failureMessages);
            documentNotification.markAsFailed(failureMessages);
            return new NotificationFailedEvent(documentNotification, DateUtils.getZoneDateTimeByUTCZoneId());
        }

        try {
            documentNotification.initializeNotification();
            log.info("Sending notification for document id: {} to recipient: {}",
                    documentNotification.getDocumentId(),
                    documentNotification.getRecipient().getTarget());

            NotificationData notificationData = mergeNotificationData(
                    getNotificationData(documentNotification),
                    additionalNotificationData
            );

            NotificationResult result = notificationSender.sendNotification(
                    documentNotification.getRecipient(),
                    documentNotification.getNotificationContent(),
                    notificationData
            );

            if (result.isSuccessful()) {
                documentNotification.markAsSent();
                log.info("Notification sent successfully for document id: {} with messageId: {}",
                        documentNotification.getDocumentId(), result.getMessageId());
                return new NotificationSentEvent(documentNotification, DateUtils.getZoneDateTimeByUTCZoneId());
            } else {
                failureMessages.add("Notification delivery failed: " + result.getDetails());
                documentNotification.markAsFailed(failureMessages);
                return new NotificationFailedEvent(documentNotification, DateUtils.getZoneDateTimeByUTCZoneId());
            }

        } catch (Exception e) {
            log.error("Failed to send notification for document id: {}", documentNotification.getDocumentId(), e);
            failureMessages.add("Failed to send notification: " + e.getMessage());
            documentNotification.markAsFailed(failureMessages);
            return new NotificationFailedEvent(documentNotification, DateUtils.getZoneDateTimeByUTCZoneId());
        }
    }

    private void validateNotification(DocumentNotification documentNotification, List<String> failureMessages) {
        if (documentNotification.getRecipient() == null || documentNotification.getRecipient().getTarget() == null) {
            failureMessages.add("Recipient information is missing for notification");
        }
        if (documentNotification.getNotificationContent() == null) {
            failureMessages.add("Notification content is missing");
        }
        if (documentNotification.getNotificationContent() != null
                && documentNotification.getNotificationContent().getSubject() == null) {
            failureMessages.add("Notification subject is required");
        }
    }

    private NotificationData getNotificationData(DocumentNotification documentNotification) {
        log.info("Building notification data for document id: {}", documentNotification.getDocumentId());

        return NotificationData.builder()
                .notificationId(documentNotification.getId().getValue().toString())
                .documentId(documentNotification.getDocumentId().getValue().toString())
                .customerId(documentNotification.getCustomerId().getValue().toString())
                .build();
    }

    private NotificationData mergeNotificationData(NotificationData domainData,
                                                    NotificationData additionalData) {
        if (additionalData == null) {
            return domainData;
        }

        return domainData.toBuilder()
                .requestId(additionalData.getRequestId())
                .sagaId(additionalData.getSagaId())
                .build();
    }
}
