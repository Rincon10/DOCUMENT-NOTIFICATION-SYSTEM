package com.document.notification.system.document.service.domain.event;

import com.document.notification.system.document.service.domain.entity.Document;

import java.time.ZonedDateTime;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
public class DocumentCreatedEvent extends DocumentEvent {
    public DocumentCreatedEvent(Document document, ZonedDateTime createdAt) {
        super(document, createdAt);
    }
}
