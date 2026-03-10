package com.document.notification.system.notification.service.domain.event;


import com.document.notification.system.domain.events.DomainEvent;
import com.document.notification.system.notification.service.domain.entity.DocumentNotification;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public abstract class NotificationEvent implements DomainEvent<DocumentNotification> {
    private final DocumentNotification documentNotification;
    private final ZonedDateTime createdAt;

    protected NotificationEvent(DocumentNotification documentNotification, ZonedDateTime createdAt) {
        this.documentNotification = documentNotification;
        this.createdAt = createdAt;
    }
}

