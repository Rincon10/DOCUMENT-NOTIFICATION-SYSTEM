package com.document.notification.system.notification.service.domain.service;

import com.document.notification.system.notification.service.domain.valueobject.NotificationContent;
import com.document.notification.system.notification.service.domain.valueobject.NotificationData;
import com.document.notification.system.notification.service.domain.valueobject.NotificationResult;
import com.document.notification.system.notification.service.domain.valueobject.Recipient;

/**
 * Service interface for sending notifications through different channels.
 * Analogous to IContentGenerator in generator-service.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 */
public interface INotificationSender {

    /**
     * Sends a notification to the specified recipient
     *
     * @param recipient           The notification recipient (email, etc.)
     * @param notificationContent The content of the notification (subject, message, attachments)
     * @param data                Additional contextual data for the notification
     * @return NotificationResult object containing the delivery result
     */
    NotificationResult sendNotification(Recipient recipient,
                                         NotificationContent notificationContent,
                                         NotificationData data);
}
