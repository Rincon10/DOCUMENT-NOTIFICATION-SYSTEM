package com.document.notification.system.notification.service.domain.mapper;

import com.document.notification.system.domain.valueobject.CustomerId;
import com.document.notification.system.domain.valueobject.DocumentId;
import com.document.notification.system.notification.service.domain.dto.NotificationRequest;
import com.document.notification.system.notification.service.domain.entity.DocumentNotification;
import com.document.notification.system.notification.service.domain.event.NotificationEvent;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.notification.service.domain.valueobject.NotificationChannel;
import com.document.notification.system.notification.service.domain.valueobject.NotificationContent;
import com.document.notification.system.notification.service.domain.valueobject.NotificationId;
import com.document.notification.system.notification.service.domain.valueobject.Recipient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationDataMapperImpl implements NotificationDataMapper {

    @Override
    public DocumentNotification notificationRequestToDocumentNotification(NotificationRequest request) {
        return new DocumentNotification(
                new NotificationId(UUID.randomUUID()),
                new DocumentId(UUID.fromString(request.getDocumentId())),
                new CustomerId(UUID.fromString(request.getCustomerId())),
                Recipient.builder()
                        .target(request.getRecipientId())
                        .channel(NotificationChannel.EMAIL)
                        .build(),
                NotificationContent.builder()
                        .subject(request.getSubject())
                        .message(request.getMessage())
                        .fileName(request.getFileName())
                        .contentType(request.getContentType())
                        .contentBase64(request.getContentBase64())
                        .build(),
                null
        );
    }

    @Override
    public DocumentEventPayload notificationEventToDocumentEventPayload(NotificationEvent notificationEvent) {
        DocumentNotification notification = notificationEvent.getDocumentNotification();

        return DocumentEventPayload.builder()
                .notificationId(notification.getId().getValue().toString())
                .customerId(notification.getCustomerId().getValue().toString())
                .documentId(notification.getDocumentId().getValue().toString())
                .recipientId(notification.getRecipient().getTarget())
                .createdAt(notificationEvent.getCreatedAt())
                .notificationStatus(notification.getNotificationStatus().name())
                .failureMessages(notification.getFailureMessages())
                .build();
    }
}
