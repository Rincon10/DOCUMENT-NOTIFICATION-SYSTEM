package com.document.notification.system.document.service.dataaccess.outbox.notification;

import com.document.notification.system.document.service.dataaccess.outbox.notification.entity.NotificationOutboxEntity;
import com.document.notification.system.outbox.model.notification.DocumentNotificationOutboxMessage;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 14/03/2026
 */
@Component
public class NotificationOutboxDataAccessMapperImpl implements NotificationOutboxDataAccessMapperI {

    @Override
    public NotificationOutboxEntity mapDocumentNotificationOutboxMessageToNotificationOutboxEntity(DocumentNotificationOutboxMessage documentNotificationOutboxMessage) {
        return NotificationOutboxEntity.builder()
                .id(documentNotificationOutboxMessage.getId())
                .sagaId(documentNotificationOutboxMessage.getSagaId())
                .createdAt(documentNotificationOutboxMessage.getCreatedAt())
                .processedAt(documentNotificationOutboxMessage.getProcessedAt())
                .type(documentNotificationOutboxMessage.getType())
                .payload(documentNotificationOutboxMessage.getPayload())
                .sagaStatus(documentNotificationOutboxMessage.getSagaStatus())
                .documentStatus(documentNotificationOutboxMessage.getDocumentStatus())
                .outboxStatus(documentNotificationOutboxMessage.getOutboxStatus())
                .version(documentNotificationOutboxMessage.getVersion())
                .build();
    }

    @Override
    public DocumentNotificationOutboxMessage mapNotificationOutboxEntityToDocumentNotificationOutboxMessage(NotificationOutboxEntity notificationOutboxEntity) {
        return DocumentNotificationOutboxMessage.builder()
                .id(notificationOutboxEntity.getId())
                .sagaId(notificationOutboxEntity.getSagaId())
                .createdAt(notificationOutboxEntity.getCreatedAt())
                .processedAt(notificationOutboxEntity.getProcessedAt())
                .type(notificationOutboxEntity.getType())
                .payload(notificationOutboxEntity.getPayload())
                .sagaStatus(notificationOutboxEntity.getSagaStatus())
                .documentStatus(notificationOutboxEntity.getDocumentStatus())
                .outboxStatus(notificationOutboxEntity.getOutboxStatus())
                .version(notificationOutboxEntity.getVersion())
                .build();
    }
}
