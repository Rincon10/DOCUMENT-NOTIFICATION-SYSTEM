package com.document.notification.system.notification.service.domain.mapper;

import com.document.notification.system.notification.service.domain.dto.NotificationRequest;
import com.document.notification.system.notification.service.domain.entity.DocumentNotification;
import com.document.notification.system.notification.service.domain.event.NotificationEvent;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentEventPayload;

public interface NotificationDataMapper {

    DocumentNotification notificationRequestToDocumentNotification(NotificationRequest request);

    DocumentEventPayload notificationEventToDocumentEventPayload(NotificationEvent notificationEvent);
}
