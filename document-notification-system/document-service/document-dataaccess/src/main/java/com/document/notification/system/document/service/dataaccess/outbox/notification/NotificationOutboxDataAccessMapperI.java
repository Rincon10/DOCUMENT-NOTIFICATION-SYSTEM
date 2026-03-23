package com.document.notification.system.document.service.dataaccess.outbox.notification;

import com.document.notification.system.document.service.dataaccess.outbox.notification.entity.NotificationOutboxEntity;
import com.document.notification.system.outbox.model.notification.DocumentNotificationOutboxMessage;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 14/03/2026
 */
public interface NotificationOutboxDataAccessMapperI {
    NotificationOutboxEntity mapDocumentNotificationOutboxMessageToNotificationOutboxEntity(DocumentNotificationOutboxMessage documentNotificationOutboxMessage);

    DocumentNotificationOutboxMessage mapNotificationOutboxEntityToDocumentNotificationOutboxMessage(NotificationOutboxEntity notificationOutboxEntity);
}
