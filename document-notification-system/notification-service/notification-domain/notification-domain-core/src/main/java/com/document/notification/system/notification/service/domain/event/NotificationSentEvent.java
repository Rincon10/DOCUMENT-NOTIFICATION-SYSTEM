package com.document.notification.system.notification.service.domain.event;

import com.document.notification.system.notification.service.domain.entity.DocumentNotification;

import java.time.ZonedDateTime;

public class NotificationSentEvent extends NotificationEvent {
    public NotificationSentEvent(DocumentNotification documentNotification, ZonedDateTime createdAt) {
        super(documentNotification, createdAt);
    }
}

