package com.document.notification.system.notification.service.domain.service;

import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.notification.service.domain.entity.DocumentNotification;
import com.document.notification.system.notification.service.domain.event.NotificationEvent;
import com.document.notification.system.notification.service.domain.event.NotificationFailedEvent;
import com.document.notification.system.notification.service.domain.event.NotificationSentEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class NotificationDomainServiceImpl implements INotificationDomainService {

    @Override
    public NotificationEvent validateAndSendNotification(DocumentNotification documentNotification,
                                                          List<String> failureMessages) {
        documentNotification.initializeNotification();

        if (documentNotification.getRecipient() == null || documentNotification.getRecipient().getTarget() == null) {
            failureMessages.add("Recipient information is missing for notification");
        }

        if (!failureMessages.isEmpty()) {
            log.error("Notification validation failed for documentId: {} with errors: {}",
                    documentNotification.getDocumentId(), failureMessages);
            documentNotification.markAsFailed(failureMessages);
            return new NotificationFailedEvent(documentNotification, DateUtils.getZoneDateTimeByUTCZoneId());
        }

        try {
            log.info("Sending notification for document id: {} to recipient: {}",
                    documentNotification.getDocumentId(),
                    documentNotification.getRecipient().getTarget());

            // Simulate notification sending
            documentNotification.markAsSent();
            log.info("Notification sent successfully for document id: {}", documentNotification.getDocumentId());
            return new NotificationSentEvent(documentNotification, DateUtils.getZoneDateTimeByUTCZoneId());
        } catch (Exception e) {
            log.error("Failed to send notification for document id: {}", documentNotification.getDocumentId(), e);
            failureMessages.add("Failed to send notification: " + e.getMessage());
            documentNotification.markAsFailed(failureMessages);
            return new NotificationFailedEvent(documentNotification, DateUtils.getZoneDateTimeByUTCZoneId());
        }
    }
}
