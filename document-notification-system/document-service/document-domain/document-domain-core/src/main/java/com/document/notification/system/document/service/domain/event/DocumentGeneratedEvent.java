package com.document.notification.system.document.service.domain.event;

import com.document.notification.system.document.service.domain.entity.Document;

import java.time.ZonedDateTime;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 14/03/2026
 */
public class DocumentGeneratedEvent extends DocumentEvent {
    public DocumentGeneratedEvent(Document document, ZonedDateTime createdAt) {
        super(document,createdAt);
    }
}
